package com.cw.fpjrasdk;

import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;


public class USBSwitch {

    private static final String TAG = "CWUSBSwitch";

    private static USBSwitch mSwitchUtil = new USBSwitch();

    private String U3_GPIO_USB = "/sys/class/finger/finger/poweron";

    //U8,U3一样的
    private String GPIO = "/sys/class/gpio_power/stm32power/enable";

    private final byte[] U3_UP1 = {'1'};
    private final byte[] U3_UP2 = {'2'};

    private final byte[] U3_DOWN1 = {'0'};
    private final byte[] U3_DOWN2 = {'3'};


    private String U8_GPIO_USB = "/sys/class/u8finger/u8finger/poweron";


    private final byte[] U8_UP1 = {'1'};
    private final byte[] U8_UP2 = {'3'};

    private final byte[] U8_DOWN1 = {'0'};
    private final byte[] U8_DOWN2 = {'2'};


    /**
     * 获取单例（get Instance）
     *
     * @return USBSwitch
     */
    public static USBSwitch getInstance() {
        return mSwitchUtil;
    }

    /**
     * 打开USB(open usb)
     *
     * @return boolean
     */
    public void openUSB() {
        try {

            FileOutputStream u3fw = new FileOutputStream(U3_GPIO_USB);
            u3fw.write(U3_UP1);
            u3fw.close();
            u3fw = new FileOutputStream(U3_GPIO_USB);
            u3fw.write(U3_UP2);
            u3fw.close();

            setUpGpio();

            //return true;
        } catch (IOException e) {
            e.printStackTrace();
            //return false;
            Log.e(TAG, "-------------openUSB--------------" + e.toString());
        }


        try {
            FileOutputStream u8fw = new FileOutputStream(U8_GPIO_USB);
            u8fw.write(U8_UP1);
            u8fw.close();
            //Thread.sleep(500);
            u8fw = new FileOutputStream(U8_GPIO_USB);
            u8fw.write(U8_UP2);
            u8fw.close();

            //return true;
        } catch (IOException e) {
            e.printStackTrace();
            //Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            //return false;
            Log.e(TAG, "-------------openUSB--------------" + e.toString());

        }

    }

    /**
     * 关闭USB(close usb)
     *
     * @return boolean
     */
    public void closeUSB() {
        try {
            FileOutputStream u3fw = new FileOutputStream(U3_GPIO_USB);
            u3fw.write(U3_DOWN1);
            u3fw.close();

            u3fw = new FileOutputStream(U3_GPIO_USB);
            u3fw.write(U3_DOWN2);
            u3fw.close();

            u3fw.close();

            setDownGpio();
            //return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "-------------closeUSB--------------" + e.toString());

            //return false;
        }

        try {
            FileOutputStream u8fw = new FileOutputStream(U8_GPIO_USB);
            u8fw.write(U8_DOWN1);
            u8fw.close();

            //Thread.sleep(500);

            u8fw = new FileOutputStream(U8_GPIO_USB);
            u8fw.write(U8_DOWN2);
            u8fw.close();

            //return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "-------------closeUSB--------------" + e.toString());

            //return false;
        }
    }

    private boolean setUpGpio() throws IOException {
        try {
            FileOutputStream fw = new FileOutputStream(GPIO);
            fw.write(U3_UP1);
            fw.close();
            fw = new FileOutputStream(GPIO);
            fw.write(U3_UP2);
            fw.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "-------------setUpGpio--------------" + e.toString());

            return false;
        }
    }

    private boolean setDownGpio() throws IOException {
        try {
            FileOutputStream fw = new FileOutputStream(GPIO);
            fw.write(U3_DOWN1);
            fw.close();
            fw = new FileOutputStream(GPIO);
            fw.write(U3_DOWN2);
            fw.close();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "-------------setDownGpio--------------" + e.toString());

            return false;
        }
    }
}