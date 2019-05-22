package android_serialport_api;

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

import static com.cw.serialportsdk.cw.*;


/**
 * 作者：李阳
 * 时间：2019/5/14
 * 描述：固件版本串口管理类
 */
public class FirmwareVersionSerialPortManager {

    private static final String TAG = "CWFirmare" + "SerialPortM";



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



     /**
     * U3
     */
    private  String U3_STM32_GPIO_DEV_PATH = "/sys/class/gpio_power/stm32power/enable";
    private  String U3_IdCard_Serialport_Path = "/dev/ttyHSL1";
    private  int U3_IdCard_Baudrate = 115200;
    private byte[] U3_UP = {'1'};
    private byte[] U3_DOWN = {'0'};

    /**
     * U8
     */
    private  String U8_STM32_GPIO_DEV_PATH = "/sys/class/gpio_power/stm32power/enable";
    private  String U8_IdCard_Serialport_Path = "/dev/ttyHSL1";
    private  int U8_IdCard_Baudrate = 115200;
    private byte[] U8_UP = {'1'};
    private byte[] U8_DOWN = {'0'};


     /**
     * A370
     */


    /**
     * 新A370
     */
    private  String New_A370_GPIO_DEV_STM32 = "/sys/class/stm32_gpios/stm32-en/enable";
    private  String New_A370_IdCard_Serialport_Path = "/dev/ttyHSL1";
    private  int New_A370_IdCard_Baudrate = 115200;
    private byte[] New_A370_UP = {'1'};
    private byte[] New_A370_DOWN = {'0'};



    /**
     * 获取该类的实例对象，为单例
     *
     * @return
     */
    private volatile static FirmwareVersionSerialPortManager mSerialPortManager;

    public static FirmwareVersionSerialPortManager getInstance() {
        if (mSerialPortManager == null) {
            synchronized (FirmwareVersionSerialPortManager.class) {
                if (mSerialPortManager == null) {
                    mSerialPortManager = new FirmwareVersionSerialPortManager();
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


                break;
            case Device_CFON640:

                break;
            case Device_A370_M4G5:

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
                setGpioStatus(U8_STM32_GPIO_DEV_PATH, U8_UP);
                try {
                    mSerialPort = new SerialPort(new File(U8_IdCard_Serialport_Path), U8_IdCard_Baudrate, 0);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "------------selectDevice---U8----------" + e.toString());

                }
                break;
            case Device_A370_CW20:
                //新A370,除了上电路径和U8不一样，其他都一样
                setGpioStatus(New_A370_GPIO_DEV_STM32,New_A370_UP);
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

                break;
            case Device_A370_M4G5:

                break;
            case Device_U1:

                break;
            case Device_U3:
                setGpioStatus(U3_STM32_GPIO_DEV_PATH,U3_DOWN);
                break;
            case Device_U8:
                setGpioStatus(U8_STM32_GPIO_DEV_PATH,U8_DOWN);

                break;
            case Device_A370_CW20:
                setGpioStatus(New_A370_GPIO_DEV_STM32,New_A370_DOWN);

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
