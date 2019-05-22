package com.cw.r2000uhfsdk;

/**
 * 作者：李阳
 * 时间：2018/11/13
 * 描述：
 */
public interface IOnRegionReceiver {

    /**
     * @param Region
     * @param FreqStart
     * @param FreqEnd
     */
    void onRegionReceiver(int Region, int FreqStart, int FreqEnd);

    void onLog(String strLog, int type);

}
