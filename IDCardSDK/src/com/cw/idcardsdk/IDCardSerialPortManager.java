package com.cw.idcardsdk;

import android.os.SystemClock;
import android.util.Log;

import com.cw.serialportsdk.utils.DataUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android_serialport_api.SerialPort;

import static com.cw.serialportsdk.cw.*;


/**
 * 作者：liyang
 * 时间：2019/5/9
 * 描述：
 */
public class IDCardSerialPortManager {

    private static final String TAG = "CWIDCard" + "SerialPortM";

    @Deprecated
    public  static boolean switchRFID = false;


    /**
     * CPOS800
     */
    //CPOS800身份证串口波特率
    private int CPOS800_IdCard_Baudrate = 230400;

    //CPOS800身份证串口路径
    private String CPOS800_IdCard_Serialport_Path = "/dev/ttyHSL1";

    //CPOS800身份证模块上电路径
    private String CPOS800_IdCard_PowerOn = "/sys/class/idcard_gpio/idcard_en/enable";

    //CPOS800身份证模块上电值
    private byte[] CPOS800_UP = {'1'};

    //CPOS800身份证模块下电值
    private byte[] CPOS800_DOWN = {'0'};

    //CPOS800身份证读卡方法
    public byte[] CPOS800_ReadIDCard = {0x07, 0x00, 0x0d, (byte) 0xca, (byte) 0xdf, 0x03, 0x00, 0x07, (byte) 0xAA, (byte) 0xAA, (byte) 0xAA, (byte) 0x96, 0x69, 0x00, 0x00, (byte) 0xe3, 0x30};

    /**
     * U3
     */
    private String U3_STM32_GPIO_DEV_PATH = "/sys/class/gpio_power/stm32power/enable";
    private String U3_IdCard_Serialport_Path = "/dev/ttyHSL1";
    private int U3_IdCard_Baudrate = 115200;
    private byte[] U3_UP = {'1'};
    private byte[] U3_DOWN = {'0'};

    /**
     * U8
     */
    private String U8_STM32_GPIO_DEV_PATH = "/sys/class/gpio_power/stm32power/enable";
    private String U8_IdCard_Serialport_Path = "/dev/ttyHSL1";
    private int U8_IdCard_Baudrate = 115200;
    private byte[] U8_UP = {'1'};
    private byte[] U8_DOWN = {'0'};


    /**
     * CFON640
     */
    private String CFON640_AS602_GPIO_DEV_PATH = "/sys/class/pwv_gpios/as602-en/enable";
    private String CFON640_IdCard_Serialport_Path = "/dev/ttyHSL0";
    private int CFON640_IdCard_Baudrate = 460800;
    private byte[] CFON640_UP = {'1'};
    private byte[] CFON640_DOWN = {'0'};
    private String CFON640_FBI_GPIO_DEV = "/sys/class/fbicode_gpios/fbicoe_state/control";

    private boolean isCFON640FBIDevice() {
        File file = new File(CFON640_FBI_GPIO_DEV);
        return file.exists();
    }


    /**
     * A370
     */

    private String A370_AS602_GPIO_DEV_PATH = "/sys/class/pwv_gpios/as602-en/enable";
    private String A370_IdCard_Serialport_Path = "/dev/ttyHSL0";
    private int A370_IdCard_Baudrate = 460800;
    private byte[] A370_UP = {'1'};
    private byte[] A370_DOWN = {'0'};
    private String A370_FBI_GPIO_DEV = "/sys/class/fbicode_gpios/fbicoe_state/control";

    private boolean isA370FBIDevice() {
        File file = new File(A370_FBI_GPIO_DEV);
        return file.exists();
    }


    /**
     * 新A370
     */
    private String New_A370_GPIO_DEV_STM32 = "/sys/class/stm32_gpios/stm32-en/enable";
    private String New_A370_IdCard_Serialport_Path = "/dev/ttyHSL1";
    private int New_A370_IdCard_Baudrate = 115200;
    private byte[] New_A370_UP = {'1'};
    private byte[] New_A370_DOWN = {'0'};


    /**
     * 获取该类的实例对象，为单例
     *
     * @return
     */
    private volatile static IDCardSerialPortManager mSerialPortManager;

    public static IDCardSerialPortManager getInstance() {
        if (mSerialPortManager == null) {
            synchronized (IDCardSerialPortManager.class) {
                if (mSerialPortManager == null) {
                    mSerialPortManager = new IDCardSerialPortManager();
                }
            }
        }
        return mSerialPortManager;
    }


    private SerialPort mSerialPort = null;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private ReadThread mReadThread;
    private boolean isOpen;


    /**
     * 根据传参来选择相应的设备接口
     * //1.先上电，2.再打开串口
     *
     * @param type
     */
    private void selectDevice(int type) {

        switch (type) {

            case Device_CPOS800:
                //1.先上电，2.再打开串口
                setGpioStatus(CPOS800_IdCard_PowerOn, CPOS800_UP);
                try {
                    mSerialPort = new SerialPort(new File(CPOS800_IdCard_Serialport_Path), CPOS800_IdCard_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---CPOS800----------" + e.toString());
                }

                break;
            case Device_CFON600:
                //暂时没有身份证功能

                break;
            case Device_CFON640:
                setGpioStatus(CFON640_AS602_GPIO_DEV_PATH, CFON640_UP);
                if (isCFON640FBIDevice()) {
                    setGpioStatus(CFON640_FBI_GPIO_DEV, CFON640_UP);
                }
                try {
                    mSerialPort = new SerialPort(new File(CFON640_IdCard_Serialport_Path), CFON640_IdCard_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---CFON640----------" + e.toString());
                }
                break;
            case Device_A370_M4G5:
                setGpioStatus(A370_AS602_GPIO_DEV_PATH, CFON640_UP);
                if (isCFON640FBIDevice()) {
                    setGpioStatus(A370_FBI_GPIO_DEV, CFON640_UP);
                }
                try {
                    mSerialPort = new SerialPort(new File(A370_IdCard_Serialport_Path), A370_IdCard_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---海派A370----------" + e.toString());
                }
                break;
            case Device_U1:

                break;
            case Device_U3:
                setGpioStatus(U3_STM32_GPIO_DEV_PATH, U3_UP);
                try {
                    mSerialPort = new SerialPort(new File(U3_IdCard_Serialport_Path), U3_IdCard_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---U3----------" + e.toString());
                }
                break;
            case Device_U8:
                setGpioStatus(U3_STM32_GPIO_DEV_PATH, U8_UP);
                try {
                    mSerialPort = new SerialPort(new File(U8_IdCard_Serialport_Path), U8_IdCard_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---U8----------" + e.toString());

                }
                break;
            case Device_A370_CW20:
                //新A370,除了上电路径和U8不一样，其他都一样
                setGpioStatus(New_A370_GPIO_DEV_STM32, New_A370_UP);
                try {
                    mSerialPort = new SerialPort(new File(New_A370_IdCard_Serialport_Path), New_A370_IdCard_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---new A370----------" + e.toString());
                }
                break;

        }
    }

    /**
     * 根据传参来释放相应的设备接口
     * 释放设备要下电
     *
     * @param type
     */
    private void releaseDevice(int type) {
        switch (type) {
            case Device_CPOS800:
                //先下电
                setGpioStatus(CPOS800_IdCard_PowerOn, CPOS800_DOWN);

                break;
            case Device_CFON600:

                break;
            case Device_CFON640:
                setGpioStatus(CFON640_AS602_GPIO_DEV_PATH, CFON640_DOWN);
                if (isCFON640FBIDevice()) {
                    setGpioStatus(CFON640_FBI_GPIO_DEV, CFON640_DOWN);
                }
                break;
            case Device_A370_M4G5:
                setGpioStatus(A370_AS602_GPIO_DEV_PATH, A370_DOWN);
                if (isA370FBIDevice()) {
                    setGpioStatus(A370_FBI_GPIO_DEV, A370_DOWN);
                }
                break;
            case Device_U1:

                break;
            case Device_U3:
                setGpioStatus(U3_STM32_GPIO_DEV_PATH, U3_DOWN);
                break;
            case Device_U8:
                setGpioStatus(U8_STM32_GPIO_DEV_PATH, U8_DOWN);

                break;
            case Device_A370_CW20:
                setGpioStatus(New_A370_GPIO_DEV_STM32, New_A370_DOWN);

                break;
        }
    }


    /**
     * 打开串口
     *
     * @param type 设备型号,见device
     * @return
     */
    public boolean openSerialPort(int type) {
        if (mSerialPort == null) {
            selectDevice(type);
            if (mSerialPort == null) {
                return false;
            }
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            isOpen = true;
            mReadThread = new ReadThread();
            mReadThread.start();
            return true;
        }
        return false;
    }


    /**
     * 关闭串口
     *
     * @param type 设备型号,见device
     */
    public void closeSerialPort(int type) {

        if (mReadThread != null) {
            mReadThread.interrupt();
            mReadThread = null;
        }

        if (mSerialPort != null) {
            try {
                mOutputStream.close();
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "-----------------closeSerialPort------------------" + e.toString());
            }
            mSerialPort.close();
            mSerialPort = null;
        }
        releaseDevice(type);
        isOpen = false;
                switchRFID = false;

    }

    /**
     * 判断串口是否打开
     *
     * @return true：打开 false：未打开
     */
    public boolean isOpen() {
        return isOpen;
    }


    /**
     * 设置GPIO口状态
     *
     * @param path   GPIO路径
     * @param status 状态值
     */
    private void setGpioStatus(String path, byte[] status) {
        FileOutputStream fw = null;
        try {
            fw = new FileOutputStream(path);
            fw.write(status);
            fw.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, status + "------------------setGpioStatus----------------------" + e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, status + "------------------setGpioStatus----------------------" + e.toString());
        }
    }


    /**
     * 用于判断某一系统路径是否存在
     *
     * @param strFile
     * @return
     */
    private boolean fileIsExists(String strFile) {
        try {
            File f = new File(strFile);
            if (!f.exists()) {
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "----------fileIsExists-----------" + e.toString());
            return false;
        }
        return true;
    }

    /**
     * 切换成读取RFID(swift Read RFID)
     * 海派平台用到的，和U3，U8没关系，我也不懂这啥意思
     * @return
     */
    private  final byte[] SWITCH_COMMAND = "D&C00040104".getBytes();

    @Deprecated
    public void switchStatus() {
        if (!isOpen) {
            return;
        }
        write(SWITCH_COMMAND);
        Log.i(TAG, "SWITCH_COMMAND hex=" + new String(SWITCH_COMMAND));
        SystemClock.sleep(200);
        if (!isOpen) {
            return;
        }
        switchRFID = true;
        Log.i(TAG, "SWITCH_COMMAND end");
    }


    ////////////////////////////////////////////////////////////////////////////////////////

    public synchronized void write(byte[] data) {
        Log.i(TAG, "send commnad=" + DataUtils.toHexString(data));
        writeCommand(data);
    }

    private void writeCommand(byte[] data) {
        if (!isOpen) {
            Log.e(TAG, "-----------writeCommand 串口未打开！-------------");

            return;
        }

        mCurrentSize = 0;
        try {
            mOutputStream.write(data);
        } catch (IOException e) {
            e.toString();
            Log.e(TAG, "-----------writeCommand-------------" + e.toString());
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////


    public void clearBuffer() {
        mBuffer = null;
        mBuffer = new byte[50 * 1024];
        mCurrentSize = 0;
    }


    private interface LooperBuffer {
        void add(byte[] buffer);

        byte[] getFullPacket();
    }


    private LooperBuffer looperBuffer;

    private int mCurrentSize = 0;

    private byte[] mBuffer = new byte[50 * 1024];

    /**
     * 读串口数据
     *
     * @param buffer
     * @param waittime
     * @param interval
     * @return
     */
    public synchronized int read(byte buffer[], int waittime, int interval) {
        if (!isOpen) {
            return 0;
        }
        int sleepTime = 5;
        int length = waittime / sleepTime;
        boolean shutDown = false;
        for (int i = 0; i < length; i++) {
            if (mCurrentSize == 0) {
                SystemClock.sleep(sleepTime);
                continue;
            } else {
                break;
            }
        }

        if (mCurrentSize > 0) {
            long lastTime = System.currentTimeMillis();
            long currentTime = 0;
            int lastRecSize = 0;
            int currentRecSize = 0;
            while (!shutDown && isOpen) {
                currentTime = System.currentTimeMillis();
                currentRecSize = mCurrentSize;
                if (currentRecSize > lastRecSize) {
                    lastTime = currentTime;
                    lastRecSize = currentRecSize;
                } else if (currentRecSize == lastRecSize && currentTime - lastTime >= interval) {
                    shutDown = true;
                }
            }
            if (mCurrentSize <= buffer.length) {
                System.arraycopy(mBuffer, 0, buffer, 0, mCurrentSize);
            }
        } else {
            SystemClock.sleep(100);
        }
        return mCurrentSize;
    }


    /**
     * 读取线程
     */
    private class ReadThread extends Thread {
        @Override
        public void run() {
            byte[] buffer = new byte[2321];
            while (!isInterrupted()) {
                int length = 0;
                try {
                    if (mInputStream == null) {
                        return;
                    }
                    length = mInputStream.read(buffer);
                    if (length > 0) {
                        if (looperBuffer != null) {
                            byte[] buf = new byte[length];
                            System.arraycopy(buffer, 0, buf, 0, length);
                            Log.i(TAG, "---------ReadThread-------------recv buf=" + DataUtils.toHexString(buf));
                            looperBuffer.add(buf);
                        }
                        System.arraycopy(buffer, 0, mBuffer, mCurrentSize, length);
                        mCurrentSize += length;
                        Log.i(TAG, "--------ReadThread----------mCurrentSize=" + mCurrentSize + "  length=" + length);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "----------------ReadThread----------------" + e.toString());
                    return;
                }
            }
        }
    }


}
