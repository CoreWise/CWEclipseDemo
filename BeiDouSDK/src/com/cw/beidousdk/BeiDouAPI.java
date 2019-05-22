package com.cw.beidousdk;


import android.os.Handler;
import android.util.Log;


import com.cw.beidousdk.intf.DWXXListener;
import com.cw.beidousdk.intf.FKXXListener;
import com.cw.beidousdk.intf.ICJCListener;
import com.cw.beidousdk.intf.TXXXListener;
import com.cw.beidousdk.intf.XTZJListener;
import com.cw.serialportsdk.utils.DataUtils;

import android_serialport_api.LooperBuffer;

/**
 * @author Administrator
 */
public class BeiDouAPI implements LooperBuffer {
    private final String TAG = "CW"+"BeiDouAPI";

    private final byte[] CMD_OPEN = {(byte) 0xCA, (byte) 0xDF, 0x05, 0x36, 00, (byte) 0xE3};
    private final byte[] CMD_CLOSE = {(byte) 0xCA, (byte) 0xDF, 0x05, 0x36, 01, (byte) 0xE3};

    private XTZJListener mXTZJListener;
    private ICJCListener mICJCListener;
    private DWXXListener mDWXXListener;
    private TXXXListener mTXXXListener;
    private FKXXListener mFKXXListener;

    private Handler mHandler;

    private BeiDouAPI() {
    }


    private static BeiDouAPI Instance = null;

    public static BeiDouAPI getInstance() {
        if (Instance == null) {
            Instance = new BeiDouAPI();
        }
        return Instance;
    }


    /**
     * 打开北斗模块
     */
    public void open(FKXXListener listener) {
        Log.d(TAG, "into open");
        //先上3个电
        BeiDouSerialPortManager.getInstance().upGPIOBeiDou(true);
        //开串口1发指令
        BeiDouSerialPortManager.getInstance().openSerialPort(true);
        BeiDouSerialPortManager.getInstance().write(CMD_OPEN);
        BeiDouSerialPortManager.getInstance().closeSerialPort();
        //关串口1上第4个电
        BeiDouSerialPortManager.getInstance().upGPIOBeiDou2(true);
        BeiDouSerialPortManager.getInstance().openSerialPort(false);
        BeiDouSerialPortManager.getInstance().setLoopBuffer(this);

        this.mFKXXListener = listener;
        mHandler = new Handler();
    }

    /**
     * 关闭北斗模块
     */
    public void close() {
        Log.d(TAG, "into close");
        BeiDouSerialPortManager.getInstance().closeLoopBuffer();
        BeiDouSerialPortManager.getInstance().closeSerialPort();
        BeiDouSerialPortManager.getInstance().upGPIOBeiDou2(false);
        BeiDouSerialPortManager.getInstance().openSerialPort(true);
        BeiDouSerialPortManager.getInstance().write(CMD_CLOSE);
        BeiDouSerialPortManager.getInstance().upGPIOBeiDou(false);
        // 这边可以添加判断是否成功
        BeiDouSerialPortManager.getInstance().closeSerialPort();
    }

    public void setTXXXListener(TXXXListener listener) {
        this.mTXXXListener = listener;
    }

    /**
     * 功率检测，检测当前模块接受各种波束的功率状况（PS:该方法已废弃，相应功能在XTZJ方法中实现）
     *
     * @param interval 输出频度，单位1分钟，0代表单次输出
     * @see BeiDouAPI#XTZJ
     */
    @Deprecated
    public void GLJC(int interval) {
        byte[] msg = {(byte) interval};
        BeiDouSerialPortManager.getInstance().write(toPackageCMD(Constants.$GLKC, msg));
    }

    /**
     * 定位申请
     *
     * @param isEmergency 是否紧急定位 true:是;false:否
     * @param interval    入站频度，单位1秒，0表示单次定位
     */
    public void DWSQ(boolean isEmergency, int interval, DWXXListener listener) {
        this.mDWXXListener = listener;

        String BinaryStr = "00" + (isEmergency ? "1" : "0") + "00100";
        // 信息类别
        byte[] msgCategory = DataUtils.converBinary2Bytes(BinaryStr);
        // 信息内容
        byte[] msg = new byte[11];
        msg[0] = msgCategory[0];
        msg[9] = (byte) (interval >> 8);
        msg[10] = (byte) (interval & 0xff);
        BeiDouSerialPortManager.getInstance().write(toPackageCMD(Constants.$DWSQ, msg));
    }

    /**
     * 通信申请
     *
     * @param type     0:汉字;1:代码;2:混发
     * @param address  对方用户地址
     * @param data     电文长度，最大210个字节
     * @param listener 通信信息回调
     */
    public void TXSQ(int type, byte[] address, byte[] data) {
        DataUtils.toHexString(address);
        byte msgCategory = type == 0 ? (byte) 0x44 : (byte) 0x46;
        byte[] msg = null;
        msg = new byte[7 + data.length];
        msg[0] = msgCategory;
        System.arraycopy(address, 0, msg, 1, 3);
        msg[4] = (byte) (data.length * 8 >> 8);
        msg[5] = (byte) (data.length * 8 & 0xff);
        msg[6] = 0;
        System.arraycopy(data, 0, msg, 7, data.length);
        DataUtils.toHexString(toPackageCMD(Constants.$TXSQ, msg));
        BeiDouSerialPortManager.getInstance().write(toPackageCMD(Constants.$TXSQ, msg));
    }

    public void HXSZ() {

    }

    /**
     * ic检测
     *
     * @param frameNo 帧号
     */
    public void ICJC(int frameNo, ICJCListener listener) {
        Log.d(TAG, "into ICJC");
        this.mICJCListener = listener;

        byte[] msg = new byte[1];
        msg[0] = (byte) frameNo;
        BeiDouSerialPortManager.getInstance().write(toPackageCMD(Constants.$ICJC, msg));
    }

    /**
     * 紧急自毁
     */
    public void JJZH() {
        byte[] msg = {0x55, (byte) 0xaa, (byte) 0xaa, 0x55};
        BeiDouSerialPortManager.getInstance().write(toPackageCMD(Constants.$JJZH, msg));
    }

    /**
     * 波束设置
     *
     * @param channel 通道号：1-6
     * @param range   波束号：0-6，0为自动选择
     */
    public void BSSZ(int channel, int range) {

    }

    /**
     * 系统自检
     *
     * @param interval 自检频度：单位1秒，0标识单次检测
     */
    public void XTZJ(int interval, XTZJListener listener) {
        Log.d(TAG, "into XTZJ ---->interval:" + interval);
        this.mXTZJListener = listener;

        byte[] msg = new byte[2];
        msg[0] = (byte) (interval >> 8);
        msg[1] = (byte) (interval & 0xff);
        BeiDouSerialPortManager.getInstance().write(toPackageCMD(Constants.$XTZJ, msg));
    }

    /**
     * 封装指令
     *
     * @param cmd 指令头
     * @param msg 消息体
     * @return 封装后的指令
     */
    private byte[] toPackageCMD(String cmd, byte[] msg) {
        byte[] data = new byte[5 + 2 + 3 + msg.length + 1];
        byte[] head = DataUtils.convertASCIIString2Bytes(cmd);
        int len = data.length;
        System.arraycopy(head, 0, data, 0, 5);
        data[5] = (byte) (len >> 8);
        data[6] = (byte) (len & 0xff);
        System.arraycopy(msg, 0, data, 10, msg.length);
        data[len - 1] = getChecksum(data);
        Log.i(TAG, "toPackageCMD:" + DataUtils.toHexString(data));
        return data;
    }

    /**
     * 获取异或结果
     *
     * @param data 需要计算的数据。PS:这边data的最后一个字节不做计算
     * @return
     */
    private byte getChecksum(byte[] data) {
        byte temp = data[0];
        for (int i = 1; i < data.length - 1; i++) {
            temp ^= data[i];
        }
        return temp;
    }

    /**
     * 测高方式 Altitude:有高程; NONE:无测高; Altimetry1:测高1; Altimetry2:测高2
     */
    @Deprecated
    public enum Altimetry {

        Altitude("00"), NONE("01"), Altimetry1("10"), Altimetry2("11");

        private String mAltimetryStyle;

        private Altimetry(String altimetryStyle) {
            this.mAltimetryStyle = altimetryStyle;
        }

        public String getmAltimetryStyle() {
            return mAltimetryStyle;
        }
    }

    @Override
    public void add(final byte[] buffer) {
        Log.i(TAG, "add:" + DataUtils.toHexString(buffer));
        // 这边未做粘包处理，建议按“$”分割，然后做长度和校验位校验
        if (buffer.length > 10) {
            // 消息头
            byte[] cmd = new byte[5];
            System.arraycopy(buffer, 0, cmd, 0, 5);
            String head = DataUtils.convertBytes2ASCIIString(cmd);
            // 截取完整消息体
            int len = DataUtils.getShort(buffer[5], buffer[6]);
            byte[] data = null;
            if (len == buffer.length) {
                data = buffer;
            } else {
                data = new byte[len];
                System.arraycopy(buffer, 0, data, 0, len);
            }
            switch (head) {
                case Constants.$ZJXX:
                    decodeZJXX(data);
                    break;
                case Constants.$ICXX:
                    decodeICXX(data);
                    break;
                case Constants.$DWXX:
                    decodeDWXX(data);
                    break;
                case Constants.$TXXX:
                    decodeTXXX(data);
                    break;
                case Constants.$FKXX:
                    decodeFKXX(data);
                    break;
                default:
                    Log.i(TAG, "无效标志：" + head);
                    break;
            }
            // 测试
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mFKXXListener.testData(buffer);
                }
            });
        }
    }

    @Override
    public byte[] getFullPacket() {
        return new byte[0];
    }

    /**
     * 解析自检信息
     *
     * @param data 原数据
     */
    private void decodeZJXX(byte[] data) {
        String ICState = Integer.toBinaryString((data[10] & 0xFF) + 0x100).substring(1);
        String hardwareState = Integer.toBinaryString((data[11] & 0xFF) + 0x100).substring(1);
        byte battery = data[12];
        String inboundState = Integer.toBinaryString((data[13] & 0xFF) + 0x100).substring(1);

        // 分析ic卡状态
        final boolean isICHandlerNormal = ICState.substring(0, 1).equals("0") ? true : false;
        final boolean isIDNormal = ICState.substring(1, 2).equals("0") ? true : false;
        final boolean isCheckCodeCorrect = ICState.substring(2, 3).equals("0") ? true : false;
        final boolean isSerialNoNormal = ICState.substring(3, 4).equals("0") ? true : false;
        final boolean ManagementCardOrUserCard = ICState.substring(4, 5).equals("1") ? true : false;
        final boolean isDataNormal = ICState.substring(5, 6).equals("0") ? true : false;
        final boolean isICNormal = ICState.substring(6, 7).equals("0") ? true : false;

        // 分析硬件状态
        final boolean isAntennaNormal = hardwareState.substring(0, 1).equals("0") ? true : false;
        final boolean isChannelNormal = hardwareState.substring(1, 2).equals("0") ? true : false;
        final boolean isMainBoardNormal = hardwareState.substring(2, 3).equals("0") ? true : false;

        // 电池电量
        final int percent = battery == 0 ? 0 : 100 / battery;

        // 分析入站状态
        final boolean isSuppression = inboundState.substring(0, 1).equals("1") ? true : false;
        final boolean isSilence = inboundState.substring(1, 2).equals("1") ? true : false;

        if (mXTZJListener != null) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mXTZJListener.ICStates(isICHandlerNormal, isIDNormal, isCheckCodeCorrect, isSerialNoNormal,
                            ManagementCardOrUserCard, isDataNormal, isICNormal);
                    mXTZJListener.HardwareStates(isAntennaNormal, isChannelNormal, isMainBoardNormal);
                    mXTZJListener.BatteryLevel(percent);
                    mXTZJListener.InboundStates(isSuppression, isSilence);
                }
            });
        }
    }

    /**
     * 解析IC信息
     *
     * @param data 源数据
     */
    private void decodeICXX(byte[] data) {
        final int broadcastID = DataUtils.getShort(data[11], data[12], data[13]);
        String character = Long.toString(data[14] & 0xff, 2);
        while (character.length() < 3) {
            character = "0" + character;
        }
        final String userCharacteristic = character.substring(0, 3);
        final int serviceFrequency = DataUtils.getShort(data[15], data[16]);
        final boolean isEncryptionUser = data[18] == 0 ? true : false;

        if (mICJCListener != null) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mICJCListener.ICInfo(broadcastID, userCharacteristic, serviceFrequency, isEncryptionUser);
                }
            });
        }
    }

    /**
     * 解析定位信息
     *
     * @param data 源数据
     */
    private void decodeDWXX(byte[] data) {
        int hh = data[14];
        int mm = data[15];
        int ss = data[16];
        int ms = data[17];

        int lngDegree = data[18];
        int lngMinute = data[19];
        int lngSecond = data[20];
        int lngMillisecond = data[21];

        int latDegree = data[22];
        int latMinute = data[23];
        int latSecond = data[24];
        int latMillisecond = data[25];

        final String time = hh + ":" + mm + ":" + ss + ":" + ms;
        final String lng = lngDegree + "." + lngMinute + "" + lngSecond + "" + lngMillisecond;
        final String lat = latDegree + "." + latMinute + "" + latSecond + "" + latMillisecond;

        if (mDWXXListener != null) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mDWXXListener.LocationInfo(time, Float.parseFloat(lng), Float.parseFloat(lat));
                }
            });
        }
    }

    /**
     * 解析通信信息
     *
     * @param data 源数据
     */
    private void decodeTXXX(final byte[] data) {
        // 未作crc判断
        byte msgCategory = data[10];
        final boolean isChinese = Integer.toBinaryString((msgCategory & 0xFF) + 0x100).substring(1).substring(2, 3)
                .equals("0") ? true : false;
        final byte[] userAddress = new byte[3];
        System.arraycopy(data, 11, userAddress, 0, 3);
        int len = DataUtils.getShort(data[16], data[17]) / 8;

        final byte[] msg = new byte[len];
        try {
            System.arraycopy(data, 18, msg, 0, len);
        } catch (Exception e) {
            e.printStackTrace();
            if (mTXXXListener != null) {
                mHandler.post(new Runnable() {

                    @Override
                    public void run() {
                        mTXXXListener.TXFail(data);
                    }
                });
            }
            return;
        }

        if (mTXXXListener != null) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mTXXXListener.TXXX(userAddress, isChinese, msg);
                }
            });
        }
    }

    /**
     * 解析反馈信息
     *
     * @param data 源数据
     */
    private void decodeFKXX(byte[] data) {
        final byte mark = data[10];
        byte[] msg = new byte[4];
        System.arraycopy(data, 11, msg, 0, 4);
        final String cmd = DataUtils.convertBytes2ASCIIString(msg);

        if (mFKXXListener != null) {
            mHandler.post(new Runnable() {

                @Override
                public void run() {
                    mFKXXListener.FKXX(mark, cmd);
                }
            });
        }
    }
}