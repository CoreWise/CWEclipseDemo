package android_serialport_api;

import android.os.SystemClock;
import android.util.Log;


import com.cw.serialportsdk.cw;
import com.cw.serialportsdk.utils.DataUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.InvalidParameterException;

/**
 * SerialPort Manager
 * 该串口管理类在20190517日废弃，已经分化到各个SDK里
 * @author Administrator
 */
@Deprecated
public class SerialPortManager {

    private static final String TAG = "CW" + "SerialPortM";

    /**
     * 串口波特率(Serial Baudrate)
     */
    private static int BAUDRATE = 460800;

    public  static boolean switchRFID = false;

    final byte[] UP = {'1'};
    final byte[] DOWN = {'0'};

    final byte[] FBIUP = {'2'};
    final byte[] FBIDOWN = {'3'};

    private  String PATH = "/dev/ttyHSL0";

    private  String DEFAULT_PATH = "/dev/ttyHSL1";
    private  int DEFAULT_BAUDRATE = 115200;

    private  String SFZ_PATH = "/dev/ttyHSL1";
    private  int BAUDRATE_SFZ = 115200;

    private  int BAUDRATE_UHF = 115200;
    private  String UHF_PATH = "/dev/ttyHSL1";


    private  String GPIO_DEV = "/sys/class/pwv_gpios/as602-en/enable";

    private  String GPIO_DEV_STM32 = "/sys/class/gpio_power/stm32power/enable";

    private  String New_A370_GPIO_DEV_STM32 = "/sys/class/stm32_gpios/stm32-en/enable";

    private  String GPIO_DEV_SFZ = "/sys/class/stm32_gpios/stm32-en/enable";


    private  static SerialPortManager mSerialPortManager = new SerialPortManager();

    private  final byte[] SWITCH_COMMAND = "D&C00040104".getBytes();

    private SerialPort mSerialPort = null;

    private boolean isOpen;

    private boolean firstOpen = false;

    private OutputStream mOutputStream;

    private InputStream mInputStream;

    private byte[] mBuffer = new byte[50 * 1024];

    //添加volatile，防止死锁
    private volatile int mCurrentSize = 0;

    private LooperBuffer looperBuffer;
/**/

    private ReadThread mReadThread;
    private ReadUHFThread mReadUHFThread;
    private ReadSFZThread mReadSFZThread;

    /**
     * 获取该类的实例对象，为单例（get single instance）
     *
     * @return
     */
    public static SerialPortManager getInstance() {
        return mSerialPortManager;
    }


    public void setBaudrate(int baudrate) {
        BAUDRATE = baudrate;
    }

    /**
     * 判断串口是否打开(Serial Port is Open?)
     *
     * @return true：打开 false：未打开
     */
    public boolean isOpen() {
        return isOpen;
    }

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


    public boolean openSerialPort() {
        return openSerialPort(cw.type.DEFAULT);
    }

    /**
     * 打开串口(open SerialPort)
     *
     * @param type
     * @return
     */
    public boolean openSerialPort(int type) {
        if (mSerialPort == null) {
            // 上电
            try {
                switch (cw.getAndroidVersion()) {

                    case cw.deviceSysVersion.O://0: A370
                        setUp602Gpio();
                        if (isFBIDevice()) {
                            setDownGpioFbi();
                        }
                        //Log.i("whw", "setUpGpio status=" + getGpioStatus());
                        mSerialPort = new SerialPort(new File(PATH), BAUDRATE, 0);
                        break;
                    case cw.device.other: //其他机型

                        break;
                    case cw.deviceSysVersion.U:  //U3_640
                        setUpGpioSTM32();
                        setGpioSTM32(New_A370_GPIO_DEV_STM32, UP);
                        mSerialPort = new SerialPort(new File(DEFAULT_PATH), DEFAULT_BAUDRATE, 0);
                        break;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG,"---------------"+e.toString());
                return false;
            }

            if (null == mSerialPort)
            {
                Log.i(TAG, "初始化失败");
                return false;
            }

            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            switch (type) {
                case cw.type.sfz: //身份证
                    Log.i(TAG, "开启了身份证串口");

                    mReadSFZThread = new ReadSFZThread();
                    mReadSFZThread.start();
                    break;

                case cw.type.uhf: //超高频
                    Log.i(TAG, "开启了超高频串口");
                    mReadUHFThread = new ReadUHFThread();
                    mReadUHFThread.start();
                    break;
                case cw.type.DEFAULT:
                    Log.i(TAG, "开启了普通串口");
                    mReadThread = new ReadThread();
                    mReadThread.start();
                    break;
            }

            isOpen = true;
            firstOpen = true;
            return true;
        }
        return false;
    }


    /**
     * 关闭串口(close SerialPort)
     */
    public void closeSerialPort() {
        if (mReadThread != null) {
            mReadThread.interrupt();
            mReadThread = null;
        }

        if (mReadSFZThread != null) {
            mReadSFZThread.interrupt();
            mReadSFZThread = null;
        }

        if (mReadUHFThread != null) {
            mReadUHFThread.interrupt();
            mReadUHFThread = null;
        }


        switch (cw.getAndroidVersion()) {

            case cw.deviceSysVersion.O://0: A370
                // 断电
                setDown602Gpio();
                setDownGpioSTM32();
                break;

            case cw.device.other: //其他机型

                break;
            case cw.deviceSysVersion.U:  //U3_640
                // 断电
                //setDownGpio();
                setDownGpioSTM32();
                setGpioSTM32(New_A370_GPIO_DEV_STM32, DOWN);
                break;
        }


        if (mSerialPort != null) {
            try {
                mOutputStream.close();
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mSerialPort.close();
            mSerialPort = null;
        }
        isOpen = false;
        firstOpen = false;
        mCurrentSize = 0;
        switchRFID = false;
        if (looperBuffer != null) {
            looperBuffer = null;
        }
    }

    /**
     * U3
     * 打开关闭身份证串口
     *
     * @throws SecurityException
     * @throws IOException
     * @throws InvalidParameterException
     */
    @Deprecated
    private boolean openSerialPortSFZ() {
        if (mSerialPort == null) {
            try {
                setUpGpioSTM32();
                setGpioSTM32(New_A370_GPIO_DEV_STM32, UP);
                mSerialPort = new SerialPort(new File(SFZ_PATH), BAUDRATE_SFZ, 0);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            mReadSFZThread = new ReadSFZThread();
            mReadSFZThread.start();
            isOpen = true;
            return true;
        }
        return false;
    }

    @Deprecated
    private void closeSerialPortSFZ() {
        if (mReadSFZThread != null) {
            mReadSFZThread.interrupt();
        }
        mReadSFZThread = null;

        setDownGpioSTM32();
        setGpioSTM32(New_A370_GPIO_DEV_STM32, DOWN);

        if (mSerialPort != null) {
            try {
                mOutputStream.close();
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
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
     * U3
     * 打开关闭超高频
     *
     * @throws SecurityException
     * @throws IOException
     * @throws InvalidParameterException
     */
    @Deprecated
    private boolean openSerialPortUHF() {
        if (mSerialPort == null) {
            try {
                setUpGpioSTM32();

                mSerialPort = new SerialPort(new File(UHF_PATH), BAUDRATE_UHF, 0);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            mReadUHFThread = new ReadUHFThread();
            mReadUHFThread.start();
            isOpen = true;
            return true;
        }
        return false;
    }

    @Deprecated
    private void closeSerialPortUHF() {
        if (mReadUHFThread != null) {
            mReadUHFThread.interrupt();
        }
        mReadUHFThread = null;

        setDownGpioSTM32();

        if (mSerialPort != null) {
            try {
                mOutputStream.close();
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
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

    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    ////////  读写串口操作    ////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////

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
            closeSerialPort2();
            SystemClock.sleep(100);
            openSerialPort2();
        }
        return mCurrentSize;
    }


    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    ////////  上电操作    ////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////


    /**
     * 通用上电(set GPIO Pin up)
     *
     * @param GPIO
     * @param cmd
     * @throws IOException
     */

    private void setGpioSTM32(String GPIO, byte[] cmd) {
        FileOutputStream fw = null;
        try {
            fw = new FileOutputStream(GPIO);
            fw.write(cmd);
            fw.close();
            Log.i(TAG, GPIO + "-----" + DataUtils.bytesToHexString(cmd) + "--------------STM32---------上电了");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, GPIO + "-----" + DataUtils.bytesToHexString(cmd) + "-------------STM32 error---------------" + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, GPIO + "-----" + DataUtils.bytesToHexString(cmd) + "-------------STM32 error---------------" + e.toString());
        }

    }

    public void setUpGpioSTM32() {
        FileOutputStream fw = null;
        try {
            fw = new FileOutputStream(GPIO_DEV_STM32);
            fw.write(UP);
            fw.close();
            Log.i(TAG, GPIO_DEV_STM32 + "--------------STM32---------上电了");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "-------------STM32上电error---------------" + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "-------------STM32上电error---------------" + e.toString());
        }

    }


    public void setDownGpioSTM32() {
        FileOutputStream fw = null;
        try {
            fw = new FileOutputStream(GPIO_DEV_STM32);

            fw.write(DOWN);
            fw.close();
            Log.i(TAG, GPIO_DEV_STM32 + "--------------STM32---------下电了");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "-------------STM32下电error---------------" + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "-------------STM32下电error---------------" + e.toString());
        }


    }


    /**
     * 602上电
     *
     * @throws IOException
     */
    private void setUp602Gpio() {
        FileOutputStream fw = null;
        try {
            fw = new FileOutputStream(GPIO_DEV);
            fw.write(UP);
            fw.close();
            Log.i(TAG, GPIO_DEV + "--------------602---------上电了");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, GPIO_DEV + "--------------602---------上电了" + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, GPIO_DEV + "--------------602---------上电了" + e.toString());
        }

    }


    /**
     * 602下电
     */
    private void setDown602Gpio() {
        FileOutputStream fw = null;
        try {
            fw = new FileOutputStream(GPIO_DEV);

            fw.write(DOWN);
            fw.close();
            Log.i(TAG, GPIO_DEV + "--------------602---------下电了");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, GPIO_DEV + "--------------602---------下电了" + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG, GPIO_DEV + "--------------602---------下电了" + e.toString());
        }

    }


    @Deprecated
    public boolean openSerialPort2() {
        if (mSerialPort == null) {
            try {
                mSerialPort = new SerialPort(new File(PATH), BAUDRATE, 0);
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.i("whw", "mSerialPort=" + mSerialPort);
            mOutputStream = mSerialPort.getOutputStream();
            mInputStream = mSerialPort.getInputStream();
            mReadThread = new ReadThread();
            mReadThread.start();
            isOpen = true;
            firstOpen = true;
            return true;
        }
        return false;
    }

    @Deprecated
    private void closeSerialPort2() {
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
            }
            mSerialPort.close();
            mSerialPort = null;
        }
        isOpen = false;
        firstOpen = false;
        mCurrentSize = 0;
        switchRFID = false;
        if (looperBuffer != null) {
            looperBuffer = null;
        }
    }

    public void clearBuffer() {
        mBuffer = null;
        mBuffer = new byte[50 * 1024];
        mCurrentSize = 0;
    }

    public void setLoopBuffer(LooperBuffer looperBuffer) {
        this.looperBuffer = looperBuffer;
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

        }
    }


    public synchronized void write(byte[] data) {
        Log.i(TAG, "send commnad=" + DataUtils.toHexString(data));
        writeCommand(data);
    }


    private void setDownGpioFbi() throws IOException {
        FileOutputStream fw = new FileOutputStream("/sys/class/fbicode_gpios/fbicoe_state/control");
        fw.write(FBIDOWN);
        fw.close();
    }

    private boolean isFBIDevice() {
        String path = "/sys/class/fbicode_gpios/fbicoe_state/control";
        File file = new File(path);
        return file.exists();
    }

    public String getGpioStatus() throws IOException {
        String value;
        BufferedReader br = null;
        FileInputStream inStream = new FileInputStream(GPIO_DEV);
        br = new BufferedReader(new InputStreamReader(inStream));
        value = br.readLine();
        inStream.close();
        return value;

    }


    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    ////////     各种读取串口的线程    ////////////////////////////////////
    /////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////


    /**
     * 通用读取串口线程(General Read Serial Thread)
     */
    private class ReadThread extends Thread {
        @Override
        public void run() {
            byte[] buffer = new byte[2325];
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
                            Log.i("xuws", "recv buf=" + DataUtils.toHexString(buf));
                            looperBuffer.add(buf);
                        }
                        System.arraycopy(buffer, 0, mBuffer, mCurrentSize, length);
                        mCurrentSize += length;
                        Log.i("whw", "mCurrentSize=" + mCurrentSize + "  length=" + length);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    /**
     * 身份证读取串口线程(Read IDCard Serial Thread)
     */
    private class ReadSFZThread extends Thread {
        @Override
        public void run() {
            while (!isInterrupted()) {
                int length = 0;
                byte[] buffer = new byte[2325];

                try {
                    if (mInputStream == null) {
                        return;
                    }
                    if (mInputStream.available() > 0 == false) {
                        continue;
                    } else {
                        switch (cw.getAndroidVersion()) {

                            case cw.deviceSysVersion.O://0: A370
                                Thread.sleep(200);   //此处延时确保一次性获取数据，最低190ms

                                break;
                            case cw.device.other: //其他机型
                                Thread.sleep(200);   //此处延时确保一次性获取数据，最低190ms

                                break;
                            case cw.deviceSysVersion.U:  //U3_640
                                Thread.sleep(200);   //此处延时确保一次性获取数据，最低190ms
                                break;

                        }
                    }

                    length = mInputStream.read(buffer);
                    if (length > 0) {
                        /*if (looperBuffer != null) {
                            byte[] buf = new byte[length];
                            System.arraycopy(buffer, 0, buf, 0, length);
                            Log.i("xuws", "recv buf=" + DataUtils.toHexString(buf));
                            looperBuffer.add(buf);
                        }*/
                        Log.i("whw22", "--length--" + length + "--buffer--" + DataUtils.bytesToHexString(buffer));

                        System.arraycopy(buffer, 0, mBuffer, mCurrentSize, length);
                        //mCurrentSize += length;
                        mCurrentSize = length;
                        Log.i("whw", "mCurrentSize=" + mCurrentSize + "  length=" + length);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 读取超高频串口线程(Read UHF Serial Thread)
     */
    private class ReadUHFThread extends Thread {
        @Override
        public void run() {
            byte[] buffer = new byte[2325];
            while (!isInterrupted()) {
                int length = 0;
                try {
                    if (mInputStream == null) {
                        return;
                    }
                    if (mInputStream.available() > 0 == false) {
                        continue;
                    } else {
                        Thread.sleep(10);
                    }
                    length = mInputStream.read(buffer);
                    if (length > 0) {
                        if (looperBuffer != null) {
                            byte[] buf = new byte[length];
                            System.arraycopy(buffer, 0, buf, 0, length);
                            Log.i("xuws", "recv buf=" + DataUtils.toHexString(buf));
                            looperBuffer.add(buf);
                        }
                        System.arraycopy(buffer, 0, mBuffer, mCurrentSize, length);
                        mCurrentSize += length;
                        Log.i("whw", "mCurrentSize=" + mCurrentSize + "  length=" + length);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
