package com.cw.idcardsdk;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


import com.cw.serialportsdk.cw;




/**
 * 异步读取身份证类
 */
public class AsyncParseSFZ extends Handler {

    private static final String TAG = "cwidcardAsyncParseSFZ";

    private static final int READ_CARD_ID = 999;
    private static final int READ_SFZ = 1000;
    private static final int READ_IDCard = 110000;
    private static final int READ_MODULE = 2000;
    private static final int FIND_CARD_SUCCESS = 1001;
    private static final int FIND_CARD_FAIL = 1002;
    private static final int FIND_MODULE_SUCCESS = 1003;
    private static final int FIND_MODULE_FAIL = 1004;
    private static final int DATA_SIZE = 1295;

    public static final int OPEN_FINGERSCANNER_FAIL = 1111;
    public static final int OPEN_FINGERSCANNER_SUCCESS = 1112;
    public static final int BIONE_INIT_SUCCESS = 1113;
    public static final int BIONE_INIT_FAIL = 1114;

    public static final int CLOSE_FINGERSCANNER_FAIL = 2333;
    public static final int CLOSE_FINGERSCANNER_SUCCESS = 2334;

    public static final int BIONE_EXIT_SUCCESS = 2336;
    public static final int BIONE_EXIT_FAIL = 2335;

    public static final int IDCARD_EXTRACTFEATURE_ERROR = 33331;
    public static final int IDCARD_MAKETEMPLATE_ERROR = 33332;
    public static final int IDCARD_IDCARDVERIFY_ERROR = 33333;
    public static final int IDCARD_IDCARDVERIFY_SUCCESS = 3333;


    private ParseSFZAPI parseAPI;


    private Handler mWorkerThreadHandler;

    private OnReadSFZListener onReadSFZListener;

    private OnReadModuleListener onReadModuleListener;

    private OnReadCardIDListener onReadCardIDListener;

    private Context mContext;

    private int device = cw.Device_None;
    private int readCount=1;

    public void setReadCount(int readCount) {
        this.readCount = readCount;
    }

    /**
     * 构造方法
     *
     * @param looper
     * @param context 上下文
     */
    public AsyncParseSFZ(Looper looper, Context context) {
        mContext = context;
        parseAPI = new ParseSFZAPI(context);

        mWorkerThreadHandler = createHandler(looper);

    }


    /**
     * 该方法废弃
     *
     * @param onReadModuleListener
     */
    @Deprecated
    public void setOnReadModuleListener(OnReadModuleListener onReadModuleListener) {
        this.onReadModuleListener = onReadModuleListener;
    }

    /**
     * 读取身份证监听函数
     *
     * @param onReadSFZListener
     */
    public void setOnReadSFZListener(OnReadSFZListener onReadSFZListener) {
        this.onReadSFZListener = onReadSFZListener;
    }

    /**
     * 读取身份证id监听函数
     *
     * @param onReadCardIDListener
     */
    @Deprecated
    public void setOnReadCardIDListener(OnReadCardIDListener onReadCardIDListener) {
        this.onReadCardIDListener = onReadCardIDListener;
    }


    /**
     * 读取身份证模块接口
     */
    @Deprecated
    public interface OnReadModuleListener {
        void onReadSuccess(String module);

        void onReadFail(int confirmationCode);
    }

    /**
     * 读取身份证监听接口
     */
    public interface OnReadSFZListener {

        /**
         * 读取身份证成功
         *
         * @param people
         */
        void onReadSuccess(ParseSFZAPI.People people);

        /**
         * 读取身份证失败
         *
         * @param confirmationCode
         */
        void onReadFail(int confirmationCode);
    }

    /**
     * 读取身份证id接口
     */
    public interface OnReadCardIDListener {

        /**
         * 读取身份证id成功
         *
         * @param id
         */
        void onReadSuccess(String id);

        /**
         * 读取身份证id失败
         */
        void onReadFail();
    }


    protected Handler createHandler(Looper looper) {
        return new WorkerHandler(looper);
    }

    protected class WorkerHandler extends Handler {

        public WorkerHandler(Looper looper) {
            super(looper);
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case READ_SFZ:
                    ParseSFZAPI.Result resultSFZ = null;

                    //parseAPI.readIDCard(device);

                    /*switch (cw.getAndroidVersion()) {
                        case cw.deviceSysVersion.O:

                            resultSFZ = parseAPI.read(msg.arg1);
                            break;
                        case cw.deviceSysVersion.U:
                            Log.i(TAG, "---U3_640---");
                            resultSFZ = parseAPI.readSFZ(3);
                            break;
                    }*/

                    switch (cw.getDeviceModel()){

                        case cw.Device_A370_CW20:
                            resultSFZ = parseAPI.readSFZ(readCount);

                            break;

                        case cw.Device_A370_M4G5:
                            resultSFZ = parseAPI.read(msg.arg1);

                            break;
                        case cw.Device_CFON640:
                            resultSFZ = parseAPI.read(msg.arg1);

                            break;
                        case cw.Device_U3:
                            resultSFZ = parseAPI.readSFZ(readCount);
                            break;
                        case cw.Device_U8:
                            resultSFZ = parseAPI.readSFZ(readCount);
                            break;
                    }


                    if (resultSFZ.data != null) {
                        //Toast.makeText(mContext, DataUtils.toHexString(resultSFZ.data), Toast.LENGTH_SHORT).show();
                    }
                    if (resultSFZ.confirmationCode == ParseSFZAPI.Result.SUCCESS) {
                        AsyncParseSFZ.this.obtainMessage(FIND_CARD_SUCCESS, resultSFZ.resultInfo).sendToTarget();
                    } else {
                        AsyncParseSFZ.this.obtainMessage(FIND_CARD_FAIL, resultSFZ.confirmationCode, -1).sendToTarget();
                    }
                    break;

                case READ_IDCard:

                    ParseSFZAPI.Result result = null;

                    result = parseAPI.readIDCard(device);

                    if (result == null) {
                        Log.e(TAG, "---------------result is null!!-------------------");
                        Toast.makeText(mContext, "IDcard Result is null ! 请检查设备型号和打开串口是否一致!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (result.data != null) {
                        //Toast.makeText(mContext, DataUtils.toHexString(resultSFZ.data), Toast.LENGTH_SHORT).show();
                    }
                    if (result.confirmationCode == ParseSFZAPI.Result.SUCCESS) {
                        AsyncParseSFZ.this.obtainMessage(FIND_CARD_SUCCESS, result.resultInfo).sendToTarget();
                    } else {
                        AsyncParseSFZ.this.obtainMessage(FIND_CARD_FAIL, result.confirmationCode, -1).sendToTarget();
                    }

                    break;
                /*case READ_MODULE:
                    ParseSFZAPI.Result resultModule = parseAPI.readModule();
                    Log.i("whw", "module=" + resultModule.resultInfo);
                    if (resultModule.confirmationCode == ParseSFZAPI.Result.SUCCESS) {
                        AsyncParseSFZ.this.obtainMessage(FIND_MODULE_SUCCESS, resultModule.resultInfo).sendToTarget();
                    } else {
                        AsyncParseSFZ.this.obtainMessage(FIND_MODULE_FAIL, resultModule.confirmationCode, -1)
                                .sendToTarget();
                    }
                    break;
                case READ_CARD_ID:


                    switch (CoreWise.getAndroidVersion()) {
                        case CoreWise.deviceSysVersion.O:
                            AsyncParseSFZ.this.obtainMessage(READ_CARD_ID, parseAPI.readCardID()).sendToTarget();
                            break;
                        case CoreWise.deviceSysVersion.U:
                            AsyncParseSFZ.this.obtainMessage(READ_CARD_ID, parseAPI.readCardID_U3()).sendToTarget();
                            break;
                    }

                    break;*/
                default:
                    break;
            }
        }
    }

    public void openIDCardSerialPort(int type) {
        device = type;
        if (IDCardSerialPortManager.getInstance().isOpen()) {
            Log.i(TAG, "身份证串口已经打开！！！！！");
            return;
        }
        if (IDCardSerialPortManager.getInstance().openSerialPort(type)) {
            Log.i(TAG, "打开身份证串口成功！！！！！");
        }
    }

    public void closeIDCardSerialPort(int type) {
        //SerialPortManager.getInstance().closeSerialPort(type);
        IDCardSerialPortManager.getInstance().closeSerialPort(type);
        Log.i(TAG, "关闭身份证串口成功！！！！！");

    }

    /**
     * 在串口管理类分化后，该方法被废弃
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Deprecated
    public void openIDCardSerialPort() {
        if (!IDCardSerialPortManager.getInstance().isOpen() && !IDCardSerialPortManager.getInstance().openSerialPort(cw.getDeviceModel())) {
            Log.i(TAG, "打开身份证串口成功！！！！！");
        }

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Deprecated
    public void closeIDCardSerialPort() {
        IDCardSerialPortManager.getInstance().closeSerialPort(cw.getDeviceModel());
        Log.i(TAG, "关闭身份证串口成功！！！！！");
    }

    /**
     * 根据类型读二三代证
     *
     * @param cardType
     */
    @Deprecated
    public void readSFZ(int cardType) {
        mWorkerThreadHandler.obtainMessage(READ_SFZ, cardType, -1).sendToTarget();
    }

    /**
     * 根据类型读二三代证
     */
    public void readSFZ() {

        if (device == cw.Device_None) {
            readSFZ(ParseSFZAPI.THIRD_GENERATION_CARD);
        } else {
            mWorkerThreadHandler.obtainMessage(READ_IDCard, ParseSFZAPI.THIRD_GENERATION_CARD, -1).sendToTarget();
        }
    }




    /**
     * 读取模块号
     */
    @Deprecated
    public void readModuleNum() {
        mWorkerThreadHandler.obtainMessage(READ_MODULE).sendToTarget();
    }

    /**
     * 读身份证id
     */
    @Deprecated
    public void readCardID() {
        mWorkerThreadHandler.obtainMessage(READ_CARD_ID).sendToTarget();
    }


    /**
     * Handle消息处理
     *
     * @param msg
     */
    @Override
    public void handleMessage(Message msg) {
        super.handleMessage(msg);

        switch (msg.what) {
            case FIND_CARD_SUCCESS:
                if (onReadSFZListener != null) {
                    onReadSFZListener.onReadSuccess((ParseSFZAPI.People) msg.obj);
                }
                break;

            case FIND_CARD_FAIL:
                if (onReadSFZListener != null) {
                    onReadSFZListener.onReadFail(msg.arg1);
                }
                break;

            case FIND_MODULE_SUCCESS:
                if (onReadModuleListener != null) {
                    onReadModuleListener.onReadSuccess((String) msg.obj);
                }
                break;

            case FIND_MODULE_FAIL:
                if (onReadModuleListener != null) {
                    onReadModuleListener.onReadFail(msg.arg1);
                }
                break;

            case READ_CARD_ID:
                if (onReadCardIDListener != null) {
                    String id = (String) msg.obj;
                    if (!TextUtils.isEmpty(id)) {
                        onReadCardIDListener.onReadSuccess(id);
                    } else {
                        onReadCardIDListener.onReadFail();
                    }
                }
                break;

            default:
                break;
        }
    }

}