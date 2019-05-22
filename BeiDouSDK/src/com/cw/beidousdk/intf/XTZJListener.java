package com.cw.beidousdk.intf;

/**
 * IC状态监听
 */
public interface XTZJListener {
	/**
	 * IC状态
	 * 
	 * @param isICHandlerNormal
	 *            智能卡处理是否正常 true:正常; false:异常
	 * @param isIDNormal
	 *            ID号是否正常 true:正常; false:出错
	 * @param isCheckCodeCorrect
	 *            校验码是否正确 true:正确; false:错误
	 * @param isSerialNoNormal
	 *            序列号是否正常 true:正常; false:错误
	 * @param ManagementCardOrUserCard
	 *            管理卡还是用户卡 true:管理卡; false:用户卡
	 * @param isDataNormal
	 *            智能卡数据是否正常 true:正常; false:不完整
	 * @param isICNormal
	 *            智能卡是否正常 true:正常; false:缺损
	 */
	void ICStates(boolean isICHandlerNormal, boolean isIDNormal, boolean isCheckCodeCorrect, boolean isSerialNoNormal,
                  boolean ManagementCardOrUserCard, boolean isDataNormal, boolean isICNormal);

	/**
	 * 硬件状态
	 * 
	 * @param isAntennaNormal
	 *            天线是否连接 true:正常; false:天线未连接
	 * @param isChannelNormal
	 *            通道是否正常 true:正常; false:通道故障
	 * @param isMainBoardNormal
	 *            主板是否正常 true:正常; false:主板故障
	 */
	void HardwareStates(boolean isAntennaNormal, boolean isChannelNormal, boolean isMainBoardNormal);

	/**
	 * 电池电量
	 * 
	 * @param percent
	 *            剩余百分比
	 */
	void BatteryLevel(int percent);

	/**
	 * 入站状态
	 * 
	 * @param isSuppression
	 *            true:抑制; false:非抑制
	 * @param isSilence
	 *            true:静默; false:非静默
	 */
	void InboundStates(boolean isSuppression, boolean isSilence);
}