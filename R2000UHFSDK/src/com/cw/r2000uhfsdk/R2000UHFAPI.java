package com.cw.r2000uhfsdk;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;


import com.cw.r2000uhfsdk.base.CMD;
import com.cw.r2000uhfsdk.base.ERROR;
import com.cw.r2000uhfsdk.base.ReaderBase;
import com.cw.r2000uhfsdk.base.StringTool;
import com.cw.r2000uhfsdk.helper.ISO180006BOperateTagBuffer;
import com.cw.r2000uhfsdk.helper.InventoryBuffer;
import com.cw.r2000uhfsdk.helper.OperateTagBuffer;
import com.cw.r2000uhfsdk.helper.ReaderHelper;
import com.cw.r2000uhfsdk.helper.ReaderSetting;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android_serialport_api.SerialPort;


/**
 * 时间：2018/10/30
 * 描述：
 */
public class R2000UHFAPI {

    private static final String TAG = "CW"+"R2000UHFAPI";

    private final String GPIO_DEV_STM32 = "/sys/class/gpio_power/stm32power/enable";

    private final byte[] UP = {'1'};
    private final byte[] DOWN = {'0'};

    private static R2000UHFAPI Instance = null;

    private String SerialPath = "/dev/ttyHSL1";
    private int BaudRate = 115200;
    private SerialPort mSerialPort;

    private ReaderBase mReader;

    private ReaderHelper mReaderHelper;

    private LocalBroadcastManager lbm;

    private ReaderSetting m_curReaderSetting;
    private InventoryBuffer m_curInventoryBuffer;
    private OperateTagBuffer m_curOperateTagBuffer;
    private ISO180006BOperateTagBuffer m_curOperateTagISO18000Buffer;

    private boolean isAuto = false;

    private boolean isRunning=false;


    /**
     * 获取读卡器
     *
     * @return
     */
    public ReaderBase getReader() {
        return mReader;
    }

    /**
     * 获取读卡器帮助类
     *
     * @return
     */
    public ReaderHelper getReaderHelper() {
        return mReaderHelper;
    }

    /**
     * 获取当前模块设置
     *
     * @return
     */
    public ReaderSetting getCurReaderSetting() {
        return m_curReaderSetting;
    }

    /**
     * 获取当前盘点缓存
     *
     * @return
     */
    public InventoryBuffer getCurInventoryBuffer() {
        return m_curInventoryBuffer;
    }

    /**
     * 获取当前操控标签缓存
     *
     * @return
     */
    public OperateTagBuffer getCurOperateTagBuffer() {
        return m_curOperateTagBuffer;
    }

    /**
     * 获取当前操作的ISO18000缓存
     *
     * @return
     */
    public ISO180006BOperateTagBuffer getCurOperateTagISO18000Buffer() {
        return m_curOperateTagISO18000Buffer;
    }

    private R2000UHFAPI() {

    }


    /**
     * 获取超高频API单例
     *
     * @return
     */
    public static R2000UHFAPI getInstance() {
        if (Instance == null) {
            Instance = new R2000UHFAPI();
        }
        return Instance;
    }


    private void setUpGpioSTM32() throws IOException {
        FileOutputStream fw = new FileOutputStream(GPIO_DEV_STM32);
        fw.write(UP);
        fw.close();
    }

    private void setDownGpioSTM32() throws IOException {
        FileOutputStream fw = new FileOutputStream(GPIO_DEV_STM32);
        fw.write(DOWN);
        fw.close();
    }


    /**
     * 打开超高频设备
     *
     * @param context
     */
    public void open(Context context) {
        try {
            setUpGpioSTM32();

            ReaderHelper.setContext(context);
            mSerialPort = new SerialPort(new File(SerialPath), BaudRate, 0);
            try {
                mReaderHelper = ReaderHelper.getDefaultHelper();
                mReaderHelper.setReader(mSerialPort.getInputStream(), mSerialPort.getOutputStream());
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
            mReader = mReaderHelper.getReader();
            mReader.openUHF();
            m_curReaderSetting = mReaderHelper.getCurReaderSetting();
            m_curInventoryBuffer = mReaderHelper.getCurInventoryBuffer();
            m_curOperateTagBuffer = mReaderHelper.getCurOperateTagBuffer();
            m_curOperateTagISO18000Buffer = mReaderHelper.getCurOperateTagISO18000Buffer();

        } catch (SecurityException e) {
            Log.i(TAG, String.valueOf(R.string.error_security));
        } catch (IOException e) {
            Log.i(TAG, String.valueOf(R.string.error_unknown));

        } catch (InvalidParameterException e) {
            Log.i(TAG, String.valueOf(R.string.error_configuration));
        } catch (Exception e) {
            e.printStackTrace();
        }

        lbm = LocalBroadcastManager.getInstance(context);

        IntentFilter itent = new IntentFilter();
        itent.addAction(ReaderHelper.BROADCAST_WRITE_LOG); //当前操作信息与结果
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_READER_SETTING); //读写器设置
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_INVENTORY_REAL); //实时盘点标签
        itent.addAction(ReaderHelper.BROADCAST_REFRESH_OPERATE_TAG); //操控标签

        lbm.registerReceiver(mRecv, itent);
    }

    /**
     * 关闭超高频设备,并清空缓存
     */
    public void closeAndClearBuffer() {

        mReader.closeUHF();

        m_curInventoryBuffer.clearInventoryRealResult();
        m_curInventoryBuffer.clearInventoryPar();
        m_curInventoryBuffer.clearInventoryResult();
        m_curOperateTagBuffer.clearBuffer();
        try {
            setDownGpioSTM32();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (lbm != null) {
            lbm.unregisterReceiver(mRecv);
        }
    }

     /**
     * 关闭超高频设备
     */
    public void close() {

        mReader.closeUHF();

        try {
            setDownGpioSTM32();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (lbm != null) {
            lbm.unregisterReceiver(mRecv);
        }
    }


    //////////////////////////////接口定义///////////////////////////////////////
    private IOnCommonReceiver onCommonReceiver;
    private IOnRegionReceiver onRegionReceiver;
    private IOnInventoryRealReceiver onInventoryRealReceiver;
    private IOnTagOperation onTagOperation;

    public void setOnCommonReceiver(IOnCommonReceiver onCmdReceive) {
        this.onCommonReceiver = onCmdReceive;
    }

    /**
     * Region接口
     *
     * @param onRegionReceive
     */

    public void setOnRegionReceiver(IOnRegionReceiver onRegionReceive) {
        this.onRegionReceiver = onRegionReceive;
    }

    public void setOnInventoryRealReceiver(IOnInventoryRealReceiver onInventoryRealReceive) {
        this.onInventoryRealReceiver = onInventoryRealReceive;
    }

    public void setOnTagOperation(IOnTagOperation onTagOperation) {
        this.onTagOperation = onTagOperation;
    }

    /////////////////////////////Tag基本操作///////////////////////////////


    /**
     * 复位
     *
     * @return 成功 :0, 失败:-1
     */
    public int reset() {
        return mReader.reset(m_curReaderSetting.btReadId);
    }


    /**
     * 获取版本号
     *
     * @return 成功 :0, 失败:-1
     */
    public int getFirmwareVersion() {
        return mReader.getFirmwareVersion(m_curReaderSetting.btReadId);
    }


    /**
     * 获取当前工作功率
     *
     * @return 成功 :0, 失败:-1
     */
    public int getOutputPower() {
        return mReader.getOutputPower(m_curReaderSetting.btReadId);
    }

    /**
     * 设置输出功率
     *
     * @param mOutPower 0~33 dBm
     */
    public void setOutputPower(int mOutPower) {
        byte btOutputPower = 0x00;

        try {
            btOutputPower = (byte) mOutPower;
        } catch (Exception e) {
            ;
        }

        mReader.setOutputPower(m_curReaderSetting.btReadId, btOutputPower);
        m_curReaderSetting.btAryOutputPower = new byte[]{btOutputPower};

    }


    /**
     * 获取模块温度
     *
     * @return 成功 :0, 失败:-1
     */
    public int getReaderTemperature() {
        return mReader.getReaderTemperature(m_curReaderSetting.btReadId);
    }


    /**
     * 获取射频频谱
     *
     * @return 成功 :0, 失败:-1
     */
    public int getFrequencyRegion() {
        return mReader.getFrequencyRegion(m_curReaderSetting.btReadId);
    }

    /**
     * 默认频点
     *
     * @param btRegion    0x01: FCC 0x02: ETSI 0x03:CHN
     * @param btStartFreq 902.00MHz~928.00MHz
     * @param btEndFreq   902.00MHz~928.00MHz
     */
    public void setFrequencyRegion(byte btRegion, byte btStartFreq, byte btEndFreq) {

        mReader.setFrequencyRegion(m_curReaderSetting.btReadId, btRegion, btStartFreq, btEndFreq);

        m_curReaderSetting.btRegion = btRegion;

        m_curReaderSetting.btFrequencyStart = btStartFreq;

        m_curReaderSetting.btFrequencyEnd = btEndFreq;
    }

    /**
     * 自定义频点
     *
     * @param nStartFrequency
     * @param nFrequencyInterval
     * @param btChannelQuantity
     */
    public void setUserDefineFrequencyRegion(int nStartFrequency, int nFrequencyInterval, byte btChannelQuantity) {
        mReader.setUserDefineFrequency(m_curReaderSetting.btReadId, (byte) nFrequencyInterval, btChannelQuantity, nStartFrequency);
        m_curReaderSetting.btRegion = 4;
        m_curReaderSetting.nUserDefineStartFrequency = nStartFrequency;
        m_curReaderSetting.btUserDefineFrequencyInterval = (byte) nFrequencyInterval;
        m_curReaderSetting.btUserDefineChannelQuantity = btChannelQuantity;
    }

    /**
     * @return 成功 :0, 失败:-1
     */
    public int getWorkAntenna() {
        return mReader.getWorkAntenna(m_curReaderSetting.btReadId);
    }


    /**
     * 切换当前工作天线
     *
     * @param context 上下文
     * @param mPos    四个天线，0,1,2,3
     * @return 成功 :0, 串口失败:-1 天线设置失败：-10
     */
    @Deprecated
    public int setWorkAntenna(Context context, int mPos) {
        List<String> mAntennaList = new ArrayList<String>();

        String[] lists = context.getResources().getStringArray(R.array.antenna_list);
        mAntennaList.addAll(Arrays.asList(lists));

        byte btWorkAntenna = (byte) mPos;
        if (btWorkAntenna < 0 || btWorkAntenna > mAntennaList.size()) return -10;

        m_curReaderSetting.btWorkAntenna = btWorkAntenna;

        return mReader.setWorkAntenna(m_curReaderSetting.btReadId, btWorkAntenna);
    }


    /**
     * 获取射频通讯链路
     *
     * @return 成功 :0, 串口失败:-1
     */
    public int getRfLinkProfile() {
        return mReader.getRfLinkProfile(m_curReaderSetting.btReadId);
    }

    /**
     * 设置射频通讯链路
     *
     * @param btProfile 0xD0：配置0  Tari 25uS; FM0 40KHz
     *                  0xD1：配置1(推荐且为默认)  Tari 25uS; Miller 4 250KHz
     *                  0xD2: 配置2  Tari 25uS; Miller 4 300KHz
     *                  0Xd3: 配置3  Tari 6.25uS; FM0 400KHz
     */
    public void setRfLinkProfile(byte btProfile) {

        mReader.setRfLinkProfile(m_curReaderSetting.btReadId, btProfile);
        m_curReaderSetting.btRfLinkProfile = btProfile;
    }


    /////////////////实时盘点////////////////////////////////////////////


    /**
     * U8只有一条天线,默认每条命令的盘存1次
     */
    public void startInventoryReal() {
        startInventoryReal(0x00, "1");
    }

    /**
     * U8只有一条天线
     * 推荐使用方法
     *
     * @param strRepeat 每条命令的盘存次数
     */
    public void startInventoryReal(String strRepeat) {
        startInventoryReal(0x00, strRepeat);
    }


    /**
     * 自定义session参数盘存标签方法
     *
     * @param strRepeat 每次命令盘存的次数
     */
    public void startInventoryReal(String strRepeat, int mPos1, int mPos2) {
        startInventoryReal(0, strRepeat, true, mPos1, mPos2);
    }


    /**
     * 盘存标签通用方法
     *
     * @param lAntenna       天线数
     * @param strRepeat      每条命令的盘存次数
     * @param mCbRealSession
     * @param mPos1
     * @param mPos2
     */
    public void startInventoryReal(int lAntenna, String strRepeat, boolean mCbRealSession, int mPos1, int mPos2) {

        m_curInventoryBuffer.clearInventoryPar();

        m_curInventoryBuffer.lAntenna.add((byte) lAntenna);


        if (m_curInventoryBuffer.lAntenna.size() <= 0) {
            Log.i(TAG, String.valueOf(R.string.antenna_empty));
            return;
        }

        m_curInventoryBuffer.bLoopInventoryReal = true;
        m_curInventoryBuffer.btRepeat = 0;

        if (strRepeat == null || strRepeat.length() <= 0) {
            Log.i(TAG, String.valueOf(R.string.repeat_empty));
            return;
        }

        m_curInventoryBuffer.btRepeat = (byte) Integer.parseInt(strRepeat);

        if ((m_curInventoryBuffer.btRepeat & 0xFF) <= 0) {
            Log.i(TAG, String.valueOf(R.string.repeat_min));
            return;
        }

        if (mCbRealSession) {
            m_curInventoryBuffer.bLoopCustomizedSession = true;
            m_curInventoryBuffer.btSession = (byte) (mPos1 & 0xFF);
            m_curInventoryBuffer.btTarget = (byte) (mPos2 & 0xFF);
        } else {
            m_curInventoryBuffer.bLoopCustomizedSession = false;
        }

        m_curInventoryBuffer.clearInventoryRealResult();
        mReaderHelper.setInventoryFlag(true);

        mReaderHelper.clearInventoryTotal();

        //refreshText();

        byte btWorkAntenna = m_curInventoryBuffer.lAntenna.get(m_curInventoryBuffer.nIndexAntenna);
        if (btWorkAntenna < 0) {
            btWorkAntenna = 0;
        }

        mReader.setWorkAntenna(m_curReaderSetting.btReadId, btWorkAntenna);

        m_curReaderSetting.btWorkAntenna = btWorkAntenna;

        //mRefreshTime = new Date().getTime();

        mLoopHandler.removeCallbacks(mLoopRunnable);

        mLoopHandler.postDelayed(mLoopRunnable, 2000);

        mHandler.removeCallbacks(mRefreshRunnable);


        mHandler.postDelayed(mRefreshRunnable, 2000);

        isRunning=true;

    }

    /**
     * 盘存标签（实时模式）
     *
     * @param lAntenna  天线数
     * @param strRepeat 每次命令盘存的次数
     */
    public void startInventoryReal(int lAntenna, String strRepeat) {
        startInventoryReal(lAntenna, strRepeat, false, -1, -1);
    }


    /**
     * 温度功率处理
     */
    public void startInventoryReal(String strRepeat, int i) {
        startInventoryReal(0x00, strRepeat);
    }


    /**
     * 停止实时盘点
     */
    public void stopInventoryReal() {

        isRunning=false;

        m_curInventoryBuffer.clearInventoryPar();

        if (m_curInventoryBuffer.lAntenna.size() <= 0) {
            Log.i(TAG, String.valueOf(R.string.antenna_empty));
            return;
        }

        m_curInventoryBuffer.bLoopInventoryReal = true;
        m_curInventoryBuffer.btRepeat = 0;

        if ((m_curInventoryBuffer.btRepeat & 0xFF) <= 0) {

            Log.i(TAG, String.valueOf(R.string.repeat_min));

            return;
        }

        mReaderHelper.setInventoryFlag(false);
        m_curInventoryBuffer.bLoopInventoryReal = false;
        mLoopHandler.removeCallbacks(mLoopRunnable);
        mHandler.removeCallbacks(mRefreshRunnable);
        mHandler.removeCallbacksAndMessages(null);

        return;
    }


    ///////////////////////////////////////////////////////////////////////////

    /////////////////缓存盘点////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////


    /////////////////操作标签////////////////////////////////////////////

    /**
     * 获取当前选定的操作标签
     *
     * @return 成功 :0, 串口失败:-1
     */
    public int getAccessEpcMatch() {
        return mReader.getAccessEpcMatch(m_curReaderSetting.btReadId);
    }

    /**
     * 选择指定的标签
     *
     * @param mPos
     * @param mAccessList
     * @return 成功 :0, 串口失败:-1 未知错误：-9 取消epc匹配失败
     */
    public int setAccessEpcMatch(int mPos, List<String> mAccessList) {

        if (mPos <= 0) {
            int i = mReader.cancelAccessEpcMatch(m_curReaderSetting.btReadId);
            if (i == 0) {
                return i;
            } else {
                return -8;
            }
        } else {
            byte[] btAryEpc = null;

            try {
                String[] result = StringTool.stringToStringArray(mAccessList.get(mPos).toUpperCase(), 2);
                btAryEpc = StringTool.stringArrayToByteArray(result, result.length);
            } catch (Exception e) {
                Log.i(TAG, String.valueOf(R.string.param_unknown_error));
                return -9;
            }

            if (btAryEpc == null) {
                Log.i(TAG, String.valueOf(R.string.param_unknown_error));
                return -9;
            }
            return mReader.setAccessEpcMatch(m_curReaderSetting.btReadId, (byte) (btAryEpc.length & 0xFF), btAryEpc);
        }
    }


    /**
     * 读标签
     * 读标签需要输入三个参数：要读取的标签区域，起始地址和数据长度。注意，这里的起始地址和数据长度的单位都是WORD，也就是16 bit 的双字节。
     *
     * @param btMemBank     0x00:密码区  0x01:EPC区  0x02:TID区  0x03:USER区
     * @param btWordAdd     起始地址(word),  eg: 00
     * @param btWordCnt     数据长度(word),  eg: 4
     * @param btAryPassWord 密码(Hex), eg: 4
     * @return 成功 :0, 串口失败:-1, 起始地址格式错误:-2,密码格式错误:-3,数据长度格式错误:-4 数据长度格式错误2:-5
     */
    public int readTag(byte btMemBank, String btWordAdd, String btWordCnt, String btAryPassWord) {

        byte[] password;//密码
        byte wordCnt;//数据长度
        byte wordAdd; //起始地址

        try {
            wordAdd = (byte) Integer.parseInt(btWordAdd);

        } catch (Exception e) {
            Log.i(TAG, String.valueOf(R.string.param_start_addr_error));
            return -2;
        }

        try {
            String[] reslut = StringTool.stringToStringArray(btAryPassWord.toUpperCase(), 2);
            password = StringTool.stringArrayToByteArray(reslut, 4);

        } catch (Exception e) {
            Log.i(TAG, String.valueOf(R.string.param_password_error));
            return -3;
        }


        try {
            wordCnt = (byte) Integer.parseInt(btWordCnt);
        } catch (Exception e) {
            Log.i(TAG, String.valueOf(R.string.param_data_len_error));
            return -4;
        }

        if ((wordCnt & 0xFF) <= 0) {
            Log.i(TAG, String.valueOf(R.string.param_data_len_error));
            return -5;
        }

        m_curOperateTagBuffer.clearBuffer();
        return mReader.readTag(m_curReaderSetting.btReadId, btMemBank, wordAdd, wordCnt, password);

    }


    /**
     * 写标签
     *
     * @param btMemBank
     * @param btWordAdd
     * @param btWordCnt
     * @param btAryPassWord
     * @param btAryData
     * @return 成功 :0, 串口失败:-1，起始地址格式错误:-2,密码格式错误:-3,数据长度格式错误:-4 数据长度格式错误2:-5 写入数据格式错误：-6
     */
    public int writeTag(byte btMemBank, String btWordAdd, String btWordCnt, String btAryPassWord, String btAryData) {

        byte[] password;//密码
        byte wordCnt;//数据长度
        byte wordAdd; //起始地址
        byte[] data;//要写入的数据

        try {

            wordAdd = (byte) Integer.parseInt(btWordAdd);

        } catch (Exception e) {
            Log.i(TAG, String.valueOf(R.string.param_start_addr_error));
            return -2;
        }

        try {
            String[] reslut = StringTool.stringToStringArray(btAryPassWord.toUpperCase(), 2);
            password = StringTool.stringArrayToByteArray(reslut, 4);

        } catch (Exception e) {
            Log.i(TAG, String.valueOf(R.string.param_password_error));
            return -3;
        }


        try {
            wordCnt = (byte) Integer.parseInt(btWordCnt);
        } catch (Exception e) {
            Log.i(TAG, String.valueOf(R.string.param_data_len_error));
            return -4;
        }

        if ((wordCnt & 0xFF) <= 0) {
            Log.i(TAG, String.valueOf(R.string.param_data_len_error));
            return -5;
        }

        try {
            String[] dataResult = StringTool.stringToStringArray(btAryData, 2);
            data = StringTool.stringArrayToByteArray(dataResult, dataResult.length);
            wordCnt = (byte) ((dataResult.length / 2 + dataResult.length % 2) & 0xFF);
        } catch (Exception e) {
            Log.i(TAG, String.valueOf(R.string.param_data_error));

            return -6;
        }
        m_curOperateTagBuffer.clearBuffer();

        onTagOperation.writeTagResult(String.valueOf(wordCnt & 0xFF));

        return mReader.writeTag(m_curReaderSetting.btReadId, password, btMemBank, wordAdd, wordCnt, data);

    }


    /**
     * 销毁标签,灭活标签操作
     * 灭活标签必须提供灭活口令，并且灭活口令不能为00 00 00 00, 因此要销毁一张标签，首先要通过写标签命令，修改密码区灭活口令的内容。
     * <p>
     * 未测试
     *
     * @param btAryPassWord 密码
     * @return 成功 :0, 串口失败:-1 ,密码格式错误:-3
     */
    public int killTag(String btAryPassWord) {

        byte[] passWord = null;
        try {
            String[] reslut = StringTool.stringToStringArray(btAryPassWord.toUpperCase(), 2);
            passWord = StringTool.stringArrayToByteArray(reslut, 4);
        } catch (Exception e) {
            Log.i(TAG, String.valueOf(R.string.param_killpassword_error));
            return -3;
        }

        if (passWord == null || passWord.length < 4) {
            Log.i(TAG, String.valueOf(R.string.param_killpassword_error));
            return -3;
        }

        m_curOperateTagBuffer.clearBuffer();
        return mReader.killTag(m_curReaderSetting.btReadId, passWord);
    }


    /**
     * 锁定标签
     * 锁定标签必须提供访问密码才能进行。
     * <p>
     * 未测试
     *
     * @param btAryPassWord 锁定标签必须要密码
     * @param btMemBank     访问密码区: 0x04 销毁密码区: 0x05 EPC区：0x03 TID区：0x02  USER区：0x01
     * @param btLockType    开放:0x00  锁定:0x01 永久开放:0x02 永久锁定:0x03
     * @return 成功 :0, 失败:-1 密码格式错误:-3
     */
    public int lockTag(String btAryPassWord, byte btMemBank, byte btLockType) {

        byte[] password;//密码

        try {
            String[] reslut = StringTool.stringToStringArray(btAryPassWord.toUpperCase(), 2);
            password = StringTool.stringArrayToByteArray(reslut, 4);

        } catch (Exception e) {
            Log.i(TAG, String.valueOf(R.string.param_password_error));
            return -3;
        }

        if (password == null || password.length < 4) {
            Log.i(TAG, String.valueOf(R.string.param_lockpassword_error));
            return -3;
        }

        m_curOperateTagBuffer.clearBuffer();

        return mReader.lockTag(m_curReaderSetting.btReadId, password, btMemBank, btLockType);
    }


    ////////////////////////////////盘点异步处理///////////////////////////////////////////


    private Handler mHandler = new Handler();

    /**
     * 设置盘点时间间隔，默认2000ms
     *
     * @param delayMillis
     */
    public void setInventoryDelayMillis(long delayMillis) {
        this.delayMillis = delayMillis;
    }

    private long delayMillis = 2000;


    private Runnable mRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            onInventoryRealReceiver.inventoryRefresh(m_curInventoryBuffer);//盘点标签

            if (isRunning) {
                mHandler.postDelayed(this, delayMillis);
            }

        }
    };


    private Handler mLoopHandler = new Handler();

    private Runnable mLoopRunnable = new Runnable() {
        @Override
        public void run() {

            if (m_curInventoryBuffer.lAntenna.size() > 0) {
                byte btWorkAntenna = m_curInventoryBuffer.lAntenna.get(m_curInventoryBuffer.nIndexAntenna);
                if (btWorkAntenna < 0) {
                    btWorkAntenna = 0;
                }

                mReader.setWorkAntenna(m_curReaderSetting.btReadId, btWorkAntenna);
                mLoopHandler.postDelayed(this, delayMillis);
            }
        }
    };


    //////////////////////////////////////////////////////////////////////


    private final BroadcastReceiver mRecv = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_READER_SETTING)) {
                byte btCmd = intent.getByteExtra("cmd", (byte) 0x00);

                switch (btCmd) {
                    case CMD.GET_FIRMWARE_VERSION:
                        Log.i(TAG, "Version:  " + String.valueOf(m_curReaderSetting.btMajor & 0xFF) + "." + String.valueOf(m_curReaderSetting.btMinor & 0xFF));
                        onCommonReceiver.onReceiver(btCmd, String.valueOf(m_curReaderSetting.btMajor & 0xFF) + "." + String.valueOf(m_curReaderSetting.btMinor & 0xFF));
                        break;
                    case CMD.GET_OUTPUT_POWER:
                        if (m_curReaderSetting.btAryOutputPower != null) {
                            Log.i(TAG, "Power:  " + String.valueOf(m_curReaderSetting.btAryOutputPower[0] & 0xFF));
                            onCommonReceiver.onReceiver(btCmd, String.valueOf(m_curReaderSetting.btAryOutputPower[0] & 0xFF));
                        }

                        break;

                    case CMD.SET_OUTPUT_POWER:

                        if (m_curReaderSetting.btAryOutputPower != null) {
                            onCommonReceiver.onReceiver(btCmd, String.valueOf(m_curReaderSetting.btAryOutputPower[0] & 0xFF));
                        }

                        break;
                    case CMD.GET_READER_TEMPERATURE:

                        String strTemperature = "";
                        if (m_curReaderSetting.btPlusMinus == 0x0) {
                            strTemperature = "-" + String.valueOf(m_curReaderSetting.btTemperature & 0xFF) + "℃";
                        } else {
                            strTemperature = String.valueOf(m_curReaderSetting.btTemperature & 0xFF) + "℃";
                        }

                        Log.i(TAG, "Temperature:  " + strTemperature);

                        onCommonReceiver.onReceiver(btCmd, strTemperature);
                        //温度，功率控制

                        if (isAuto) {

                            if (Integer.parseInt(String.valueOf(m_curReaderSetting.btTemperature & 0xFF)) > 50) {

                                // setOutputPower(25);
                                Log.e("TEST", "我自动调节功率了-----暂停该功能开发!--20181119");

                            }

                        }


                        break;
                    case CMD.SET_FREQUENCY_REGION:

                        onRegionReceiver.onRegionReceiver(m_curReaderSetting.btRegion & 0xFF, m_curReaderSetting.btFrequencyStart & 0xFF, m_curReaderSetting.btFrequencyEnd & 0xFF);

                        break;
                    case CMD.GET_FREQUENCY_REGION:

                        onRegionReceiver.onRegionReceiver(m_curReaderSetting.btRegion & 0xFF, m_curReaderSetting.btFrequencyStart & 0xFF, m_curReaderSetting.btFrequencyEnd & 0xFF);

                        break;
                    case CMD.GET_WORK_ANTENNA:

                        onCommonReceiver.onReceiver(btCmd, m_curReaderSetting.btWorkAntenna);

                        break;
                    case CMD.SET_WORK_ANTENNA:
                        onCommonReceiver.onReceiver(btCmd, m_curReaderSetting.btWorkAntenna);

                        break;

                    case CMD.GET_RF_LINK_PROFILE:
                        onCommonReceiver.onReceiver(btCmd, m_curReaderSetting.btRfLinkProfile & 0xFF);

                        break;
                    case CMD.SET_RF_LINK_PROFILE:
                        onCommonReceiver.onReceiver(btCmd, m_curReaderSetting.btRfLinkProfile & 0xFF);

                        break;


                }

            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_INVENTORY_REAL)) {

                byte btCmd = intent.getByteExtra("cmd", (byte) 0x00);

                switch (btCmd) {

                    //盘存标签（实时模式）
                    case CMD.REAL_TIME_INVENTORY:
                        onInventoryRealReceiver.realTimeInventory();
                        break;
                    case CMD.CUSTOMIZED_SESSION_TARGET_INVENTORY:

                        mLoopHandler.removeCallbacks(mLoopRunnable);
                        mLoopHandler.postDelayed(mLoopRunnable, 2000);
                        onInventoryRealReceiver.customized_session_target_inventory(m_curInventoryBuffer);
                        break;
                    case ReaderHelper.INVENTORY_ERR:
                        onInventoryRealReceiver.inventoryErr();
                        break;
                    case ReaderHelper.INVENTORY_ERR_END:
                        onInventoryRealReceiver.inventoryErrEnd();
                        break;
                    case ReaderHelper.INVENTORY_END:
                        if (mReaderHelper.getInventoryFlag()) {
                            mLoopHandler.removeCallbacks(mLoopRunnable);
                            mLoopHandler.postDelayed(mLoopRunnable, 2000);
                        } else {
                            mLoopHandler.removeCallbacks(mLoopRunnable);
                        }
                        onInventoryRealReceiver.inventoryEnd(m_curInventoryBuffer);
                        //温度，功率控制

                        if (isAuto) {
                            getReaderTemperature();
                        }

                        break;
                }

            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_REFRESH_OPERATE_TAG)) {
                byte btCmd = intent.getByteExtra("cmd", (byte) 0x00);

                switch (btCmd) {
                    case CMD.GET_ACCESS_EPC_MATCH:
                        onTagOperation.getAccessEpcMatch(m_curOperateTagBuffer);
                        break;
                    case CMD.READ_TAG:
                        onTagOperation.readTagResult(m_curOperateTagBuffer);
                        break;
                    case CMD.WRITE_TAG:
                        //onTagOperation.writeTagResult();
                        break;
                    case CMD.LOCK_TAG:
                        onTagOperation.lockTagResult();
                        break;
                    case CMD.KILL_TAG:

                        onTagOperation.killTagResult();
                        break;
                }
            } else if (intent.getAction().equals(ReaderHelper.BROADCAST_WRITE_LOG)) {

                String log = intent.getStringExtra("log");
                int type = intent.getIntExtra("type", ERROR.SUCCESS);

                if (onRegionReceiver != null) {
                    onRegionReceiver.onLog(log, type);
                }

                if (onCommonReceiver != null) {
                    onCommonReceiver.onLog(log, type);
                }

                if (onInventoryRealReceiver != null) {
                    onInventoryRealReceiver.onLog(log, type);
                }

                if (onTagOperation != null) {
                    onTagOperation.onLog(log, type);
                }
            }
        }
    };


}
