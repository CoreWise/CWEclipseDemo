package com.cw.r2000uhfsdk;


import com.cw.r2000uhfsdk.helper.InventoryBuffer;

/**
 * 时间：2018/11/13
 * 描述：盘存标签，实时模式
 */
public interface IOnInventoryRealReceiver {

    void realTimeInventory();
    void customized_session_target_inventory(InventoryBuffer inventoryBuffer);
    void inventoryErr();
    void inventoryErrEnd();
    void inventoryEnd(InventoryBuffer inventoryBuffer);

    void inventoryRefresh(InventoryBuffer inventoryBuffer);

    void onLog(String strLog, int type);

}
