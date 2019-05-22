package com.cw.beidousdk.intf;

/**
 * IC自检回调
 */
public interface ICJCListener {
	/**
	 * IC信息
	 * 
	 * @param broadcastID
	 *            通播ID
	 * @param userCharacteristic
	 *            用户特征
	 * @param serviceFrequency
	 *            服务频率 单位秒
	 * @param isEncryptionUser
	 *            是否是保密用户 true:保密用户; false:非保密用户
	 */
	void ICInfo(int broadcastID, String userCharacteristic, int serviceFrequency, boolean isEncryptionUser);
}