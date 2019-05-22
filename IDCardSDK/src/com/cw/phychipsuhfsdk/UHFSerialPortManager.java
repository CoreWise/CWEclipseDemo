package com.cw.phychipsuhfsdk;

import android.os.SystemClock;
import android.util.Log;

import com.cw.serialportsdk.utils.DataUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android_serialport_api.LooperBuffer;
import android_serialport_api.SerialPort;

import static com.cw.serialportsdk.cw.*;



/**
 * 作者：liyang
 * 时间：2019/5/9
 * 描述：HXUHF 和RED5 UHF，两者部分兼容，不兼容部分:功率部分不兼容!
 * <p>
 * 慧讯: U3,CFON640
 * Red5: CPOS800,A370
 * <p>
 * 应该是能设置功率的是韩国Pr9200AE模块。不能设置功率的是慧迅的。
 */
public class UHFSerialPortManager {

    private static final String TAG = "CWHXUHF" + "SerialPortM";


    /**
     * CPOS800
     */
    //CPOS800 RED5UHF串口波特率
    private int CPOS800_RED5UHF_Baudrate = 230400;

    //CPOS800 RED5UHF串口路径
    private String CPOS800_RED5UHF_Serialport_Path = "/dev/ttyHSL0";

    //CPOS800 RED5UHF模块上电路径
    private String CPOS800_RED5UHF_PowerOn = "/sys/class/cw_gpios/printer_en/enable";

    //CPOS800 RED5UHF模块上电路径
    private String CPOS800_STM32_PowerOn = "/sys/class/stm32_gpio/stm32_en/enable";

    private boolean isCPOS800Stm32 = fileIsExists(CPOS800_STM32_PowerOn);

    //CPOS800 RED5UHF模块上电值
    private byte[] CPOS800_UP = {'1'};

    //CPOS800 RED5UHF模块下电值
    private byte[] CPOS800_DOWN = {'0'};


    /**
     * U3
     */
    //U3 HXUHF串口波特率
    private int U3_HXUHF_Baudrate = 115200;

    //U3 HXUHF串口路径
    private String U3_HXUHF_Serialport_Path = "/dev/ttyHSL1";

    //U3 HXUHF模块上电路径
    private String U3_HXUHF_PowerOn = "/sys/class/gpio_power/stm32power/enable";

    private static String U3_GPIO_DEV_STM32 = "/sys/class/gpio_power/stm32power/enable";


    //U3 HXUHF模块上电值
    private byte[] U3_UP = {'1'};

    //U3 HXUHF模块下电值
    private byte[] U3_DOWN = {'0'};


    /**
     * CFON640
     */

    //CFON640 HXUHF串口波特率
    private int CFON640_HXUHF_Baudrate = 460800;

    //CFON640 HXUHF串口路径
    private String CFON640_HXUHF_Serialport_Path = "/dev/ttyHSL0";

    //CFON640 HXUHF模块上电路径
    private  String CFON640_AS602_GPIO_DEV = "/sys/class/pwv_gpios/as602-en/enable";

    private  String CFON640_FBI_GPIO_DEV = "/sys/class/fbicode_gpios/fbicoe_state/control";


    private boolean isCFON640FBIDevice() {
        File file = new File(CFON640_FBI_GPIO_DEV);
        return file.exists();
    }


    //CFON640 HXUHF模块上电值
    private byte[] CFON640_UP = {'1'};

    //CFON640 HXUHF模块下电值
    private byte[] CFON640_DOWN = {'0'};

    /**
     * A370
     */

    //A370 HXUHF串口波特率
    private int A370_HXUHF_Baudrate = 460800;

    //A370 HXUHF串口路径
    private String A370_HXUHF_Serialport_Path = "/dev/ttyHSL0";

    //A370 HXUHF模块上电路径
    private  String A370_AS602_GPIO_DEV = "/sys/class/pwv_gpios/as602-en/enable";

    private  String A370_FBI_GPIO_DEV = "/sys/class/fbicode_gpios/fbicoe_state/control";

    private boolean isA370FBIDevice() {
        File file = new File(A370_FBI_GPIO_DEV);
        return file.exists();
    }

    //A370 HXUHF模块上电值
    private byte[] A370_UP = {'1'};

    //A370 HXUHF模块下电值
    private byte[] A370_DOWN = {'0'};


    /**
     * 新A370
     */


    /**
     * 获取该类的实例对象，为单例
     *
     * @return
     */
    private volatile static UHFSerialPortManager mSerialPortManager;

    public static UHFSerialPortManager getInstance() {
        if (mSerialPortManager == null) {
            synchronized (UHFSerialPortManager.class) {
                if (mSerialPortManager == null) {
                    mSerialPortManager = new UHFSerialPortManager();
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
                setGpioStatus(CPOS800_RED5UHF_PowerOn, CPOS800_UP);
                if (isCPOS800Stm32) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    setGpioStatus(CPOS800_STM32_PowerOn, CPOS800_UP);
                }
                try {
                    mSerialPort = new SerialPort(new File(CPOS800_RED5UHF_Serialport_Path), CPOS800_RED5UHF_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---CPOS800----------" + e.toString());
                }

                break;
            case Device_CFON600:
                //CFON600没有超高频功能
                break;
            case Device_CFON640:
                setGpioStatus(CFON640_AS602_GPIO_DEV, CFON640_UP);
                if (isCFON640FBIDevice()) {
                    setGpioStatus(CFON640_FBI_GPIO_DEV, CFON640_DOWN);

                }
                try {
                    mSerialPort = new SerialPort(new File(CFON640_HXUHF_Serialport_Path), CFON640_HXUHF_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---CFON640----------" + e.toString());
                }
                break;
            case Device_A370_M4G5:
                setGpioStatus(A370_AS602_GPIO_DEV, A370_UP);
                if (isA370FBIDevice()) {
                    setGpioStatus(A370_FBI_GPIO_DEV, A370_UP);

                }
                try {
                    mSerialPort = new SerialPort(new File(A370_HXUHF_Serialport_Path), A370_HXUHF_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---CFON640----------" + e.toString());
                }
                break;
            case Device_U1:
                //U1没有超高频功能
                break;
            case Device_U3:
                setGpioStatus(U3_HXUHF_PowerOn, U3_UP);
                try {
                    mSerialPort = new SerialPort(new File(U3_HXUHF_Serialport_Path), U3_HXUHF_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---U3----------" + e.toString());

                }
                break;
            case Device_U8:
                //U8是R2000超高频
                break;
            case Device_A370_CW20:

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
                setGpioStatus(CPOS800_RED5UHF_PowerOn, CPOS800_DOWN);
                if (isCPOS800Stm32) {
                    setGpioStatus(CPOS800_STM32_PowerOn, CPOS800_DOWN);
                }

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
                if (isA370FBIDevice()) {
                    setGpioStatus(A370_FBI_GPIO_DEV, CFON640_DOWN);

                }
                break;
            case Device_U1:

                break;
            case Device_U3:
                setGpioStatus(U3_HXUHF_PowerOn, U3_DOWN);
                break;
            case Device_U8:


                break;
            case Device_A370_CW20:


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





    private LooperBuffer looperBuffer;

    private int mCurrentSize = 0;

    private byte[] mBuffer = new byte[50 * 1024];


    public void setLoopBuffer(LooperBuffer looperBuffer) {
        this.looperBuffer = looperBuffer;
    }



        /**
     * @param buffer
     * @param waittime
     * @param requestLength
     * @return
     */
    public synchronized int readFixedLength(byte buffer[], int waittime, int requestLength) {
        return readFixedLength(buffer, waittime, requestLength, 15);
    }

    /**
     * @param buffer
     * @param waittime
     * @param requestLength
     * @param interval
     * @return
     */
    public synchronized int readFixedLength(byte buffer[], int waittime, int requestLength, int interval) {
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
                if (mCurrentSize == requestLength) {
                    shutDown = true;
                } else {
                    currentTime = System.currentTimeMillis();
                    currentRecSize = mCurrentSize;
                    if (currentRecSize > lastRecSize) {
                        lastTime = currentTime;
                        lastRecSize = currentRecSize;
                    } else if (currentRecSize == lastRecSize && currentTime - lastTime >= interval) {
                        shutDown = true;
                    }
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
