package com.cw.localsfz;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcB;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.cw.localsfz.bean.PeopleBean;
import com.cw.serialportsdk.utils.DataUtils;
import com.ivsign.android.IDCReader.IDCReaderSDK;
import com.ivsign.android.IDCReader.SfzFileManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import android_serialport_api.SerialPortActivity;

/**
 * 作者：李阳
 * 时间：2019/3/13
 * 描述：源代码不小心被删了，这是根据编译好的aar反编译的
 */
public abstract class NFCReadLocalIDCardActivity extends SerialPortActivity {

    private static final String TAG = Constants.RootTAG + "NFCReadIDCard";
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] mWriteTagFilters;
    private String[][] mTechLists;
    private NfcB mfc = null;
    private volatile int mCurrentSize = 0;
    byte[] sfzRaw;
    private long startMillis;
    private volatile boolean isReadIDCardSuccess = false;
    private String sfzBmpPath = null;

    public NFCReadLocalIDCardActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "----------------NFCReadLocalIDCardActivity-----onCreate------------------------");
        this.initNFC();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "----------------NFCReadLocalIDCardActivity-----onResume------------------------");
        this.nfcAdapter.enableForegroundDispatch(this, this.pendingIntent, this.mWriteTagFilters, this.mTechLists);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "----------------NFCReadLocalIDCardActivity-----onPause------------------------");
        this.nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "----------------NFCReadLocalIDCardActivity-----onDestroy------------------------");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Log.e(TAG, "----------------NFCReadLocalIDCardActivity-----onNewIntent------------------------");
        if ("android.nfc.action.TECH_DISCOVERED".equals(intent.getAction())) {
            this.startMillis = System.currentTimeMillis();
            this.mCurrentSize = 0;
            this.sfzRaw = new byte[2321];
            Log.i(TAG, "send read idcard success!");
            byte[] sfzId = intent.getByteArrayExtra("android.nfc.extra.ID");
            Log.d(TAG, DataUtils.toHexString(sfzId));
            this.onReadIDCardUID(sfzId);
            Tag tagFromIntent = (Tag) intent.getParcelableExtra("android.nfc.extra.TAG");
            String[] techList = tagFromIntent.getTechList();

            for (int i = 0; i < techList.length; ++i) {
                if (techList[i].equals(NfcB.class.getName())) {
                    this.mfc = NfcB.get(tagFromIntent);

                    try {
                        this.mfc.connect();
                        this.onReadIDCardStart();
                    } catch (IOException var7) {
                        var7.printStackTrace();
                        Log.e(TAG, "nfc connect is failure! " + var7.toString());
                        this.setOnReadIDCardFailure("nfc connect is failure! " + var7.toString());
                    }

                    if (this.mfc.isConnected()) {
                        Log.i(TAG, "身份证已连接");
                        writeCommand("D&C00040108".getBytes());
                    }
                }
            }
        }
    }

    @Override
    protected void onDataReceived(byte[] buffer, int size) {
        byte[] data = new byte[size];
        System.arraycopy(buffer, 0, data, 0, size);
        String dataCmd = DataUtils.toHexString(data);
        Log.i(TAG, "sam--->app: " + dataCmd);
        if (dataCmd.equals("03050000")) {
            this.writeCommand(Commands.cmds[2]);
        } else if (dataCmd.equals("011d")) {
            Log.e(TAG, "011d");
        } else if (dataCmd.equals("091d0000000000080108")) {
            this.writeCommand(Commands.cmds[3]);
        } else if (this.isSAMReturnCmd(data)) {
            this.nfcSendCmdToSFZ(data);
        } else if (!dataCmd.equals("00")) {
            if (dataCmd.startsWith("aaaaaa") && data.length == 10) {
                this.setOnReadIDCardFailure(dataCmd);
            } else {
                System.arraycopy(buffer, 0, this.sfzRaw, this.mCurrentSize, size);
                this.mCurrentSize += size;
                Log.i(TAG, "length = " + size + "  mCurrentSize = " + this.mCurrentSize);
                if (this.mCurrentSize == 1295 || this.mCurrentSize == 1297 || this.mCurrentSize == 2321) {
                    byte[] sfzRaw2 = new byte[this.mCurrentSize];
                    System.arraycopy(this.sfzRaw, 0, sfzRaw2, 0, this.mCurrentSize);
                    final PeopleBean peopleBean = this.decodeSFZInfos(sfzRaw2);
                    final long readTime = System.currentTimeMillis() - this.startMillis;
                    Log.i(TAG, "读卡时间: " + readTime + " ms");
                    this.isReadIDCardSuccess = true;
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            NFCReadLocalIDCardActivity.this.onReadIDCardSuccess(peopleBean, readTime);
                        }
                    });

                    try {
                        this.mfc.close();
                    } catch (IOException var10) {
                        var10.printStackTrace();
                        Log.e(TAG, var10.toString());
                        this.setOnReadIDCardFailure(var10.toString());
                    }
                }
            }
        }

    }

    @Override
    protected void onErrorReceived(String error) {
        this.setOnReadIDCardFailure(error);
    }

    private void initNFC() {
        this.nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (this.nfcAdapter == null) {
            Toast.makeText(this, "当前设备不支持NFC功能", Toast.LENGTH_SHORT).show();
        } else if (!this.nfcAdapter.isEnabled()) {
            Toast.makeText(this, "NFC功能未打开，请先开启后重试！", Toast.LENGTH_SHORT).show();
        }

        this.pendingIntent = PendingIntent.getActivity(this, 0, (new Intent(this, this.getClass())).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        IntentFilter ndef = new IntentFilter("android.nfc.action.TECH_DISCOVERED");
        ndef.addCategory("*/*");
        this.mWriteTagFilters = new IntentFilter[]{ndef};
        this.mTechLists = new String[][]{{NfcB.class.getName()}};
    }

    private String decodeNation(int code) {
        String[] nation = new String[]{"汉", "蒙古", "回", "藏", "维吾尔", "苗", "彝", "壮", "布依", "朝鲜", "满", "侗", "瑶", "白", "土家", "哈尼", "哈萨克", "傣", "黎", "傈僳", "佤", "畲", "高山", "拉祜", "水", "东乡", "纳西", "景颇", "柯尔克孜", "土", "达斡尔", "仫佬", "羌", "布朗", "撒拉", "毛南", "仡佬", "锡伯", "阿昌", "普米", "塔吉克", "怒", "乌孜别克", "俄罗斯", "鄂温克", "德昂", "保安", "裕固", "京", "塔塔尔", "独龙", "鄂伦春", "赫哲", "门巴", "珞巴", "基诺", "其他", "外国血统中国籍人士"};
        return nation[code - 1];
    }

    private byte[] decodePhoto(byte[] raw, String src) {
        Log.i(TAG, "decodePhoto Raw len: " + raw.length);
        if (src == null) {
            src = Environment.getExternalStorageDirectory().getAbsolutePath() + "/sfzPic";
            Log.i(TAG, "decode sfz image is in /sdcard/sfzpic/");
        } else {
            Log.i(TAG, "decode sfz image is " + src);
        }

        SfzFileManager sfzFileManager = new SfzFileManager(src);
        byte[] sfzRaw = new byte[1295];
        if (sfzFileManager.initDB(this, R.raw.base, R.raw.license)) {
            int ret = IDCReaderSDK.Init();
            if (0 == ret) {
                if (raw.length == 1297) {
                    System.arraycopy(raw, 0, sfzRaw, 0, 13);
                    System.arraycopy(raw, 15, sfzRaw, 13, 1281);
                    ret = IDCReaderSDK.unpack(sfzRaw);
                } else {
                    ret = IDCReaderSDK.unpack(raw);
                }

                if (1 == ret) {
                    byte[] imageRaw = IDCReaderSDK.getPhoto();
                    return imageRaw;
                }
            }
        }

        return null;
    }

    private PeopleBean decodeSFZInfos(byte[] sfzRaw) {
        Log.e(TAG, "length: " + sfzRaw.length + "\nRawData: " + DataUtils.toHexString(sfzRaw));
        PeopleBean peopleBean = null;
        byte[] nameRaw = new byte[30];
        byte[] sexRaw = new byte[2];
        byte[] nationalityRaw = new byte[4];
        byte[] birthdayRaw = new byte[16];
        byte[] addressRaw = new byte[70];
        byte[] idNumberRaw = new byte[36];
        byte[] departmentRaw = new byte[30];
        byte[] startDateRaw = new byte[16];
        byte[] endDateRaw = new byte[16];
        byte[] photoRaw = new byte[1024];
        byte[] fingerRaw = new byte[1024];
        //int headRawLen = true;
        byte headRawLen;
        if (sfzRaw.length == 1295) {
            headRawLen = 14;
        } else {
            headRawLen = 16;
        }

        System.arraycopy(sfzRaw, headRawLen, nameRaw, 0, nameRaw.length);
        System.arraycopy(sfzRaw, headRawLen + nameRaw.length, sexRaw, 0, sexRaw.length);
        System.arraycopy(sfzRaw, headRawLen + nameRaw.length + sexRaw.length, nationalityRaw, 0, nationalityRaw.length);
        System.arraycopy(sfzRaw, headRawLen + nameRaw.length + sexRaw.length + nationalityRaw.length, birthdayRaw, 0, birthdayRaw.length);
        System.arraycopy(sfzRaw, headRawLen + nameRaw.length + sexRaw.length + nationalityRaw.length + birthdayRaw.length, addressRaw, 0, addressRaw.length);
        System.arraycopy(sfzRaw, headRawLen + nameRaw.length + sexRaw.length + nationalityRaw.length + birthdayRaw.length + addressRaw.length, idNumberRaw, 0, idNumberRaw.length);
        System.arraycopy(sfzRaw, headRawLen + nameRaw.length + sexRaw.length + nationalityRaw.length + birthdayRaw.length + addressRaw.length + idNumberRaw.length, departmentRaw, 0, departmentRaw.length);
        System.arraycopy(sfzRaw, headRawLen + nameRaw.length + sexRaw.length + nationalityRaw.length + birthdayRaw.length + addressRaw.length + idNumberRaw.length + departmentRaw.length, startDateRaw, 0, startDateRaw.length);
        System.arraycopy(sfzRaw, headRawLen + nameRaw.length + sexRaw.length + nationalityRaw.length + birthdayRaw.length + addressRaw.length + idNumberRaw.length + departmentRaw.length + startDateRaw.length, endDateRaw, 0, endDateRaw.length);
        System.arraycopy(sfzRaw, headRawLen + nameRaw.length + sexRaw.length + nationalityRaw.length + birthdayRaw.length + addressRaw.length + idNumberRaw.length + departmentRaw.length + startDateRaw.length + endDateRaw.length, photoRaw, 0, photoRaw.length);
        if (sfzRaw.length == 2321) {
            System.arraycopy(sfzRaw, headRawLen + nameRaw.length + sexRaw.length + nationalityRaw.length + birthdayRaw.length + addressRaw.length + idNumberRaw.length + departmentRaw.length + startDateRaw.length + endDateRaw.length + photoRaw.length, fingerRaw, 0, fingerRaw.length);
        }

        try {
            String name = (new String(nameRaw, "UTF-16LE")).trim();
            String sex = (new String(sexRaw, "UTF-16LE")).trim();
            if (sex.equals("1")) {
                sex = "男";
            } else {
                sex = "女";
            }

            String nationality = (new String(nationalityRaw, "UTF-16LE")).trim();
            nationality = this.decodeNation(Integer.parseInt(nationality));
            String birthday = (new String(birthdayRaw, "UTF-16LE")).trim();
            String year = birthday.substring(0, 4);
            String month = birthday.substring(4, 6);
            String daya = birthday.substring(6, 8);
            String address = (new String(addressRaw, "UTF-16LE")).trim();
            String idNumber = (new String(idNumberRaw, "UTF-16LE")).trim();
            String department = (new String(departmentRaw, "UTF-16LE")).trim();
            String startDate = (new String(startDateRaw, "UTF-16LE")).trim();
            String endDate = (new String(endDateRaw, "UTF-16LE")).trim();
            photoRaw = this.decodePhoto(sfzRaw, this.sfzBmpPath);
            if (sfzRaw.length == 2321) {
                peopleBean = new PeopleBean(name, sex, nationality, birthday, address, idNumber, department, startDate, endDate, photoRaw, fingerRaw);
            } else {
                peopleBean = new PeopleBean(name, sex, nationality, birthday, address, idNumber, department, startDate, endDate, photoRaw, null);
            }

            Log.i(TAG, peopleBean.toString());
        } catch (UnsupportedEncodingException var27) {
            var27.printStackTrace();
        }

        return peopleBean;
    }

    private void nfcSendCmdToSFZ(byte[] data) {
        if (this.mfc == null) {
            Log.e(TAG, "mfc is null !");
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    NFCReadLocalIDCardActivity.this.setOnReadIDCardFailure("mfc is null !");
                }
            });
        } else {
            if (this.mfc.isConnected()) {
                try {
                    byte[] receiveData = this.mfc.transceive(this.getRealCmd(data));
                    this.writeCommand(this.getFullCmd(receiveData));
                } catch (final IOException var3) {
                    var3.printStackTrace();
                    Log.e(TAG, var3.toString());
                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            NFCReadLocalIDCardActivity.this.setOnReadIDCardFailure(var3.toString());
                        }
                    });
                }
            }

        }
    }

    private byte[] getFullCmd(byte[] data) {
        Log.i(TAG, "sfz--->app: " + DataUtils.toHexString(data));
        int len = data.length;
        byte[] cmd = new byte[len + 1];
        cmd[0] = (byte) len;
        System.arraycopy(data, 0, cmd, 1, len);
        return cmd;
    }

    private byte[] getRealCmd(byte[] data) {
        int length = data.length;
        byte[] cmd = new byte[length - 1];
        System.arraycopy(data, 1, cmd, 0, length - 1);
        Log.i(TAG, "app--->sfz: " + DataUtils.toHexString(cmd));
        return cmd;
    }

    private boolean isSAMReturnCmd(byte[] dataCmd) {
        int len = dataCmd.length;
        return dataCmd[0] == len - 1;
    }

    private void setOnReadIDCardFailure(final String errorCode) {
        if (this.mfc == null) {
            Log.e(TAG, "mfc is null !");
        } else {
            if (this.mfc.isConnected()) {
                try {
                    this.mfc.close();
                } catch (IOException var3) {
                    var3.printStackTrace();
                    Log.e(TAG, var3.toString());
                }
            }

            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onReadIDCardFailure(errorCode);
                }
            });
        }
    }

    protected abstract void onReadIDCardStart();

    protected abstract void onReadIDCardSuccess(PeopleBean peopleBean, long readtime);

    protected abstract void onReadIDCardFailure(String error);

    protected abstract void onReadIDCardUID(byte[] uid);

    public void setIDCardBmpStorePath(String sfzBmpPath) {
        this.sfzBmpPath = sfzBmpPath;
    }

/*    public String getSDKVersion() {
        return BuildConfig.VERSION_NAME;
    }*/

}
