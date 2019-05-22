package com.cw.beidousdk;

import android.os.SystemClock;
import android.util.Log;


import com.cw.serialportsdk.utils.DataUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

import android_serialport_api.LooperBuffer;
import android_serialport_api.SerialPort;

/**
 * 串口控制类
 * 张振东
 * 北斗
 */
public class BeiDouSerialPortManager {
    private final String TAG = "cw" + "SerialPortManager";
    private final int BAUDRATE = 115200;

    private final byte[] CHAR0 = {'0'};
    private final byte[] CHAR1 = {'1'};
    private final byte[] CHAR2 = {'2'};
    private final byte[] CHAR3 = {'3'};
    private final byte[] CHAR4 = {'4'};
    private final byte[] CHAR5 = {'5'};
    private final byte[] CHAR6 = {'6'};

    private final String PATH1 = "/dev/ttyHSL1";
    private final String PATH2 = "/dev/ttyHSL2";
    //	private final String GPIO_DEV_STM32_POWER = "sys/class/gpio_power/stm32power/poweron";
    private final String GPIO_DEV_STM32_ENABLE = "/sys/class/gpio_power/stm32power/enable";
    private final String GPIO_DEV_FINGER = "/sys/class/u8finger/u8finger/poweron";

    private SerialPort mSerialPort = null;

    private OutputStream mOutputStream;

    private InputStream mInputStream;

    private byte[] mBuffer = new byte[50 * 1024];

    private int mCurrentSize = 0;

    private ReadThread mReadThread;

    private boolean isOpen;

    private LooperBuffer looperBuffer;

    private static class SerialPortManagerHolder {
        private static final BeiDouSerialPortManager INSTANCE = new BeiDouSerialPortManager();
    }

    private BeiDouSerialPortManager() {
    }

    public static BeiDouSerialPortManager getInstance() {
        return SerialPortManagerHolder.INSTANCE;
    }

    /**
     * 给北斗模块供电前需要拉的脚
     *
     * @param isUp 拉高还是拉低
     * @return 是否成功
     */
    public boolean upGPIOBeiDou(boolean isUp) {
        try {
            FileOutputStream f1 = new FileOutputStream(GPIO_DEV_STM32_ENABLE);
            f1.write(isUp ? CHAR2 : CHAR3);
            f1.close();

            FileOutputStream f2 = new FileOutputStream(GPIO_DEV_STM32_ENABLE);
            f2.write(isUp ? CHAR1 : CHAR0);
            f2.close();

            FileOutputStream f3 = new FileOutputStream(GPIO_DEV_STM32_ENABLE);
            f3.write(isUp ? CHAR4 : CHAR5);
            f3.close();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "------------------BeiDouSerialPortManager:--upGPIOBeiDou--------------------------" + e.toString());
            return false;
        }
    }

    public boolean upGPIOBeiDou2(boolean isUp) {
        try {
            if (isUp) {
                Thread.sleep(2000);
            }

            FileOutputStream f4 = new FileOutputStream(GPIO_DEV_FINGER);
            f4.write(isUp ? CHAR6 : CHAR5);
            f4.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "------------------BeiDouSerialPortManager:--upGPIOBeiDou2--------------------------" + e.toString());
            return false;
        }
    }

    /**
     * 打开串口
     *
     * @param tty1OrTtty2 串口1还是2 true:串口1;false:串口2
     * @return 是否成功
     */
    public boolean openSerialPort(boolean tty1OrTtty2) {
        if (mSerialPort == null) {
            try {
                mSerialPort = new SerialPort(new File(tty1OrTtty2 ? PATH1 : PATH2), BAUDRATE, 0);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "------------------BeiDouSerialPortManager:----openSerialPort------------------------" + e.toString());
                return false;
            }

            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            mReadThread = new ReadThread();
            mReadThread.start();
            isOpen = true;
            return true;
        }
        return false;
    }

    /**
     * 关闭串口
     */
    public void closeSerialPort() {
        if (mReadThread != null) {
            mReadThread.interrupt();
        }
        mReadThread = null;
        if (mSerialPort != null) {
            try {
                mOutputStream.close();
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "------------------BeiDouSerialPortManager:----openSerialPort------------------------" + e.toString());
            }
            mSerialPort.close();
            mSerialPort = null;
        }
        isOpen = false;
        mCurrentSize = 0;
        if (looperBuffer != null) {
            looperBuffer = null;
        }
    }

    /**
     * 从串口读取数据
     *
     * @param buffer   数据缓冲区
     * @param timeout  超时时间
     * @param interval 每次间隔时间
     * @return 有效数据长度
     */
    public synchronized int read(byte buffer[], int timeout, int interval) {
        if (!isOpen) {
            Log.e(TAG, "------------------BeiDouSerialPortManager:----read------------------------没有打开串口");
            return 0;
        }
        int sleepTime = 5;
        int length = timeout / sleepTime;
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

    public void setLoopBuffer(LooperBuffer looperBuffer) {
        this.looperBuffer = looperBuffer;
    }

    public void closeLoopBuffer() {
        this.looperBuffer = null;
    }

    /**
     * 给串口写数据
     *
     * @param data 写入的数据
     */
    public synchronized void write(byte[] data) {
        Log.i(TAG, "---write-----send commnad=" + DataUtils.toHexString(data));
        writeCommand(data);
    }

    private void writeCommand(byte[] data) {
        if (!isOpen) {
            return;
        }
        mCurrentSize = 0;
        try {
            mOutputStream.write(data);
        } catch (IOException e) {
            Log.e(TAG, "------------------BeiDouSerialPortManager:----writeCommand---------------------" + e.toString());
        }
    }

    /**
     * 清空串口数据缓冲
     */
    public void clearBuffer() {
        mBuffer = null;
        mBuffer = new byte[50 * 1024];
        mCurrentSize = 0;
    }

    /**
     * 获取gpio状态
     *
     * @param GPIO_DEV gpio路径
     * @return 最后状态
     * @throws IOException
     */
    public String getGpioStatus(String GPIO_DEV) throws IOException {
        String value;
        BufferedReader br = null;
        FileInputStream inStream = new FileInputStream(GPIO_DEV);
        br = new BufferedReader(new InputStreamReader(inStream));
        value = br.readLine();
        inStream.close();
        return value;
    }

    private class ReadThread extends Thread {

        @Override
        public void run() {
            byte[] buffer = new byte[512];
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
                            Log.i(TAG, "recv buf=" + DataUtils.toHexString(buf));
                            looperBuffer.add(buf);
                        }
                        System.arraycopy(buffer, 0, mBuffer, mCurrentSize, length);
                        mCurrentSize += length;
                        Log.i(TAG, "mCurrentSize=" + mCurrentSize + "  length=" + length);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}