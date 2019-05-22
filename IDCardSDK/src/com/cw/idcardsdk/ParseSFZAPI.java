package com.cw.idcardsdk;

import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;


import com.cw.serialportsdk.cw;
import com.cw.serialportsdk.utils.DataUtils;
import com.ivsign.android.IDCReader.IDCReaderSDK;
import com.ivsign.android.IDCReader.SfzFileManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


import static com.cw.serialportsdk.cw.*;


/**
 * u3,cfon640,a370身份证Api
 */
public class ParseSFZAPI {

    /**
     * 身份证API，适用于U3
     * aa aa aa 96 69 09 0a 00 00 90 ......
     * <p>
     * buffer  10+2+2+2+256+1024+(1024)+1  ---->二代证长度为1297；三代证长度2321
     * <p>
     * 10:前十个字节;  -------读卡命令返回的结果状态
     * 2：文本字节长度;
     * 2: 照片字节长度;
     * 2: 指纹字节长度；
     * 256: 文本;
     * 1024:照片;
     * 1024:指纹;
     * 1:?
     */


    private static final String TAG = "CWIDCard" + "ParseSFZAPI";

    private String src = Environment.getExternalStorageDirectory().getAbsolutePath() + "/sfzPic";

    /**
     * U3_640读取身份证指令
     */
    private static final byte[] U3_READ_CARD = {(byte) 0xca, (byte) 0xdf, 0x02, 0x35, 0x00, 0x09, 0x00, 0x07, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0x96, 0x69, 0x00, 0x00, (byte) 0xe3};

    /**
     * U3_640读取身份证id指令
     */
    private static final byte[] U3_READ_ID = {(byte) 0xca, (byte) 0xdf, 0x02, 0x35, 0x00, 0x09, 0x00, 0x07, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0x96, 0x69, 0x00, 0x01, (byte) 0xe3};


    /**
     * CPOS800机器
     */
    private static final byte[] CPOS9800_READ_IDCARD = {0x07, 0x00, 0x0d, (byte) 0xca, (byte) 0xdf, 0x03, 0x00, 0x07, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0x96, 0x69, 0x00, 0x00, (byte) 0xe3, 0x30};


    /**
     *
     */
    private static final String CARD_SUCCESS = "AAAAAA96690508000090"; //读卡成功的标志，二代证，---海派老平台

    private static final String CARD_SUCCESS2 = "AAAAAA9669090A000090"; //三代证读卡成功的标志   ---海派、U3两个平台

    private static final String CARD_SUCCESS3 = "aaaaaa9669050a000090"; //二代证读卡成功的标志   ----U3平台

    private static final String TIMEOUT_RETURN = "AAAAAA96690005050505";

    private static final String CMD_ERROR = "CMD_ERROR";

    private static final byte[] command1 = "D&C00040101".getBytes();
    private static final byte[] command2 = "D&C00040102".getBytes();
    private static final byte[] command3 = "D&C00040108".getBytes();
    private static final byte[] SFZ_ID_COMMAND1 = "f050000\r\n".getBytes();
    private static final byte[] SFZ_ID_COMMAND2 = "f1d0000000000080108\r\n".getBytes();
    private static final byte[] SFZ_ID_COMMAND3 = "f0036000008\r\n".getBytes();

    private static final String TURN_ON = "c050601\r\n";// 打开天线厂
    private static final String TURN_OFF = "c050602\r\n";// 关闭天线厂

    private static final String SFZ_ID_RESPONSE1 = "5000000000";
    private static final String SFZ_ID_RESPONSE2 = "08";
    private static final String TURN_OFF_RESPONSE = "RF carrier off!";

    private static final String MODULE_SUCCESS = "AAAAAA96690014000090";

    public static final int DATA_SIZE = 2321;

    private byte[] buffer = new byte[DATA_SIZE];

    @Deprecated
    public static final int SECOND_GENERATION_CARD_OLD = 1295;//废弃

    /**
     * 二代身份证（无指纹数据）
     */
    public static final int SECOND_GENERATION_CARD = 1297;

    /**
     * 三代身份证（有指纹数据）
     */
    public static final int THIRD_GENERATION_CARD = 2321;

    private Context m_Context;

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    public ParseSFZAPI(Context context) {
        Log.i(TAG, "-------------------ParseSFZAPI实例化---------------------");
        this.m_Context = context;
    }

    private Result result;


    /**
     * 移远U3，U8读法
     * <p>
     * 读取身份证信息，此方法为阻塞的，建议放在子线程处理
     *
     * @return Result：获取身份证信息结果
     */
    @Deprecated
    public Result read() {

        People people = null;
        IDCardSerialPortManager.getInstance().clearBuffer();

        //此处判断型号	msm8953 for arm64

        IDCardSerialPortManager.getInstance().write(U3_READ_CARD);

        int length = IDCardSerialPortManager.getInstance().read(buffer, 1500, 100);

        result = new Result();

        if (length == 0) {
            result.confirmationCode = Result.TIME_OUT;//超时
            return result;
        }

        if (length == 11) {

            String s = DataUtils.bytesToHexString(buffer);
            if (s.substring(18, 20).equals("80")) {//未寻到卡
                result.confirmationCode = Result.FIND_FAIL_8084;//未寻到卡
            } else if (s.substring(18, 20).equals("41")) {//读卡失败
                result.confirmationCode = Result.FIND_FAIL_4145;//读卡失败
            } else {
                result.confirmationCode = Result.FIND_FAIL_other;//其他失败
                //LocalLog.i("其他失败  ---  " + s);
            }
            Log.i(TAG, "失败原因: " + s.substring(0, 20));
            Log.i(TAG, "result sfz=" + toHexString(buffer));
            return result;
        }


        if (length > 0) {
            byte[] data = new byte[length];
            System.arraycopy(buffer, 0, data, 0, length);
            Log.i(TAG, length + "-----" + DataUtils.toHexString(data));
            result.data = data;
        }

        people = decode(buffer, length);

        if (people == null) {
            result.confirmationCode = Result.FIND_FAIL;
            //LocalLog.i("解码失败  ---  ");
        } else {
            result.confirmationCode = Result.SUCCESS;
            result.resultInfo = people;
        }

        //buffer = new byte[DATA_SIZE];
        Arrays.fill(buffer, (byte) 0);//清空buffer数组
        return result;
    }


    /**
     * 移远U3，U8读法
     * <p>
     * 读取身份证信息，此方法为阻塞的，建议放在子线程处理(带自动规避功能！)
     *
     * @return Result：获取身份证信息结果
     */
    @Deprecated
    public Result readSFZ(int count) {
        Log.i(TAG, "-------------------ParseSFZAPI U3、U8读身份证---------------------");
        int countNum = 0;

        People people = null;
        IDCardSerialPortManager.getInstance().clearBuffer();

        IDCardSerialPortManager.getInstance().write(U3_READ_CARD);

        int length = IDCardSerialPortManager.getInstance().read(buffer, 1500, 100);

        result = new Result();

        long time = System.currentTimeMillis();

        //b为真就不规避了，为假规避
        boolean b = (length == SECOND_GENERATION_CARD || length == THIRD_GENERATION_CARD);


        while (!b && countNum <= count) {
            countNum++;
            // add by yjj at 2017/1/13 11:00
            Log.e(TAG, countNum + " 规避了" + length + "------内容:----" + DataUtils.bytesToHexString(buffer));

            Log.d(TAG, "SFZ:" + IDCardSerialPortManager.getInstance().isOpen());

            IDCardSerialPortManager.getInstance().clearBuffer();

            IDCardSerialPortManager.getInstance().write(U3_READ_CARD);

            length = IDCardSerialPortManager.getInstance().read(buffer, 1500, 100);
            b = (length == SECOND_GENERATION_CARD || length == THIRD_GENERATION_CARD);
        }


        if (length == 0) {
            result.confirmationCode = Result.TIME_OUT;//超时
            return result;
        }

        if (length == 11) {
            String s = DataUtils.bytesToHexString(buffer);
            if (s.substring(18, 20).equals("80")) {
                //未寻到卡
                result.confirmationCode = Result.FIND_FAIL_8084;
                Log.e(TAG, "---------------IDCard---8084错误-------------------------------------");
            } else if (s.substring(18, 20).equals("41")) {
                //读卡失败
                result.confirmationCode = Result.FIND_FAIL_4145;
                Log.e(TAG, "---------------IDCard---4145错误-------------------------------------");
            } else {
                //其他失败
                result.confirmationCode = Result.FIND_FAIL_other;
                Log.e(TAG, "---------------IDCard---其他失败-------------------------------------");
            }
            return result;
        }


        if (length > 0) {
            byte[] data = new byte[length];
            System.arraycopy(buffer, 0, data, 0, length);
            Log.i(TAG, "IDCardRaws: length: " + length + "-------------Raw: " + DataUtils.toHexString(data));
            result.data = data;
        }

        people = decode(buffer, length);

        if (people == null) {
            result.confirmationCode = Result.FIND_FAIL;
            Log.e(TAG, "-------------------------IDCard--解码失败-------------------------------------");
        } else {
            result.confirmationCode = Result.SUCCESS;
            result.resultInfo = people;
        }

        //清空buffer数组
        //buffer = new byte[DATA_SIZE];
        Arrays.fill(buffer, (byte) 0);
        return result;
    }


    /**
     * 海派CFON640,A370读法
     * <p>
     * 读取身份证信息，此方法为阻塞的，建议放在子线程处理
     *
     * @param cardType
     * @return Result：获取身份证信息结果
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Deprecated
    public Result read(int cardType) {

        People people = null;
        if (cardType == SECOND_GENERATION_CARD) {
            IDCardSerialPortManager.getInstance().write(command1);
        } else if (cardType == THIRD_GENERATION_CARD) {
            IDCardSerialPortManager.getInstance().write(command3);
        } else {
            return null;
        }

        int length = 0;
        result = new Result();
        IDCardSerialPortManager.switchRFID = false;
        length = IDCardSerialPortManager.getInstance().read(buffer, 3000, 100);

        long time = System.currentTimeMillis();

        while (length == 0 && (System.currentTimeMillis() - time) <= 4000 && IDCardSerialPortManager.getInstance().isOpen()) {
            // add by yjj at 2017/1/13 11:00
            try {
                IDCardSerialPortManager.getInstance().closeSerialPort(cw.getDeviceModel());
                Thread.sleep(1500);
                Log.d(TAG, "SFZ:" + IDCardSerialPortManager.getInstance().isOpen());
                IDCardSerialPortManager.getInstance().openSerialPort(cw.getDeviceModel());
                Thread.sleep(1500);
                Log.d(TAG, "SFZ:" + IDCardSerialPortManager.getInstance().isOpen());
                if (cardType == SECOND_GENERATION_CARD) {
                    IDCardSerialPortManager.getInstance().write(command1);
                } else if (cardType == THIRD_GENERATION_CARD) {
                    IDCardSerialPortManager.getInstance().write(command3);
                } else {
                    return null;
                }
                length = IDCardSerialPortManager.getInstance().read(buffer, 3000, 100);
            } catch (Exception e) {
                e.printStackTrace();
            }
            // end
        }

        if (length == 0) {
            result.confirmationCode = Result.TIME_OUT;
            return result;
        }

        if (length == 1297 || length == 1295 && cardType == THIRD_GENERATION_CARD) {
            result.confirmationCode = Result.NO_THREECARD;
            //return result;
        }

        people = decode(buffer, length);
        if (people == null) {
            result.confirmationCode = Result.FIND_FAIL;
        } else {
            result.confirmationCode = Result.SUCCESS;
            result.resultInfo = people;
        }
        return result;
    }


    /**
     * 全平台通用的
     *
     * @param device
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public Result readIDCard(int device) {

        switch (device) {

            case Device_CPOS800:
                return readByCommand(CPOS9800_READ_IDCARD);
            case Device_CFON600:
                //CFON600没有身份证功能
                break;
            case Device_CFON640:
                //读CFON640，和海派A370一样
                return read(THIRD_GENERATION_CARD);
            case Device_A370_M4G5:
                //读A370，和海派CFON640一样
                return read(THIRD_GENERATION_CARD);
            case Device_U1:
                //U1没有身份证功能

                break;
            case Device_U3:
                //U3读卡命令和U8一样，串口也一样

                return readByCommand(U3_READ_CARD);
            case Device_U8:
                //U8读卡命令和U3一样，串口也一样
                return readByCommand(U3_READ_CARD);

            case Device_A370_CW20:
                //新A370读卡命令和U3一样，串口也一样,上电路径不一样
                return readByCommand(U3_READ_CARD);


        }
        return null;
    }


    /**
     * 读卡方法，发送读卡命令
     *
     * @param command
     * @return
     */
    private Result readByCommand(byte[] command) {
        People people = null;
        result = new Result();

        IDCardSerialPortManager.getInstance().write(command);
        int length = IDCardSerialPortManager.getInstance().read(buffer, 2500, 200);
        long time = System.currentTimeMillis();

        //b为真就不规避了，为假规避
        boolean b = (length == SECOND_GENERATION_CARD || length == THIRD_GENERATION_CARD || length == SECOND_GENERATION_CARD_OLD);

        int countNum = 0;

        while (!b && countNum <= 3) {
            countNum++;
            Log.e(TAG, countNum + " 规避了" + length + "------内容:----" + DataUtils.bytesToHexString(buffer));

            IDCardSerialPortManager.getInstance().clearBuffer();

            IDCardSerialPortManager.getInstance().write(command);

            length = IDCardSerialPortManager.getInstance().read(buffer, 1500, 100);
            b = (length == SECOND_GENERATION_CARD || length == THIRD_GENERATION_CARD || length == SECOND_GENERATION_CARD_OLD);
        }

        if (length == 0) {
            //超时
            result.confirmationCode = Result.TIME_OUT;
            return result;
        }

        if (length == 11) {
            String s = DataUtils.bytesToHexString(buffer);
            if (s.substring(18, 20).equals("80")) {
                //未寻到卡
                result.confirmationCode = Result.FIND_FAIL_8084;
                Log.e(TAG, "---------------IDCard---8084错误-------------------------------------");
            } else if (s.substring(18, 20).equals("41")) {
                //读卡失败
                result.confirmationCode = Result.FIND_FAIL_4145;
                Log.e(TAG, "---------------IDCard---4145错误-------------------------------------");
            } else {
                //其他失败
                result.confirmationCode = Result.FIND_FAIL_other;
                Log.e(TAG, "---------------IDCard---其他失败-------------------------------------");
            }
            return result;
        }


        if (length > 0) {
            byte[] data = new byte[length];
            System.arraycopy(buffer, 0, data, 0, length);
            Log.i(TAG, "IDCardRaws: length: " + length + "-------------Raw: " + DataUtils.toHexString(data));
            result.data = data;
        }

        people = decode(buffer, length);

        if (people == null) {
            result.confirmationCode = Result.FIND_FAIL;
            Log.e(TAG, "-------------------------IDCard--解码失败-------------------------------------");
        } else {
            result.confirmationCode = Result.SUCCESS;
            result.resultInfo = people;
        }

        //清空buffer数组
        //buffer = new byte[DATA_SIZE];
        Arrays.fill(buffer, (byte) 0);

        return result;
    }

    @Deprecated
    public Result readModule() {

        result = new Result();
        IDCardSerialPortManager.getInstance().write(U3_READ_ID);
        byte[] buffer = new byte[DATA_SIZE];
        int length = IDCardSerialPortManager.getInstance().read(buffer, 3000, 300);
        if (length == 0) {
            result.confirmationCode = Result.TIME_OUT;
            return result;
        }
        byte[] module = new byte[length];
        System.arraycopy(buffer, 0, module, 0, length);
        String data = DataUtils.toHexString(module);
        if (length > 10) {
            String prefix = data.substring(0, 20);
            if (prefix.equalsIgnoreCase(MODULE_SUCCESS)) {
                String temp1 = DataUtils.toHexString1(module[10]);
                String temp2 = DataUtils.toHexString1(module[12]);
                byte[] temp3 = new byte[4];
                System.arraycopy(module, 14, temp3, 0, temp3.length);
                reversal(temp3);
                byte[] temp4 = new byte[4];
                System.arraycopy(module, 18, temp4, 0, temp4.length);
                reversal(temp4);
                byte[] temp5 = new byte[4];
                System.arraycopy(module, 22, temp5, 0, temp5.length);
                reversal(temp5);
                StringBuffer sb = new StringBuffer();
                sb.append(temp1);
                sb.append(".");
                sb.append(temp2);
                sb.append("-");
                sb.append(byte2Int(temp3));
                sb.append("-");
                String str4 = Long.toString(byte2Int(temp4));
                for (int i = 0; i < 10 - str4.length(); i++) {
                    sb.append("0");
                }
                sb.append(str4);
                sb.append("-");
                String str5 = Long.toString(byte2Int(temp5));
                for (int i = 0; i < 10 - str5.length(); i++) {
                    sb.append("0");
                }
                sb.append(str5);
                result.confirmationCode = Result.SUCCESS;
                result.resultInfo = sb.toString();
                return result;
            }
        }
        result.confirmationCode = Result.FIND_FAIL;
        return result;
    }

    /**
     * 640平台
     *
     * @return String 返回id
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String readCardID() {
        if (!IDCardSerialPortManager.switchRFID) {
            IDCardSerialPortManager.getInstance().switchStatus();
        }
        turnOff();
        Log.i(TAG, "readCardID");
        if (sendReceive(SFZ_ID_COMMAND1, SFZ_ID_RESPONSE1)) {
            if (sendReceive(SFZ_ID_COMMAND2, SFZ_ID_RESPONSE2)) {
                return sendReceive(SFZ_ID_COMMAND3);
            }
        }
        return "";
    }


    /**
     * U3平台
     *
     * @return String 返回id
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public String readCardID_U3() {
        //turnOff();
        Log.i(TAG, "readCardID");
        if (sendReceive(U3_READ_ID, "9000")) {
            String s = DataUtils.bytesToHexString(buffer);
            //String dataStr = new String(buffer, 0, length);
            Log.i(TAG, "dataStr=" + s);
            return s.substring(0, 16);
        }
        return "";
    }


    // 关闭天线厂
    private boolean turnOff() {
        byte[] command = TURN_OFF.getBytes();
        IDCardSerialPortManager.getInstance().write(command);
        int length = IDCardSerialPortManager.getInstance().read(buffer, 3000, 10);
        String str = "";
        if (length > 0) {
            str = new String(buffer, 0, length).trim();
            if (str.equals(TURN_OFF_RESPONSE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 发送指令
     *
     * @param command
     * @param response
     * @return
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private boolean sendReceive(byte[] command, String response) {
        IDCardSerialPortManager.getInstance().write(command);
        int length = IDCardSerialPortManager.getInstance().read(buffer, 3000, 10);
        if (length > 0) {


            /*switch (cw.getAndroidVersion()) {
                case cw.deviceSysVersion.O:
                    String dataStr = new String(buffer, 0, length).trim();
                    Log.i(TAG, "dataStr=" + dataStr);
                    if (dataStr.startsWith(response)) {
                        return true;
                    }
                case cw.deviceSysVersion.U:
                    dataStr = DataUtils.bytesToHexString(buffer).substring(0, 20);
                    Log.i(TAG, "dataStr=" + dataStr);
                    if (dataStr.endsWith(response)) {
                        return true;
                    }
                    break;
            }*/
            String dataStr;

            switch (cw.getDeviceModel()) {
                case cw.Device_A370_M4G5:
                    dataStr = new String(buffer, 0, length).trim();
                    Log.i(TAG, "dataStr=" + dataStr);
                    if (dataStr.startsWith(response)) {
                        return true;
                    }
                    break;

                case cw.Device_CFON640:
                    dataStr = new String(buffer, 0, length).trim();
                    Log.i(TAG, "dataStr=" + dataStr);
                    if (dataStr.startsWith(response)) {
                        return true;
                    }
                    break;
                case cw.Device_CPOS800:
                    dataStr = new String(buffer, 0, length).trim();
                    Log.i(TAG, "dataStr=" + dataStr);
                    if (dataStr.startsWith(response)) {
                        return true;
                    }
                    break;
                case cw.Device_U1:
                    dataStr = DataUtils.bytesToHexString(buffer).substring(0, 20);
                    Log.i(TAG, "dataStr=" + dataStr);
                    if (dataStr.endsWith(response)) {
                        return true;
                    }
                    break;
                case cw.Device_U3:
                    dataStr = DataUtils.bytesToHexString(buffer).substring(0, 20);
                    Log.i(TAG, "dataStr=" + dataStr);
                    if (dataStr.endsWith(response)) {
                        return true;
                    }
                    break;
            }

        }
        return false;
    }


    private String sendReceive(byte[] command) {
        IDCardSerialPortManager.getInstance().write(command);
        int length = IDCardSerialPortManager.getInstance().read(buffer, 3000, 10);
        if (length > 0) {
            String dataStr = new String(buffer, 0, length).trim();
            Log.i(TAG, "dataStr=" + dataStr);
            if (dataStr.endsWith("9000")) {
                return dataStr.substring(0, 16);
            }
        }
        return "";
    }

    /**
     * 逆序
     *
     * @param data
     */
    private void reversal(byte[] data) {
        int length = data.length;
        for (int i = 0; i < length / 2; i++) {
            byte temp = data[i];
            data[i] = data[length - 1 - i];
            data[length - 1 - i] = temp;
        }
    }

    private long byte2Int(byte[] data) {
        int intValue = 0;
        for (int i = 0; i < data.length; i++) {
            intValue += (data[i] & 0xff) << (8 * (3 - i));
        }
        long temp = intValue;
        temp <<= 32;
        temp >>>= 32;
        return temp;
    }

    /**
     * aa aa aa 96 69 09 0a 00 00 90 ......
     * <p>
     * buffer  10+2+2+2+256+1024+(1024)+1  ---->二代证长度为1297；三代证长度2321
     * <p>
     * 10:前十个字节;2：文本字节长度;  2: 照片字节长度; 2: 指纹字节长度；256: 文本;1024:照片;1024:指纹;1:?
     *
     * @param buffer
     * @param length
     * @return
     */
    private People decode(byte[] buffer, int length) {

        if (buffer == null) {
            return null;
        }
        byte[] b = new byte[10];
        System.arraycopy(buffer, 0, b, 0, 10);
        String result = toHexString(b);
        Log.i(TAG, "result sfz=" + result);
        People people = null;
        if (result.equalsIgnoreCase(CARD_SUCCESS) || result.equalsIgnoreCase(CARD_SUCCESS3) || result.equalsIgnoreCase(CARD_SUCCESS2)) {
            byte[] data = new byte[buffer.length - 10];
            //System.arraycopy(buffer, 10, data, 0, buffer.length - 10);

            if (length == SECOND_GENERATION_CARD_OLD) {
                Log.e(TAG, "---------------------IDCard 数据为1295位:老机器CFON640，A370，CPOS800----------------------------------------");
                System.arraycopy(buffer, 10, data, 0, buffer.length - 10);
                people = decodeInfo(data, length);

            } else if (length == SECOND_GENERATION_CARD) {
                //二代证1297
                Log.e(TAG, "---------------------IDCard 数据为1297位（不带指纹）: U3，U8----------------------------------------");
                System.arraycopy(buffer, 10, data, 0, buffer.length - 10);
                people = decodeInfo2(data, length);

            } else if (length == THIRD_GENERATION_CARD) {
                //三代证2321
                Log.e(TAG, "---------------------IDCard 数据为2321位（带指纹）:U3，U8----------------------------------------");
                System.arraycopy(buffer, 10, data, 0, buffer.length - 10);
                people = decodeInfo(data, length);

                //获取指纹,判断左右手哪个拇指
                String[] finger = getFinger(people);
                Log.i(TAG, finger[0] + "-----" + finger[1]);
                this.result.finger = finger[0] + "\n" + finger[1];
                people.setWhichFinger(finger);

            } else {
                return null;
            }

        } else if (result.equalsIgnoreCase(TIMEOUT_RETURN)) {
            Log.d(TAG, "----------------------读卡超时:TIMEOUT_RETURN---------------------------");
        } else if (result.startsWith(CMD_ERROR)) {
            Log.d(TAG, "----------------------------------CMD_ERROR-------------------------");
        }

        return people;
    }


    private Map<String, String> FingerMap = new HashMap<String, String>() {
        {
            put("0b", "右手拇指");
            put("0c", "右手食指");
            put("0d", "右手中指");
            put("0e", "右手无名指");
            put("0f", "右手小指");

            put("10", "左手拇指");
            put("11", "右手食指");
            put("12", "左手中指");
            put("13", "左手无名指");
            put("14", "右手食小指");

            put("61", "右手不确定指位");
            put("62", "左手不确定指位");

            put("63", "其他不确定指位");

        }
    };

    /**
     * 哪个手指
     *
     * @param people
     * @return
     */
    private String[] getFinger(People people) {

        String[] whichFinger = new String[2];


        if (people == null) {
            return null;
        }

        String FingerModel = DataUtils.bytesToHexString(people.getModel());
        Log.i(TAG, "---指纹数据长度---" + FingerModel.length() + "---指纹数据---" + FingerModel);

        Log.i(TAG, "--1--" + FingerModel.substring(10, 12) + "--2--" + FingerModel.substring(1024 + 10, 1024 + 12));

        String oneFinger = FingerModel.substring(10, 12);
        String twoFinger = FingerModel.substring(1024 + 10, 1024 + 12);

        whichFinger[0] = FingerMap.get(oneFinger);
        whichFinger[1] = FingerMap.get(twoFinger);

        return whichFinger;
    }


    /**
     * 三代身份证信息解码
     * <p>
     * THIRD_GENERATION_CARD = 2321;
     *
     * @param buffer
     * @param length
     * @return
     */
    private People decodeInfo(byte[] buffer, int length) {

        short textSize = getShort(buffer[0], buffer[1]);

        short imageSize = getShort(buffer[2], buffer[3]);

        short modelSize = 0;

        byte[] model = null;

        short skipLength = 0;

        if (length == THIRD_GENERATION_CARD) {
            modelSize = getShort(buffer[4], buffer[5]);  //
            skipLength = 2;
            model = new byte[modelSize];
            System.arraycopy(buffer, 4 + skipLength + textSize + imageSize, model, 0, modelSize);
        }

        byte[] text = new byte[textSize];

        System.arraycopy(buffer, 4 + skipLength, text, 0, textSize);

        byte[] image = new byte[imageSize];

        System.arraycopy(buffer, 4 + skipLength + textSize, image, 0, imageSize);

        People people = null;

        try {
            String temp = null;
            people = new People();
            people.setHeadImage(image);
            // 姓名
            temp = new String(text, 0, 30, "UTF-16LE").trim();
            people.setPeopleName(temp);

            // 性别
            temp = new String(text, 30, 2, "UTF-16LE");
            if (temp.equals("1")) {
                temp = "男";
            } else {
                temp = "女";
            }
            people.setPeopleSex(temp);

            // 民族
            temp = new String(text, 32, 4, "UTF-16LE");
            try {
                int code = Integer.parseInt(temp.toString());
                temp = decodeNation(code);
            } catch (Exception e) {
                temp = "";
            }
            people.setPeopleNation(temp);

            // 出生
            temp = new String(text, 36, 16, "UTF-16LE").trim();
            people.setPeopleBirthday(temp);

            // 住址
            temp = new String(text, 52, 70, "UTF-16LE").trim();
            people.setPeopleAddress(temp);

            // 身份证号
            temp = new String(text, 122, 36, "UTF-16LE").trim();
            people.setPeopleIDCode(temp);

            // 签发机关
            temp = new String(text, 158, 30, "UTF-16LE").trim();
            people.setDepartment(temp);

            // 有效起始日期
            temp = new String(text, 188, 16, "UTF-16LE").trim();
            people.setStartDate(temp);

            // 有效截止日期
            temp = new String(text, 204, 16, "UTF-16LE").trim();
            people.setEndDate(temp);

        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            return null;
        }
        people.setPhoto(parsePhoto(image));
        people.setModel(model);
        return people;
    }

    /**
     * 二代身份证信息解码
     * <p>
     * SECOND_GENERATION_CARD = 1297; 不包括指纹
     *
     * @param buffer
     * @param length
     * @return
     */
    private People decodeInfo2(byte[] buffer, int length) {

        short textSize = getShort(buffer[0], buffer[1]);

        short imageSize = getShort(buffer[2], buffer[3]);

        byte[] model = null;

        short skipLength = 2;

        byte[] text = new byte[textSize];

        System.arraycopy(buffer, 4 + skipLength, text, 0, textSize);

        byte[] image = new byte[imageSize];

        System.arraycopy(buffer, 4 + skipLength + textSize, image, 0, imageSize);

        People people = null;

        try {
            String temp = null;
            people = new People();
            people.setHeadImage(image);
            // 姓名
            temp = new String(text, 0, 30, "UTF-16LE").trim();
            people.setPeopleName(temp);

            // 性别
            temp = new String(text, 30, 2, "UTF-16LE");
            if (temp.equals("1")) {
                temp = "男";
            } else {
                temp = "女";
            }
            people.setPeopleSex(temp);

            // 民族
            temp = new String(text, 32, 4, "UTF-16LE");
            try {
                int code = Integer.parseInt(temp.toString());
                temp = decodeNation(code);
            } catch (Exception e) {
                temp = "";
            }
            people.setPeopleNation(temp);

            // 出生
            temp = new String(text, 36, 16, "UTF-16LE").trim();
            people.setPeopleBirthday(temp);

            // 住址
            temp = new String(text, 52, 70, "UTF-16LE").trim();
            people.setPeopleAddress(temp);

            // 身份证号
            temp = new String(text, 122, 36, "UTF-16LE").trim();
            people.setPeopleIDCode(temp);

            // 签发机关
            temp = new String(text, 158, 30, "UTF-16LE").trim();
            people.setDepartment(temp);

            // 有效起始日期
            temp = new String(text, 188, 16, "UTF-16LE").trim();
            people.setStartDate(temp);

            // 有效截止日期
            temp = new String(text, 204, 16, "UTF-16LE").trim();
            people.setEndDate(temp);

        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
            return null;
        }
        people.setPhoto(parsePhoto2(image));
        people.setModel(model);
        return people;
    }

    private String decodeNation(int code) {
        String nation;
        switch (code) {
            case 1:
                nation = "汉";
                break;
            case 2:
                nation = "蒙古";
                break;
            case 3:
                nation = "回";
                break;
            case 4:
                nation = "藏";
                break;
            case 5:
                nation = "维吾尔";
                break;
            case 6:
                nation = "苗";
                break;
            case 7:
                nation = "彝";
                break;
            case 8:
                nation = "壮";
                break;
            case 9:
                nation = "布依";
                break;
            case 10:
                nation = "朝鲜";
                break;
            case 11:
                nation = "满";
                break;
            case 12:
                nation = "侗";
                break;
            case 13:
                nation = "瑶";
                break;
            case 14:
                nation = "白";
                break;
            case 15:
                nation = "土家";
                break;
            case 16:
                nation = "哈尼";
                break;
            case 17:
                nation = "哈萨克";
                break;
            case 18:
                nation = "傣";
                break;
            case 19:
                nation = "黎";
                break;
            case 20:
                nation = "傈僳";
                break;
            case 21:
                nation = "佤";
                break;
            case 22:
                nation = "畲";
                break;
            case 23:
                nation = "高山";
                break;
            case 24:
                nation = "拉祜";
                break;
            case 25:
                nation = "水";
                break;
            case 26:
                nation = "东乡";
                break;
            case 27:
                nation = "纳西";
                break;
            case 28:
                nation = "景颇";
                break;
            case 29:
                nation = "柯尔克孜";
                break;
            case 30:
                nation = "土";
                break;
            case 31:
                nation = "达斡尔";
                break;
            case 32:
                nation = "仫佬";
                break;
            case 33:
                nation = "羌";
                break;
            case 34:
                nation = "布朗";
                break;
            case 35:
                nation = "撒拉";
                break;
            case 36:
                nation = "毛南";
                break;
            case 37:
                nation = "仡佬";
                break;
            case 38:
                nation = "锡伯";
                break;
            case 39:
                nation = "阿昌";
                break;
            case 40:
                nation = "普米";
                break;
            case 41:
                nation = "塔吉克";
                break;
            case 42:
                nation = "怒";
                break;
            case 43:
                nation = "乌孜别克";
                break;
            case 44:
                nation = "俄罗斯";
                break;
            case 45:
                nation = "鄂温克";
                break;
            case 46:
                nation = "德昂";
                break;
            case 47:
                nation = "保安";
                break;
            case 48:
                nation = "裕固";
                break;
            case 49:
                nation = "京";
                break;
            case 50:
                nation = "塔塔尔";
                break;
            case 51:
                nation = "独龙";
                break;
            case 52:
                nation = "鄂伦春";
                break;
            case 53:
                nation = "赫哲";
                break;
            case 54:
                nation = "门巴";
                break;
            case 55:
                nation = "珞巴";
                break;
            case 56:
                nation = "基诺";
                break;
            case 97:
                nation = "其他";
                break;
            case 98:
                nation = "外国血统中国籍人士";
                break;
            default:
                nation = "";
        }
        return nation;
    }

    /**
     * 数组转成16进制字符串
     *
     * @param b
     * @return
     */
    private static String toHexString(byte[] b) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            buffer.append(toHexString1(b[i]));
        }
        return buffer.toString();
    }

    private static String toHexString1(byte b) {
        String s = Integer.toHexString(b & 0xFF);
        if (s.length() == 1) {
            return "0" + s;
        } else {
            return s;
        }
    }

    private short getShort(byte b1, byte b2) {
        short temp = 0;
        temp |= (b1 & 0xff);
        temp <<= 8;
        temp |= (b2 & 0xff);
        return temp;
    }

    private byte[] parsePhoto(byte[] wltdata) {

        SfzFileManager sfzFileManager = new SfzFileManager(src);
        if (sfzFileManager.initDB(this.m_Context, R.raw.base, R.raw.license)) {
            int ret = IDCReaderSDK.Init();
            if (0 == ret) {
                ret = IDCReaderSDK.unpack(buffer);
                if (1 == ret) {
                    byte[] image = IDCReaderSDK.getPhoto();
                    Log.e(TAG, "-----------------parsePhoto:IDCReaderSDK.getPhoto();---------------------------------");
                    return image;
                } else {
                    Log.e(TAG, "-----------------parsePhoto:IDCReaderSDK.unpack()失败---------------------------------");
                }
            } else {
                Log.e(TAG, "-----------------parsePhoto:IDCReaderSDK.Init()初始化失败---------------------------------");
            }
        } else {
            Log.e(TAG, "-----------------parsePhoto:sfzFileManager.initDB初始化失败---------------------------------");
        }
        return null;
    }

    private byte[] parsePhoto2(byte[] wltdata) {

        SfzFileManager sfzFileManager = new SfzFileManager(src);
        if (sfzFileManager.initDB(this.m_Context, R.raw.base, R.raw.license)) {
            int ret = IDCReaderSDK.Init();
            if (0 == ret) {
                //将1297转为1295
                byte[] buf = new byte[1295];
                System.arraycopy(buffer, 0, buf, 0, 13);
                System.arraycopy(buffer, 15, buf, 13, 1281);

                ret = IDCReaderSDK.unpack(buf);
                if (1 == ret) {
                    byte[] image = IDCReaderSDK.getPhoto();
                    return image;
                } else {
                    Log.e(TAG, "-----------------parsePhoto:IDCReaderSDK.Init()初始化失败---------------------------------");
                }
            } else {
                Log.e(TAG, "-----------------parsePhoto:IDCReaderSDK.Init()初始化失败---------------------------------");
            }
        } else {
            Log.e(TAG, "-----------------parsePhoto:sfzFileManager.initDB初始化失败---------------------------------");
        }
        return null;
    }

    /**
     * 获得指定文件的byte数组
     */
    private static byte[] getBytes(String filePath) {
        byte[] buffer = null;
        try {
            File file = new File(filePath);
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1000];
            int n;
            while ((n = fis.read(b)) != -1) {
                bos.write(b, 0, n);
            }
            fis.close();
            bos.close();
            buffer = bos.toByteArray();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return buffer;
    }


    /**
     * 身份证信息封装
     */
    public static class People {
        /**
         * 姓名
         */
        private String peopleName;

        /**
         * 性别
         */
        private String peopleSex;

        /**
         * 民族
         */
        private String peopleNation;

        /**
         * 出生日期
         */
        private String peopleBirthday;

        /**
         * 住址
         */
        private String peopleAddress;

        /**
         * 身份证号
         */
        private String peopleIDCode;

        /**
         * 签发机关
         */
        private String department;

        /**
         * 有效期限：开始
         */
        private String startDate;

        /**
         * 有效期限：结束
         */
        private String endDate;

        /**
         * 身份证头像
         */
        private byte[] photo;

        /**
         * 没有解析成图片的数据大小一般为1024字节
         */
        private byte[] headImage;

        /**
         * 三代证指纹模板数据，正常位1024，如果为null，说明为二代证，没有指纹模板数据
         */
        private byte[] model;

        /**
         * 哪个手指
         */
        private String[] whichFinger;


        public String getPeopleName() {
            return peopleName;
        }

        public void setPeopleName(String peopleName) {
            this.peopleName = peopleName;
        }

        public String getPeopleSex() {
            return peopleSex;
        }

        public void setPeopleSex(String peopleSex) {
            this.peopleSex = peopleSex;
        }

        public String getPeopleNation() {
            return peopleNation;
        }

        public void setPeopleNation(String peopleNation) {
            this.peopleNation = peopleNation;
        }

        public String getPeopleBirthday() {
            return peopleBirthday;
        }

        public void setPeopleBirthday(String peopleBirthday) {
            this.peopleBirthday = peopleBirthday;
        }

        public String getPeopleAddress() {
            return peopleAddress;
        }

        public void setPeopleAddress(String peopleAddress) {
            this.peopleAddress = peopleAddress;
        }

        public String getPeopleIDCode() {
            return peopleIDCode;
        }

        public void setPeopleIDCode(String peopleIDCode) {
            this.peopleIDCode = peopleIDCode;
        }

        public String getDepartment() {
            return department;
        }

        public void setDepartment(String department) {
            this.department = department;
        }

        public String getStartDate() {
            return startDate;
        }

        public void setStartDate(String startDate) {
            this.startDate = startDate;
        }

        public String getEndDate() {
            return endDate;
        }

        public void setEndDate(String endDate) {
            this.endDate = endDate;
        }

        public byte[] getPhoto() {
            return photo;
        }

        public void setPhoto(byte[] photo) {
            this.photo = photo;
        }

        public byte[] getHeadImage() {
            return headImage;
        }

        public void setHeadImage(byte[] headImage) {
            this.headImage = headImage;
        }

        public byte[] getModel() {
            return model;
        }

        public void setModel(byte[] model) {
            this.model = model;
        }


        public String[] getWhichFinger() {
            return whichFinger;
        }

        public void setWhichFinger(String[] whichFinger) {
            this.whichFinger = whichFinger;
        }


        @Override
        public String toString() {
            return "People{" +
                    "peopleName='" + peopleName + '\'' +
                    ", peopleSex='" + peopleSex + '\'' +
                    ", peopleNation='" + peopleNation + '\'' +
                    ", peopleBirthday='" + peopleBirthday + '\'' +
                    ", peopleAddress='" + peopleAddress + '\'' +
                    ", peopleIDCode='" + peopleIDCode + '\'' +
                    ", department='" + department + '\'' +
                    ", startDate='" + startDate + '\'' +
                    ", endDate='" + endDate + '\'' +
                    ", photo=" + Arrays.toString(photo) +
                    ", headImage=" + Arrays.toString(headImage) +
                    ", model=" + Arrays.toString(model) +
                    ", whichFinger=" + Arrays.toString(whichFinger) +
                    '}';
        }
    }


    /**
     * 读取结果封装
     */
    public static class Result {

        public static final int SUCCESS = 1;
        public static final int FIND_FAIL = 2;
        public static final int TIME_OUT = 3;
        public static final int OTHER_EXCEPTION = 4;
        public static final int NO_THREECARD = 5;


        public static final int FIND_FAIL_8084 = 6;  //寻找居民身份证失败
        public static final int FIND_FAIL_4145 = 7; //读取居民身份证操作失败
        public static final int FIND_FAIL_81 = 8; //选取居民身份证失败
        public static final int FIND_FAIL_91 = 9; //选取居民身份证失败
        public static final int FIND_FAIL_40 = 10; //无法识别居民身份证卡类型
        public static final int FIND_FAIL_24 = 11; //无法识别的错误
        public static final int FIND_FAIL_other = 12; //无法识别的错误
        public static final int FIND_FAIL_Length = 13; //获取长度


        /**
         * 确认码 1: 成功 2：失败 3: 超时 4：其它异常5:不是三代证
         */
        public int confirmationCode;

        /**
         * 结果集:当确认码为1时，再判断是否有结果
         */
        public Object resultInfo;

        public byte[] data;

        public String finger;

        public String RightHand;

    }


    /**
     * 指位代码封装
     */

    public static class FingerTemp {

        public static final String LeftThumb = "10";
        public static final String LeftIndexFinger = "11";
        public static final String LeftMiddleFinger = "12";
        public static final String LeftHandRingFinger = "13";
        public static final String LeftHandLittleFinger = "14";

        public static final String RightThumb = "0B";
        public static final String RightIndexFinger = "0C";
        public static final String RightMiddleFinger = "0D";
        public static final String RightHandRingFinger = "0E";
        public static final String RightHandLittleFinger = "0F";

        public static final String Right = "61";
        public static final String Left = "62";

        public static final String unKnown = "63";


    }
}