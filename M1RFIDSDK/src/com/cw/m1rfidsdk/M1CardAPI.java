package com.cw.m1rfidsdk;

import android.os.Build;
import android.util.Log;

import com.cw.serialportsdk.cw;
import com.cw.serialportsdk.utils.DataUtils;



public class M1CardAPI {

    private static final String TAG = "CW" + "M1CardAPI";

    //S50
    public static final int KEY_A = 1;
    //S70
    public static final int KEY_B = 2;


    /**
     * 为了兼容No response!\r\n 和 No responese!\r\n ，只判断前面部分"No respon"
     */
    private static final String NO_RESPONSE = "No respon";


    //发送数据包的前缀
    private static final String DATA_PREFIX = "c050605";

    private static final String FIND_CARD_ORDER = "01";// 寻卡指令
    private static final String PASSWORD_SEND_ORDER = "02";// 密码下发指令
    private static final String PASSWORD_VALIDATE_ORDER = "03";// 密码认证命令
    private static final String WRITE_DATA_ORDER = "05";// 写指令
    private static final String ENTER = "\r\n";// 换行符

    // 寻卡的指令包
    private static final String FIND_CARD = DATA_PREFIX + FIND_CARD_ORDER + ENTER;

    // 下发密码指令包(A，B段密码各12个’f‘)
    private static final String SEND_PASSWORD = DATA_PREFIX + PASSWORD_SEND_ORDER + "ffffffffffffffffffffffff" + ENTER;
    private static final String DEFAULT_PASSWORD = "ffffffffffff";
    private static final String FIND_SUCCESS = "0x00,";
    private static final String WRITE_SUCCESS = " Write Success!" + ENTER;
    public byte[] buffer = new byte[100];


    /**
     * 读卡号
     *
     * @return
     */
    @Deprecated
    public Result readCardNum() {

        Result result = null;

        switch (cw.getAndroidVersion()) {
            case Build.VERSION_CODES.N_MR1:
                result = readCardNum_U3();
                break;
            case Build.VERSION_CODES.M:
                result = readCardNum_640();
                break;

        }
        return result;
    }

    /**
     * 读卡号
     *
     * @return
     */
    public Result readCardNum(int device) {

        Result result = null;

        switch (device) {
            case cw.Device_U3:
                result = readCardNum_U3();
                break;
            case cw.Device_U8:

                result = readCardNum_U3();
                break;
            case cw.Device_CFON640:
                result = readCardNum_640();
                break;

        }
        return result;
    }


    //////////////////////////////////////////
    //////////////////////////////////////////


    private int receive(byte[] command, byte[] buffer) {
        int length = -1;
        if (!M1RFIDSerialPortManger.switchRFID) {
            M1RFIDSerialPortManger.getInstance().switchStatus();
        }
        sendCommand(command);
        length = M1RFIDSerialPortManger.getInstance().read(buffer, 1500, 5);
        return length;
    }

    private void sendCommand(byte[] command) {
        M1RFIDSerialPortManger.getInstance().write(command);
    }

    /**
     * 函数说明：获取密码类型对应的字节数据，默认密码类型为KEYA
     *
     * @param keyType
     * @return
     */
    private String getKeyTypeStr(int keyType) {
        String keyTypeStr = null;
        switch (keyType) {
            case KEY_A:
                keyTypeStr = "60";
                break;
            case KEY_B:
                keyTypeStr = "61";
                break;
            default:
                keyTypeStr = "60";
                break;
        }
        return keyTypeStr;
    }

    /**
     * 函数说明：转换扇区里块的地址为两位
     *
     * @param block 块号
     * @return
     */
    private String getZoneId(int block) {
        return DataUtils.byte2Hexstr((byte) block);
    }


    /**
     * 函数说明：读取M1卡卡号 Read the M1 card number
     *
     * @return Result
     */
    private Result readCardNum_640() {
        Log.i(TAG, "!!!!!!!!!!!!readCard");
        Result result = new Result();
        byte[] command = FIND_CARD.getBytes();


        int length = receive(command, buffer);
        if (length == 0) {
            result.confirmationCode = Result.TIME_OUT;
            return result;
        }
        String msg = "";
        msg = new String(buffer, 0, length);
        Log.i(TAG, "msg hex=" + msg);
        turnOff();
        if (msg.startsWith(FIND_SUCCESS)) {
            result.confirmationCode = Result.SUCCESS;
            result.num = msg.substring(FIND_SUCCESS.length());
        } else {
            result.confirmationCode = Result.FIND_FAIL;
        }
        return result;
    }

    /**
     * 函数说明：验证密码
     *
     * @param block
     * @param keyType
     * @param keyA
     * @param keyB
     * @return
     */
    public boolean validatePassword(int block, int keyType, String keyA, String keyB) {


        byte[] cmd = (DATA_PREFIX + PASSWORD_SEND_ORDER + keyA + keyB + ENTER).getBytes();// 下发密码指令
        int tempLength = receive(cmd, buffer);// 下发验证指令
        String verifyStr = new String(buffer, 0, tempLength);
        Log.i(TAG, "validatePassword verifyStr=" + verifyStr);
        byte[] command2 = (DATA_PREFIX + PASSWORD_VALIDATE_ORDER + getKeyTypeStr(keyType) + getZoneId(block) + ENTER).getBytes();
        int length = receive(command2, buffer);// 验证密码
        String msg = new String(buffer, 0, length);
        Log.i(TAG, "validatePassword msg=" + msg);
        String prefix = "0x00,\r\n";
        return msg.startsWith(prefix);

    }

    /**
     * 读取指定块号存储的数据，长度一般为16字节 Reads the specified number stored data, length of
     * 16 bytes
     *
     * @param startPosition block number
     * @return
     */
    public byte[][] read(int startPosition, int num) {

        byte[] command = {'c', '0', '5', '0', '6', '0', '5', '0', '4', '0',
                '0', '\r', '\n'};
        byte[][] pieceDatas = new byte[num][];
        for (int i = 0; i < num; i++) {
            char[] c = getZoneId(startPosition + i).toCharArray();
            command[9] = (byte) c[0];
            command[10] = (byte) c[1];
            int readTime = 0;
            int length = 0;
            String data = "";
            while (readTime < 3) {
                readTime++;
                length = receive(command, buffer);
                data = new String(buffer, 0, length);
                if (data != null && data.startsWith(NO_RESPONSE)) {
                    continue;
                } else {
                    break;
                }
            }
            Log.i(TAG, "read data=" + data + "   readTime=" + readTime);
            String[] split = data.split(";");
            String msg = "";
            if (split.length == 2) {
                int index = split[1].indexOf("\r\n");
                if (index != -1) {
                    msg = split[1].substring(0, index);
                }

                Log.i(TAG,
                        "split msg=" + msg + "  msg length=" + msg.length());
            }
            pieceDatas[i] = DataUtils.hexStringTobyte(msg);
        }

        return pieceDatas;


    }

    /**
     * 函数说明: 读取指定块号存储的数据，长度一般为16字节 Reads the specified number stored data,
     * length of 16 bytes
     *
     * @param startPosition 块号(S50 类型默认0-63)
     * @return 返回指定块号数据
     */
    public byte[] read(int startPosition) {

        switch (cw.getAndroidVersion()) {
            case cw.deviceSysVersion.O:

                byte[] command = {'c', '0', '5', '0', '6', '0', '5', '0', '4', '0',
                        '0', '\r', '\n'};
                byte[] pieceDatas = null;
                char[] c = getZoneId(startPosition).toCharArray();
                command[9] = (byte) c[0];
                command[10] = (byte) c[1];
                int readTime = 0;
                int length = 0;
                String data = "";
                while (readTime < 3) {
                    readTime++;
                    length = receive(command, buffer);
                    data = new String(buffer, 0, length);
                    if (data != null && data.startsWith(NO_RESPONSE)) {
                        continue;
                    } else {
                        break;
                    }
                }
                Log.i(TAG, "read data=" + data + "   readTime=" + readTime);
                String[] split = data.split(";");
                String msg = "";
                if (split.length == 2) {
                    int index = split[1].indexOf("\r\n");
                    if (index != -1) {
                        msg = split[1].substring(0, index);
                    }
                    Log.i(TAG, "split msg=" + msg + "  msg length=" + msg.length());
                }
                pieceDatas = DataUtils.hexStringTobyte(msg);
                return pieceDatas;

            case cw.deviceSysVersion.U:


                break;

        }
        return null;
    }

    /**
     * 向指定的块号写入数据，长度为16字节 Write data to the specified block, length is 16 bytes
     * argument should be data[i].length == num
     *
     * @param data
     * @param num
     * @param num  the number of block
     * @return
     */
    public boolean write(int block, int num, String data) {
        if (data.length() == 0) {
            return false;
        }
        for (int i = 0; i < num; i++) {
            byte[] command = (DATA_PREFIX + WRITE_DATA_ORDER + getZoneId(block)
                    + data + ENTER).getBytes();
            Log.i(TAG, "***write hexStr=" + DataUtils.toHexString(command));
            int length = receive(command, buffer);
            boolean isWrite = false;
            if (length > 0) {
                String writeResult = new String(buffer, 0, length);
                Log.i(TAG, "write result=" + writeResult);
                isWrite = M1CardAPI.WRITE_SUCCESS.equals(writeResult);
            }
            if (!isWrite) {
                return false;
            }
        }
        return true;
    }

    /**
     * 函数说明：修改密码
     *
     * @param block
     * @param num
     * @param data
     * @param keyType
     * @return
     */
    public boolean updatePwd(int block, int num, String data, int keyType) {
        if (data.length() == 0) {
            return false;
        }
        for (int i = 0; i < num; i++) {
            byte[] command = (DATA_PREFIX + WRITE_DATA_ORDER + getZoneId(block)
                    + makeCompletePassword(keyType, data) + ENTER).getBytes();
            Log.i(TAG, "***write hexStr=" + DataUtils.toHexString(command));
            int length = receive(command, buffer);
            boolean isWrite = false;
            if (length > 0) {
                String writeResult = new String(buffer, 0, length);
                Log.i(TAG, "write result=" + writeResult);
                isWrite = M1CardAPI.WRITE_SUCCESS.equals(writeResult);
            }
            if (!isWrite) {
                return false;
            }
        }
        return true;
    }

    /**
     * 函数说明：组装密码块
     *
     * @param keyType
     * @param passwordHexStr
     * @return
     */
    private String makeCompletePassword(int keyType, String passwordHexStr) {
        String completePasswordHexStr = "";
        switch (keyType) {
            case KEY_A:
                completePasswordHexStr = passwordHexStr + "ff078069"
                        + DEFAULT_PASSWORD;
                break;
            case KEY_B:
                completePasswordHexStr = DEFAULT_PASSWORD + "ff078069"
                        + completePasswordHexStr;
                break;
            default:
                break;
        }
        Log.i(TAG, "completePasswordHexStr == " + completePasswordHexStr);
        return completePasswordHexStr;
    }

    /**
     * 向指定的块号写入数据，长度为16字节 Write data to the specified block, length is 16 bytes
     *
     * @param data
     * @param position
     * @return
     */
    public boolean write(byte[] data, int position) {
        String hexStr = DataUtils.toHexString(data);
        byte[] command = (DATA_PREFIX + WRITE_DATA_ORDER + getZoneId(position)
                + hexStr + ENTER).getBytes();
        Log.i(TAG, "***write hexStr=" + hexStr);
        int length = receive(command, buffer);
        if (length > 0) {
            String writeResult = new String(buffer, 0, length);
            Log.i(TAG, "write result=" + writeResult);
            return M1CardAPI.WRITE_SUCCESS.equals(writeResult);
        }
        return false;
    }

    /**
     * 函数说明：关闭天线厂
     *
     * @return
     */
    public String turnOff() {
        // byte[] command = TURN_OFF.getBytes();
        // int length = receive(command, buffer);
        // String str = "";
        // if (length > 0) {
        // str = new String(buffer, 0, length);
        // }
        // return str;
        return "";
    }


    ///////////////////////////////////////////////
    ///////////////////////////////////////////////
    ///////////////////////////////////////////////
    ////////////////////////U3///////////////////////
    ///////////////////////////////////////////////
    ///////////////////////////////////////////////

    /**
     * 发送数据包的前缀
     */
    private final byte[] DATA_PRE = {(byte) 0xca, (byte) 0xdf, 0x06, 0x35};
    private final byte[] DATA_END = {(byte) 0xe3};


    /**
     * 0x01:获取卡id
     * 0x02:操作卡
     */
    private final byte[][] M1CMD = {{0x01}, {0x02}};

    public static final byte M1S50 = 0x00;
    public static final byte M1S70 = 0x01;

    /**
     * 卡的类型
     * 0x00:s50A
     * 0x01:s70B
     */
    private final byte[][] M1CARDTYPE = {{0x00}, {0x01}};

    /**
     * 0x30:读块
     * 0xa0::写
     * 0xc1::加1
     * 0xc0：减1
     * 0xc2：转存
     */
    private final byte[][] OPCMD = {{0x30}, {(byte) 0xa0}, {(byte) 0xc1}, {(byte) 0xc0}, {(byte) 0xc2}};

    private final String FINDSUCCESS = "060010";
    private final String WRITESUCCESS = "0600029000";

    /**
     * 函数说明：读取M1卡卡号 Read the M1 card number
     *
     * @return Result
     */
    /*private Result readCardNum_U3() {

        byte[] buffer2 = new byte[40];
        Log.i(TAG, "!!!!!!!!!!!!readCard");
        M1CardAPI.Result result = new M1CardAPI.Result();
        byte[] readCardNumCommand = byteMerger(DATA_PRE, new byte[]{0x00, 0x01}, M1CMD[0], DATA_END);
        int length = receive(readCardNumCommand, buffer2);

        if (length == 0) {
            result.confirmationCode = M1CardAPI.Result.TIME_OUT;
            return result;
        }

        String msg = DataUtils.bytesToHexString(buffer2);
        Log.i(TAG, "msg hex=" + msg);

        if (!msg.substring(0, 8).equals("06010104")) {
            result.confirmationCode = M1CardAPI.Result.SUCCESS;
            result.num = msg.substring(0, 8);
        } else {
            result.confirmationCode = M1CardAPI.Result.FIND_FAIL;
        }

        return result;
    }
*/

    /**
     * 函数说明：读取M1卡卡号 Read the M1 card number
     *
     * @return Result
     */
    private Result readCardNum_U3() {

        Log.i(TAG, "!!!!!!!!!!!!readCard");
        M1CardAPI.Result result = new M1CardAPI.Result();
        byte[] readCardNumCommand = byteMerger(DATA_PRE, new byte[]{0x00, 0x01}, M1CMD[0], DATA_END);
        int length = receive(readCardNumCommand, buffer);

        byte[] buffer2 = new byte[length];
        System.arraycopy(buffer, 0, buffer2, 0, length);


        if (length == 0) {
            result.confirmationCode = M1CardAPI.Result.TIME_OUT;
            return result;
        }

        String msg = DataUtils.bytesToHexString(buffer2);
        Log.i(TAG, "msg hex=" + msg);

        if (msg.startsWith("32303138313132315f56312e3000")) {
            result.confirmationCode = M1CardAPI.Result.FIND_FAIL;
            result.error = msg;
            return result;
        }

        if (!msg.substring(0, 8).equals("06010104")) {
            result.confirmationCode = M1CardAPI.Result.SUCCESS;
            result.num = msg.substring(0, 8);
        } else {
            result.confirmationCode = M1CardAPI.Result.FIND_FAIL;
        }

        return result;
    }


    /**
     * 操作指定块号存储的数据，长度一般为16字节 Reads the specified number stored data, length of
     *
     * @param optype    操作卡的方式：读id 0x01 、操作卡(读、写、加一、减一、转存) 0x02
     * @param cardType  卡的类型 s50A 0x00 /s70B 0x01
     * @param opcmd     读块0x30 / 写0xa0 / 加一0xc1 / 减一0xc0 / 转存0xc2
     * @param sector    扇区号 00-0F
     * @param blocknum  块号
     * @param secret    6个字节的密码  默认:ffffffffffff
     * @param writedata 要写入的数据
     * @return
     */
    public Result opBlock(byte optype, byte cardType, byte opcmd, byte sector, byte blocknum, String secret, String writedata) {

        M1CardAPI.Result result = new M1CardAPI.Result();

        byte[] data = byteMerger(optype, cardType, opcmd, sector, blocknum, blocknum);
        //byte[] data = new byte[]{0x02, 0x00, 0x30, 0x00, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        byte[] command = null;

        if (writedata == null) {
            //读操作
            command = byteMerger(DATA_PRE, new byte[]{0x00, 0x0c}, data, DataUtils.hexStringTobyte(secret), DATA_END);

            //byte[] command = new byte[]{(byte) 0xca, (byte) 0xdf, 0x06, 0x35, 0x00, 0x0c, 0x02, 0x00, 0x30, 0x00, 0x00, 0x00, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xe3};
            int length = 0;
            int count = 5;
            do {
                sendCommand(command);

                length = M1RFIDSerialPortManger.getInstance().read(buffer, 1500, 100);
                count--;
            } while (length == 0 && count != 0);
            String msg = DataUtils.bytesToHexString(buffer);
            Log.i(TAG, "msg hex=" + msg);


            if (FINDSUCCESS.equals(msg.substring(0, 6))) {
                result.confirmationCode = M1CardAPI.Result.SUCCESS;
                result.resultInfo = msg.substring(0 + 6, 32 + 6);
            } else {
                result.confirmationCode = M1CardAPI.Result.FIND_FAIL;
            }
            return result;

        } else {
            //写操作

            byte[] datas = byteMerger(data, DataUtils.hexStringTobyte(secret), DataUtils.hexStringTobyte(calibrationData(writedata)));

            Log.i(TAG, "----" + datas.length);

            byte[] bytes = DataUtils.int2Byte2(datas.length);


            command = byteMerger(DATA_PRE, bytes, datas, DATA_END);

            //command = new byte[]{(byte) 0xca, (byte) 0xdf, 0x06, 0x35, 0x00, 0x1b, 0x02, 0x00, (byte) 0xa0, 0x02, 0x01, 0x01, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xbb, (byte) 0xbb, (byte) 0xbb, (byte) 0xbb, (byte) 0xaa, (byte) 0xbb, (byte) 0xbb, (byte) 0xbb, (byte) 0xbb, (byte) 0xbb, (byte) 0xbb, (byte) 0xbb, (byte) 0xbb, (byte) 0xbb, (byte) 0xbb, (byte) 0xbb, (byte) 0xe3};


            int length = 0;
            int count = 5;
            do {
                sendCommand(command);
                length = M1RFIDSerialPortManger.getInstance().read(buffer, 1500, 100);
                count--;
            } while (length == 0 && count != 0);
            String msg = DataUtils.bytesToHexString(buffer);
            Log.i(TAG, "msg hex=" + msg);


            if (WRITESUCCESS.equals(msg.substring(0, 10))) {
                result.confirmationCode = M1CardAPI.Result.SUCCESS;
                result.resultInfo = msg.substring(0 + 6, 32 + 6);
            } else {
                result.confirmationCode = M1CardAPI.Result.FIND_FAIL;
            }
            return result;
        }
    }


    /**
     * 读指定块号存储的数据，长度一般为16字节 Reads the specified number stored data, length of
     *
     * @param cardType 卡的类型 s50A 0x00 /s70B 0x01
     * @param sector   扇区号 00-0F
     * @param blocknum 块号
     * @param secret   6个字节的密码  默认:ffffffffffff
     * @return
     */
    public Result readBlock(byte cardType, byte sector, byte blocknum, String secret) {
        return opBlock((byte) 0x02, cardType, (byte) 0x30, sector, blocknum, secret, null);
    }

    /**
     * 读指定块号存储的数据，长度一般为16字节 Reads the specified number stored data, length of
     *
     * @param cardType 卡的类型 s50A 0x00 /s70B 0x01
     * @param blocknum 块号
     * @param secret   6个字节的密码  默认:ffffffffffff
     * @return
     */
    public Result readBlock(byte cardType, byte blocknum, String secret) {
        return opBlock((byte) 0x02, cardType, (byte) 0x30, (byte) (blocknum / 4), (byte) (blocknum % 4), secret, null);
    }


    /**
     * 读指定块号存储的数据，长度一般为16字节 Reads the specified number stored data, length of
     *
     * @param cardType 卡的类型 s50A 0x00 /s70B 0x01
     * @param sector   扇区号 00-0F
     * @param blocknum 块号
     * @param secret   6个字节的密码  默认:ffffffffffff
     * @return
     */
    public Result writeBlock(byte cardType, byte sector, byte blocknum, String secret, String data) {
        return opBlock((byte) 0x02, cardType, (byte) 0xa0, sector, blocknum, secret, data);
    }

    /**
     * 读指定块号存储的数据，长度一般为16字节 Reads the specified number stored data, length of
     *
     * @param cardType 卡的类型 s50A 0x00 /s70B 0x01
     * @param blocknum 块号
     * @param secret   6个字节的密码  默认:ffffffffffff
     * @return
     */
    public Result writeBlock(byte cardType, byte blocknum, String secret, String data) {
        return opBlock((byte) 0x02, cardType, (byte) 0xa0, (byte) (blocknum / 4), (byte) (blocknum % 4), secret, data);
    }


    /**
     * 合并byte  
     *
     * @param data
     * @return
     */
    private byte[] byteMerger(byte... data) {

        byte[] byte_3 = new byte[data.length];

        for (int i = 0; i < data.length; i++) {
            byte_3[i] = data[i];
        }

        return byte_3;
    }


    /**
     * 合并byte数组
     *
     * @param data
     * @return
     */
    private byte[] byteMerger(byte[]... data) {

        byte[] byte_3;
        int len = 0;

        for (int i = 0; i < data.length; i++) {
            len += data[i].length;
        }
        byte_3 = new byte[len];

        int lastlen = 0;

        for (int i = 0; i < data.length; i++) {
            System.arraycopy(data[i], 0, byte_3, lastlen, data[i].length);
            lastlen += data[i].length;

        }
        return byte_3;
    }


    /**
     * 校准数据（调试发现原数据写入数据错乱）
     *
     * @param data 32位长度
     */
    private String calibrationData(String data) {
        if (data.length() != 32) {
            return data;
        }

        String[] strsl = new String[data.length() / 2];

        /**
         * 将32位data拆分--->16位
         */
        for (int i = 0; i < data.length() / 2; i++) {
            strsl[i] = data.substring(2 * i, 2 * (i + 1));
        }

        String[] strsl2 = new String[data.length() / 2];

        strsl2[0] = strsl[3];
        strsl2[1] = strsl[2];
        strsl2[2] = strsl[1];
        strsl2[3] = strsl[0];

        strsl2[4] = strsl[7];
        strsl2[5] = strsl[6];
        strsl2[6] = strsl[5];
        strsl2[7] = strsl[4];

        strsl2[8] = strsl[11];
        strsl2[9] = strsl[10];
        strsl2[10] = strsl[9];
        strsl2[11] = strsl[8];


        strsl2[12] = strsl[15];
        strsl2[13] = strsl[14];
        strsl2[14] = strsl[13];
        strsl2[15] = strsl[12];

        String data2 = "";

        for (int i = 0; i < strsl2.length; i++) {
            data2 += strsl2[i];
        }

        return data2;
    }


    public static class Result {
        /**
         * 成功 successful
         */
        public static final int SUCCESS = 1;
        /**
         * 寻卡失败 Find card failure
         */
        public static final int FIND_FAIL = 2;
        /**
         * 验证失败 Validation fails
         */
        public static final int VALIDATE_FAIL = 3;
        /**
         * 读卡失败 Read card failure
         */
        public static final int READ_FAIL = 4;
        /**
         * 写卡失败 Write card failure
         */
        public static final int WRITE_FAIL = 5;
        /**
         * 超时 timeout
         */
        public static final int TIME_OUT = 6;
        /**
         * 其它异常 other exception
         */
        public static final int OTHER_EXCEPTION = 7;

        /**
         * 确认码 1: 成功 2：寻卡失败 3：验证失败 4:写卡失败 5：超时 6：其它异常
         */
        public int confirmationCode;

        /**
         * 具体的字节流错误信息，系统组知道
         */
        public String error;

        /**
         * 结果集:当确认码为1时，再判断是否有结果 Results: when the code is 1, then determine
         * whether to have the result
         */
        public Object resultInfo;

        /**
         * 卡号 The card number
         */
        public String num;

        @Override
        public String toString() {
            return "Result{" +
                    "confirmationCode=" + confirmationCode +
                    ", resultInfo=" + resultInfo +
                    ", num='" + num + '\'' +
                    '}';
        }
    }

}
