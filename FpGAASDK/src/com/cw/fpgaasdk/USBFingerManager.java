package com.cw.fpgaasdk;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * 作者：李阳
 * 时间：2019/4/24
 * 描述：
 */
public class USBFingerManager {


    private static final String TAG = "CW" + "BYDBigUSBFingerM";


    private static String BYD_BIG_DEVICE = "BHMDevice";
    public static String BYD_BIG_DEVICE2 = "ZiDevice";

    //public static String BYD_SMALL_DEVICE = "USBKey Chip";
    //public static String Aratek_FBI_DEVICE = "Aratek     ";


    private UsbManager mUsbManager;

    private UsbDevice mDevice;

    private PendingIntent mPermissionIntent;

    //设备打开
    private boolean mDeviceOpened = false;

    private OnUSBFingerListener onUSBFingerListener;

    private Context mContext;

    private USBReceiver receiver;

    private boolean isRegisterReceiver = false;

    private final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }

    private long delayMs = 1000;


    private static volatile USBFingerManager singleton;

    private USBFingerManager(Context context) {
        this.mContext = context;
    }

    public static USBFingerManager getInstance(Context context) {
        if (singleton == null) {
            synchronized (USBFingerManager.class) {
                if (singleton == null) {
                    singleton = new USBFingerManager(context);

                }
            }
        }
        return singleton;
    }


    /**
     * 切换USB到指纹模块
     *
     * @param usbFingerListener
     * @return
     */
    public boolean openUSB(final OnUSBFingerListener usbFingerListener) {

        Log.i(TAG, "--------------------------openUSB--------------------------------");
        this.onUSBFingerListener = usbFingerListener;

        receiver = new USBReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        mContext.registerReceiver(receiver, filter);
        isRegisterReceiver = true;

        //先关闭usb，避免再进入该界面时，白屏
        USBSwitch.getInstance().closeUSB();
        USBSwitch.getInstance().openUSB();

        mUsbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
        mPermissionIntent = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);

        return true;
    }

    /**
     * 切换USB到正常模式
     *
     * @return
     */
    public boolean closeUSB() {

        if (isRegisterReceiver) {
            mContext.unregisterReceiver(receiver);
        }

        try {
            USBSwitch.getInstance().closeUSB();
            return true;
        } catch (Exception e) {
            USBSwitch.getInstance().closeUSB();
            return false;
        }
    }

    public boolean isUSBFingerOpened() {
        return mDeviceOpened;
    }


    private class USBReceiver extends BroadcastReceiver {

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action == null) {
                onUSBFingerListener.onOpenUSBFingerFailure("----------------action = null----------------");
                return;
            }

            switch (action) {
                case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                    Log.i(TAG, "---------------------USB--ATTACHED----------------------------------");
                    List<UsbDevice> deviceList = getDeviceList();
                    if (deviceList.size() == 0) {
                        onUSBFingerListener.onOpenUSBFingerFailure("UsbDevice = 0!");
                        return;
                    }
                    UsbDevice device = deviceList.get(0);
                    final String manufacturerName = device.getManufacturerName();

                    switch (manufacturerName) {
                        case "ZiDevice":
                            Log.i(TAG, "----------------USB设备: ZiDevice----------------");
                            getUSBDevice();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!mUsbManager.hasPermission(mDevice)) {
                                        mDeviceOpened = false;
                                    }
                                    onUSBFingerListener.onOpenUSBFingerSuccess(manufacturerName);
                                    mDeviceOpened = true;
                                }
                            }, delayMs);
                            break;


                        case "BHMDevice":

                            Log.i(TAG, "----------------USB设备: BHMDevice----------------");
                            getUSBDevice();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!mUsbManager.hasPermission(mDevice)) {
                                        mDeviceOpened = false;
                                    }
                                    onUSBFingerListener.onOpenUSBFingerSuccess(manufacturerName);
                                    mDeviceOpened = true;
                                }
                            }, delayMs);

                            break;

                    }
                    break;
                case UsbManager.ACTION_USB_DEVICE_DETACHED:
                    //Toast.makeText(mContext, "DETACHED", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "---------------------USB--DETACHED----------------------------------");
                    break;
            }
        }

    }

    /**
     * 获取指纹设备
     */
    private void getUSBDevice() {
        for (UsbDevice device : mUsbManager.getDeviceList().values()) {
            if (0x2109 == device.getVendorId() && 0x7638 == device.getProductId()) {
                mDevice = device;
                mUsbManager.requestPermission(mDevice, mPermissionIntent);
                //logMsg("find this usb device vID:0x2109");
                break;
            }
            if (0x0453 == device.getVendorId() && 0x9005 == device.getProductId()) {
                mDevice = device;
                mUsbManager.requestPermission(mDevice, mPermissionIntent);
                //logMsg("find this usb device vID:0x0453");
                break;
            }
        }
    }


    private List<UsbDevice> getDeviceList() {
        HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        List<UsbDevice> usbDevices = new ArrayList<>();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();
            usbDevices.add(device);
            Log.e(TAG, "getDeviceList: " + device.getDeviceName());
        }
        return usbDevices;
    }


    public interface OnUSBFingerListener {

        void onOpenUSBFingerSuccess(String device);

        void onOpenUSBFingerFailure(String error);
    }

}
