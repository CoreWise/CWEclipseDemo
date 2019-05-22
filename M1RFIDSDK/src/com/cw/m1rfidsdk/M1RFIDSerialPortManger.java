package com.cw.m1rfidsdk;

import android.os.SystemClock;
import android.util.Log;

import com.cw.serialportsdk.cw;
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
 * Created by 金宇凡 on 2019/5/14.
 */
public class M1RFIDSerialPortManger {

    private String TAG = "cw" + M1RFIDSerialPortManger.class.getSimpleName();
    private boolean isOpen;
    private boolean firstOpen = false;

    //添加volatile，防止死锁
    private volatile int mCurrentSize = 0;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private SerialPort mSerialPort = null;

    private byte[] mBuffer = new byte[50 * 1024];

    /**
     * U3
     */
    private String U3_STM32_GPIO_DEV_PATH = "/sys/class/gpio_power/stm32power/enable";//上下电路径
    private String U3_M1RFID_Serialport_Path = "/dev/ttyHSL1";
    private int U3_M1RFID_Baudrate = 115200;
    private byte[] U3_UP = {'1'};//上电指令
    private byte[] U3_DOWN = {'0'};

    /**
     * U8
     */
    private String U8_STM32_GPIO_DEV_PATH = "/sys/class/gpio_power/stm32power/enable";//上下电路径
    private String U8_M1RFID_Serialport_Path = "/dev/ttyHSL1";
    private int U8_M1RFID_Baudrate = 115200;
    private byte[] U8_UP = {'1'};//上电指令
    private byte[] U8_DOWN = {'0'};

    /**
     * CFON640
     */

    //CFON640 HXUHF串口波特率
    private int CFON640_M1RFID_Baudrate = 460800;

    //CFON640 HXUHF串口路径
    private String CFON640_M1RFID_Serialport_Path = "/dev/ttyHSL0";

    //CFON640 HXUHF模块上电路径
    private String CFON640_AS602_GPIO_DEV = "/sys/class/pwv_gpios/as602-en/enable";

    private String CFON640_FBI_GPIO_DEV = "/sys/class/fbicode_gpios/fbicoe_state/control";

    private boolean isCFON640FBIDevice() {
        File file = new File(CFON640_FBI_GPIO_DEV);
        return file.exists();
    }

    private byte[] CFON640_UP = {'1'};//上电指令
    private byte[] CFON640_DOWN = {'0'};


    /**
     * 新A370
     */
    private String New_A370_GPIO_DEV_STM32 = "/sys/class/stm32_gpios/stm32-en/enable";
    private String New_A370_M1RFID_Serialport_Path = "/dev/ttyHSL1";
    private int New_A370_M1RFID_Baudrate = 115200;
    private byte[] New_A370_UP = {'1'};
    private byte[] New_A370_DOWN = {'0'};


    /**
     * 海派A370
     */

    //CFON640 HXUHF串口波特率
    private int A370_M1RFID_Baudrate = 460800;

    //CFON640 HXUHF串口路径
    private String A370_M1RFID_Serialport_Path = "/dev/ttyHSL0";

    //CFON640 HXUHF模块上电路径
    private String A370_AS602_GPIO_DEV = "/sys/class/pwv_gpios/as602-en/enable";

    private String A370_FBI_GPIO_DEV = "/sys/class/fbicode_gpios/fbicoe_state/control";


    private boolean isA370640FBIDevice() {
        File file = new File(CFON640_FBI_GPIO_DEV);
        return file.exists();
    }

    private byte[] A370_UP = {'1'};//上电指令
    private byte[] A370_DOWN = {'0'};

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////我是丑陋的分割线////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////


    private ReadThread mReadThread;

    private static M1RFIDSerialPortManger mM1RFIDSerialPortManger;

    public static M1RFIDSerialPortManger getInstance() {
        if (mM1RFIDSerialPortManger == null) {
            synchronized (M1RFIDSerialPortManger.class) {
                if (mM1RFIDSerialPortManger == null) {
                    mM1RFIDSerialPortManger = new M1RFIDSerialPortManger();
                }
            }
        }
        return mM1RFIDSerialPortManger;
    }


    public boolean isOpen() {
        return isOpen;
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
     * 根据传参来释放相应的设备接口
     * 释放设备要下电
     *
     * @param type
     */
    private void releaseDevice(int type) {
        switch (type) {
            case Device_CPOS800:

                break;
            case Device_CFON600:

                break;
            case Device_CFON640:
                setGpioStatus(CFON640_AS602_GPIO_DEV, CFON640_DOWN);
                if (isCFON640FBIDevice()) {
                    setGpioStatus(CFON640_FBI_GPIO_DEV, CFON640_DOWN);
                }
                break;
            case Device_A370_M4G5:
                setGpioStatus(A370_AS602_GPIO_DEV, A370_DOWN);
                if (isCFON640FBIDevice()) {
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
                //setGpioStatus(New_A370_GPIO_DEV_STM32,New_A370_DOWN);

                break;
        }
    }


    /**
     * 根据传参来选择相应的设备接口
     * //1.先上电，2.再打开串口
     *
     * @param type
     */
    private void selectDevice(int type) {

        switch (type) {

            case Device_CFON640:
                setGpioStatus(CFON640_AS602_GPIO_DEV, CFON640_UP);
                if (isCFON640FBIDevice()) {
                    setGpioStatus(CFON640_FBI_GPIO_DEV, CFON640_UP);
                }
                try {
                    mSerialPort = new SerialPort(new File(CFON640_M1RFID_Serialport_Path), CFON640_M1RFID_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---CFON640----------" + e.toString());

                }
                break;

            case Device_A370_M4G5:
                setGpioStatus(A370_AS602_GPIO_DEV, CFON640_UP);
                if (isCFON640FBIDevice()) {
                    setGpioStatus(A370_FBI_GPIO_DEV, CFON640_UP);
                }
                try {
                    mSerialPort = new SerialPort(new File(A370_M1RFID_Serialport_Path), A370_M1RFID_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---海派A370----------" + e.toString());

                }

                break;

            case Device_U3:
                setGpioStatus(U3_STM32_GPIO_DEV_PATH, U3_UP);
                try {
                    mSerialPort = new SerialPort(new File(U3_M1RFID_Serialport_Path), U3_M1RFID_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---U3----------" + e.toString());

                }
                break;
            case Device_A370_CW20:
                //新A370,除了上电路径和U8不一样，其他都一样
                setGpioStatus(New_A370_GPIO_DEV_STM32, New_A370_UP);
                try {
                    mSerialPort = new SerialPort(new File(New_A370_M1RFID_Serialport_Path), New_A370_M1RFID_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---new A370----------" + e.toString());
                }
                break;

            case Device_U8:
                setGpioStatus(U8_STM32_GPIO_DEV_PATH, U8_UP);
                try {
                    mSerialPort = new SerialPort(new File(U8_M1RFID_Serialport_Path), U8_M1RFID_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---U3----------" + e.toString());

                }
                break;
        }
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
            Log.e(TAG, status + "------------------setGpioStatus---------此处报错属于系统级报错，不影响应用，因为每个产品机器的上电路径不一样，这里只是一次性把几个上电路径给上了-------------" + e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, status + "------------------setGpioStatus----------此处报错属于系统级报错，不影响应用，因为每个产品机器的上电路径不一样，这里只是一次性把几个上电路径给上了------------" + e.toString());
        }
    }


    public synchronized void write(byte[] data) {
        Log.i(TAG, "send commnad=" + DataUtils.toHexString(data));
        writeCommand(data);
    }

    private void writeCommand(byte[] data) {
        if (!isOpen) {
            return;
        }
        if (firstOpen) {
            SystemClock.sleep(2000);
            firstOpen = false;
        }
        mCurrentSize = 0;
        try {
            mOutputStream.write(data);
        } catch (IOException e) {
            Log.e(TAG,"-------writeCommand-------"+e.toString());
        }
    }

    /**
     * 读串口操作(Read SerialPort)
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
            // closeSerialPort2();
            SystemClock.sleep(100);
            // openSerialPort2();
        }
        //LocalLog.setLogPath("/sdcard/SFZ/");
        //LocalLog.setFileName("SFZ");
        //LocalLog.setDefalutTag("SFZ");
        //LocalLog.i("测试数据不全---" + mCurrentSize + "--内容--" + DataUtils.bytesToHexString(buffer));
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

                        System.arraycopy(buffer, 0, mBuffer, mCurrentSize, length);
                        mCurrentSize += length;
                        Log.i(TAG, "--------ReadThread----------mCurrentSize=" + mCurrentSize + "  length=" + length+"------recv buf = " + DataUtils.toHexString(buffer));

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "----------------ReadThread----------------" + e.toString());
                    return;
                }
            }
        }
    }


    /**
     * 老海派的
     */

    private static final byte[] SWITCH_COMMAND = "D&C00040104".getBytes();
        public  static boolean switchRFID = false;
        /**
     * 切换成读取RFID(swift Read RFID)
     *
     * @return
     */
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
}
