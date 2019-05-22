package com.cw.serialportsdk;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.UnsupportedEncodingException;

import android_serialport_api.FirmwareVersionSerialPortManager;

/**
 * 时间：2018/10/16
 * 描述：
 * <p>
 * 型号封装,后续产品都可以在这里添加型号，客户定制也可以
 * <p>
 * <p>
 * 兼容性说明:
 * 身份证:CPOS800，U3，U8,
 *
 *
 *
 *
 */
public class cw {


    /**
     * 肯麦思产品设备型号接口
     */
    @Deprecated
    public interface device {
        int A370 = 0;
        int CFON640 = 1;
        int other = 2;
        int U3_640 = 3;
        int U3_A370 = 4;
    }


    @Deprecated
    private interface scrren {
        int screen_5_height = 1280;
        int screen_5_width = 720;

        int screen_43_height = 800;
        int screen_43_with = 480;
    }


    /**
     * 肯麦思产品设备型号系统版本接口
     */
    @Deprecated
    public interface deviceSysVersion {

        /**
         * 海派之类老产品
         */
        int O = Build.VERSION_CODES.M;

        /**
         * U系列的新产品
         */
        int U = Build.VERSION_CODES.N_MR1;
    }


    /**
     * 肯麦思产品设备功能接口
     */
    @Deprecated
    public interface type {
        int DEFAULT = 100;
        int sfz = 111;
        int uhf = 112;
    }

    @Deprecated
    public static String[] deviceName = new String[]{"A370", "CFON640", "其他机型", "msm8953 for arm64"};


    /**
     * 获取设备型号，用来判断使用什么指令
     *
     * @return 0: A370 1:CFON640 2:其他机型,3:msm8953 for arm64
     */

    @Deprecated
    public static int getModel() {
        String model = Build.MODEL;
        Log.i("TAG", "---2---" + model);
        File file = new File("/sys/class/fbicode_gpios/fbicoe_state/control");
        if (model.contains("CFON640") || model.contains("COREWISE_V0")) {
            return device.CFON640;
        } else if (model.equals("A370") || file.exists()) {
            return device.A370;
        } else if (model.equals("msm8953 for arm64")) {
            return device.U3_640;
        } else if (model.equals("msm8909")) {
            return device.U3_A370;
        } else /*if (model.equals("U3")) {
            return device.U3_640;
        }else*/ {
            return 2;
        }
    }

    @Deprecated
    public static int getAndroidVersion() {
        int sdkInt = Build.VERSION.SDK_INT;
        return sdkInt;
    }


    private static byte[] stm32 = { (byte) 0xca, (byte) 0xdf, (byte) 0x07, 0x36, 0x01, (byte) 0xe3 };
	private static byte[] v32550 = { 0x09, 0x00, 0x00, 0x09 };
	private static byte[] buffer = new byte[1024];
	private static byte[] data = new byte[1024];

	/**
	 * 获取apk版本号
	 *
	 * @param context
	 * @return apk版本号
	 */
	public static String getApkVersion(Context context) {
		String appVersion = null;
		PackageManager manager = context.getPackageManager();
		try {
			PackageInfo info = manager.getPackageInfo(context.getPackageName(), 0);
			appVersion = info.versionName; // 版本名
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
			Log.e(TAG,"------------getApkVersion------------"+e.toString());
		}
		return appVersion;
	}

	/**
	 * 获取stm32返回的版本号
	 *
	 * @return 版本号
	 */
	@RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getStm32Version() {

	    FirmwareVersionSerialPortManager.getInstance().openSerialPort(getDeviceModel());
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
            Log.e(TAG,"------------getStm32Version------------"+e.toString());
		}
		FirmwareVersionSerialPortManager.getInstance().write(stm32);

		String strdata = "";
		int length = FirmwareVersionSerialPortManager.getInstance().read(buffer, 2000, 100);
		Log.d(TAG, "getStm32Version: " + length);

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			e.printStackTrace();
            Log.e(TAG,"------------getStm32Version------------"+e.toString());
		}
        FirmwareVersionSerialPortManager.getInstance().write(stm32);


        length = FirmwareVersionSerialPortManager.getInstance().read(buffer, 2000, 100);
		Log.d(TAG, "getStm32Version: " + length);

		if (length > 0) {
			byte[] getData = new byte[length];
			System.arraycopy(buffer, 0, getData, 0, length);
			strdata = new String(getData);
		}
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
            Log.e(TAG,"------------getStm32Version------------"+e.toString());
		}

        FirmwareVersionSerialPortManager.getInstance().closeSerialPort(getDeviceModel());

		return strdata;
	}









    private static final String TAG = "CWDevice";

    //////////////产品设备型号接口
    public final static int Device_None = -1;

    public final static int Device_CPOS800 = 0;
    public final static int Device_CFON600 = 1;
    public final static int Device_CFON640 = 2;
    public final static int Device_A370_M4G5 = 3;

    public final static int Device_U1 = 10;
    public final static int Device_U3 = 11;
    public final static int Device_U8 = 12;
    public final static int Device_A370_CW20 = 13;

    //////////////

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
     * 获取设备型号，用来判断使用什么机器
     */


    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static int getDeviceModel() {
        String model = Build.MODEL + "#" + Build.DISPLAY + "#" + Build.VERSION.SDK_INT + "#" + Build.VERSION.RELEASE;
        Log.e(TAG, "------------getDevice--------------" + model);

        ///////////////////////////////////////////////////////////////////////////////////
        /////////////////////////////////////以下为公司产品内部正式叫法名字//////////////////////////////////////////////

        if (model.indexOf("CFON600") != -1) {
            Log.e(TAG, "------------getDevice--------------CFON600机器");

            return Device_CFON600;
        }
        if (model.indexOf("CFON640") != -1) {
            Log.e(TAG, "------------getDevice--------------CFON640机器");

            return Device_CFON640;
        }

        if (model.indexOf("CPOS800") != -1) {
            Log.e(TAG, "------------getDevice--------------CFON800机器");

            return Device_CPOS800;
        }


        if (model.indexOf("A370") != -1 && model.indexOf("6.0.1") != -1) {
            Log.e(TAG, "------------getDevice--------------海派A370机器");

            return Device_A370_M4G5;
        }

        if (model.indexOf("A370") != -1 && model.indexOf("7.1.2") != -1) {
            Log.e(TAG, "------------getDevice--------------新A370机器");

            return Device_A370_CW20;
        }


        if (model.indexOf("U1") != -1) {
            Log.e(TAG, "------------getDevice--------------U1机器");

            return Device_U1;
        }

        if (model.indexOf("U3") != -1) {
            Log.e(TAG, "------------getDevice--------------U3机器");

            return Device_U3;
        }

        if (model.indexOf("U8") != -1) {
            Log.e(TAG, "------------getDevice--------------U8机器");

            return Device_U8;
        }

        ///////////////////////////////////////////////////////////////////////////////////
        //////////////////////////////////以下为客户定制产品叫法名字,优先级比本公司内部叫法低//////////////////////////
        //////////////////////////////////客户定制需要在此处添加说明//////////////////////////
        //////////////////////////////////1.需要系统开发人员提供机器定制model参数//////////////////////////

        ////////////////////////////////////////////////////////////////////////////////////

        /*if (model.indexOf("HR853") != -1) {
            Log.e(TAG, "------------getDevice--------------U8机器---客户定制系统:百事吉电子");
            return Device_U8;
        }*/


        ///////////////////////////////////////////////////////////////////////////////////
        /////////////以下为读取sd卡根目录配置文件，优先级最低，配置文件为xml格式:device//////////////////////////////////
        /////////////////////////////////////后续再加///////////////////////////////////////////////


        String device = Environment.getExternalStorageDirectory().getPath() + File.separator + "device";
        File deviceFile = new File(device);
        if (deviceFile.exists()) {
            Log.e(TAG, "------------getDevice--------------配置文件存在");

            StringBuilder result = new StringBuilder();
            try {
                //构造一个BufferedReader类来读取文件
                BufferedReader br = new BufferedReader(new FileReader(deviceFile));
                String s = null;
                //使用readLine方法，一次读一行
                while ((s = br.readLine()) != null) {
                    result.append(System.lineSeparator() + s);
                }
                br.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (result.indexOf(Build.MODEL) != -1) {
                Log.e(TAG, "------------getDevice--------------是本机器");
            }


            return dddevice(result.toString());

        }


        Log.e(TAG, "------------getDevice--------------未能识别机器型号是啥？请联系技术人员！");

        return Device_None;


    }

    private static int dddevice(String model) {
        if (model.indexOf("CFON600") != -1) {
            Log.e(TAG, "------------getDevice--------------CFON600机器");

            return Device_CFON600;
        }
        if (model.indexOf("CFON640") != -1) {
            Log.e(TAG, "------------getDevice--------------CFON640机器");

            return Device_CFON640;
        }

        if (model.indexOf("CPOS800") != -1) {
            Log.e(TAG, "------------getDevice--------------CFON800机器");

            return Device_CPOS800;
        }


        if (model.indexOf("A370") != -1 && model.indexOf("m4g5") != -1) {
            Log.e(TAG, "------------getDevice--------------海派A370机器");

            return Device_A370_M4G5;
        }

        if (model.indexOf("A370") != -1 && model.indexOf("cw20") != -1) {
            Log.e(TAG, "------------getDevice--------------新A370机器");

            return Device_A370_CW20;
        }


        if (model.indexOf("U1") != -1) {
            Log.e(TAG, "------------getDevice--------------U1机器");

            return Device_U1;
        }

        if (model.indexOf("U3") != -1) {
            Log.e(TAG, "------------getDevice--------------U3机器");

            return Device_U3;
        }

        if (model.indexOf("U8") != -1) {
            Log.e(TAG, "------------getDevice--------------U8机器");

            return Device_U8;
        }
        Log.e(TAG, "------------getDevice--------------未能识别机器型号是啥？请联系技术人员！");
        return Device_None;

    }

}
