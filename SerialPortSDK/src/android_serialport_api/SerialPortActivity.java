/*
 * Copyright 2009 Cedric Priscal
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package android_serialport_api;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;


import com.cw.serialportsdk.R;
import com.cw.serialportsdk.utils.DataUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;

public abstract class SerialPortActivity extends FragmentActivity {


    private final String TAG = "CW" + "SerialPort";
    private int BAUDRATE = 460800;
    private String PATH = "/dev/ttyHSL0";
    protected OutputStream mOutputStream;
    private InputStream mInputStream;
    private ReadThread mReadThread;
    private SerialPort mSerialPort = null;

    private static String GPIO_DEV = "/sys/class/pwv_gpios/as602-en/enable";
    private String SwitchGPIO = "/sys/class/fbicode_gpios/fbicoe_state/control";

    private final byte[] Byte_602 = {'3'};
    private final byte[] Byte_FBI = {'2'};


    private final byte[] UP = {'1'};
    private final byte[] DOWN = {'0'};

    private HomeReceiver receiver;
    private IntentFilter intentFilter;

    private boolean isRegister = false;




    private void DisplayError(int resourceId) {
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Error");
        b.setMessage(resourceId);
        b.setPositiveButton("OK", new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SerialPortActivity.this.finish();
            }
        });
        b.show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "------------------SerialPortActivity---onCreate------------------------");
        switchGpio(Byte_602);
        this.setUpGpio();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            this.mSerialPort = new SerialPort(new File(PATH), BAUDRATE, 0);
            this.mOutputStream = this.mSerialPort.getOutputStream();
            this.mInputStream = this.mSerialPort.getInputStream();
            this.mReadThread = new ReadThread();
            this.mReadThread.start();
        } catch (SecurityException var3) {
            this.DisplayError(R.string.error_security);
            this.onErrorReceived(this.getResources().getString(R.string.error_security));
        } catch (IOException var4) {
            this.DisplayError(R.string.error_unknown);
            this.onErrorReceived(this.getResources().getString(R.string.error_unknown));
        } catch (InvalidParameterException var5) {
            this.DisplayError(R.string.error_configuration);
            this.onErrorReceived(this.getResources().getString(R.string.error_configuration));
        }

        receiver = new HomeReceiver();
        intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
        intentFilter.addAction(Intent.ACTION_SCREEN_ON);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(receiver, intentFilter);
        isRegister = true;

    }

    protected abstract void onDataReceived(byte[] buffer, int size);

    protected abstract void onErrorReceived(String error);

    @Override
    protected void onDestroy() {
        Log.e(TAG, "------------------SerialPortActivity---onDestroy------------------------");
        if (this.mReadThread != null) {
            this.mReadThread.interrupt();
            new Handler().removeCallbacks(mReadThread);
        }

        mSerialPort.close();
        mSerialPort = null;
        setDownGpio();
        if (isRegister) {

            unregisterReceiver(receiver);
            isRegister = false;
        }

        super.onDestroy();

    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "-----------------SerialPortActivity----onStop------------------------");

        setDownGpio();

        if (isRegister) {
            unregisterReceiver(receiver);
            isRegister = false;
        }


    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e(TAG, "-----------------SerialPortActivity----onRestart------------------------");

        setUpGpio();

        registerReceiver(receiver, intentFilter);
        isRegister = true;

    }

    public class HomeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i(TAG, "onReceive: action: " + action);
            if (action.equals(Intent.ACTION_SCREEN_ON)) {
                Log.i(TAG, "亮屏了");
                switchGpio(Byte_602);

                setUpGpio();
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                try {
                    mSerialPort = new SerialPort(new File(PATH), BAUDRATE, 0);
                    mOutputStream = mSerialPort.getOutputStream();
                    mInputStream = mSerialPort.getInputStream();
                    mReadThread = new ReadThread();
                    mReadThread.start();
                } catch (SecurityException var3) {
                    DisplayError(R.string.error_security);
                    onErrorReceived(getResources().getString(R.string.error_security));
                } catch (IOException var4) {
                    DisplayError(R.string.error_unknown);
                    onErrorReceived(getResources().getString(R.string.error_unknown));
                } catch (InvalidParameterException var5) {
                    DisplayError(R.string.error_configuration);
                    onErrorReceived(getResources().getString(R.string.error_configuration));
                }

            } else if (action.equals(Intent.ACTION_SCREEN_OFF)) {
                Log.i(TAG, "息屏了");
                if (mReadThread != null) {
                    mReadThread.interrupt();
                    new Handler().removeCallbacks(mReadThread);
                }


                mSerialPort.close();
                mSerialPort = null;
                setDownGpio();
            }
        }
    }


    public void writeCommand(byte[] data) {
        try {
            Log.e(TAG, "send commnad=" + DataUtils.toHexString(data));
            this.mOutputStream.write(data);
        } catch (IOException var3) {
            Log.e(TAG, "send commnad=" + DataUtils.toHexString(data) + " is " + var3.toString());
            this.onErrorReceived("send commnad=" + DataUtils.toHexString(data) + " is " + var3.toString());
        }

    }

    public void setUpGpio() {
        FileOutputStream fw = null;

        try {
            fw = new FileOutputStream(GPIO_DEV);
            fw.write(this.UP);
            fw.close();
        } catch (FileNotFoundException var3) {
            var3.printStackTrace();
            Log.e(TAG, "setUpGpio  is " + var3.toString());
            this.onErrorReceived("setUpGpio  is " + var3.toString());
        } catch (IOException var4) {
            var4.printStackTrace();
            Log.e(TAG, "setUpGpio  is " + var4.toString());
            this.onErrorReceived("setUpGpio  is " + var4.toString());
        }

    }

    public void setDownGpio() {
        FileOutputStream fw = null;

        try {
            fw = new FileOutputStream(GPIO_DEV);
            fw.write(DOWN);
            fw.close();
        } catch (FileNotFoundException var3) {
            var3.printStackTrace();
            Log.e(TAG, "setDownGpio  is " + var3.toString());
            this.onErrorReceived("setDownGpio  is " + var3.toString());
        } catch (IOException var4) {
            var4.printStackTrace();
            Log.e(TAG, "setDownGpio  is " + var4.toString());
            this.onErrorReceived("setDownGpio  is " + var4.toString());
        }

    }

    public int getGPIOStatus(String Path) {

        FileInputStream fr;
        int b = 0;
        try {
            fr = new FileInputStream(Path);
            b = fr.read();
            fr.close();
            Log.i(TAG, "getGPIO: " + Path + " status: " + b);
            return b;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "getGPIO: " + Path + " error:" + e.toString());
            return -1;

        }
    }

    public void switchGpio(byte[] flag) {
        FileOutputStream fw = null;
        try {
            fw = new FileOutputStream(SwitchGPIO);
            fw.write(flag);
            fw.close();
            Log.i(TAG, "switchGpio: " + SwitchGPIO + " status: " + DataUtils.bytesToAscii(flag, 0, flag.length));

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.e(TAG, "switchGpio  is " + e.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "switchGpio  is " + e.toString());
        }
    }

    private class ReadThread extends Thread {
        @Override
        public void run() {
            super.run();
            while (!isInterrupted()) {
                try {
                    byte[] buffer = new byte[2321];
                    if (mInputStream == null) {
                        return;
                    }

                    int size = mInputStream.read(buffer);
                    if (size > 0) {
                        onDataReceived(buffer, size);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }
}
