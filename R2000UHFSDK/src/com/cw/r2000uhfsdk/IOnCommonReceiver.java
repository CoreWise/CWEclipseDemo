package com.cw.r2000uhfsdk;

/**
 * 时间：2018/11/12
 * 描述：包括获取版本号、功率、温度
 */
public interface IOnCommonReceiver {
    void onReceiver(byte cmd, Object result);
    void onLog(String strLog, int type);
}
