package com.cw.beidousdk.intf;

/**
 * 通信信息回调
 */
public interface TXXXListener {
	/**
	 * 通信信息
	 * 
	 * @param address
	 *            对方地址
	 * @param isChinese
	 *            true:汉字;false:代码
	 * @param msg
	 *            消息
	 */
	void TXXX(byte[] address, boolean isChinese, byte[] msg);
	
	/**
	 * 通信失败时候的数据
	 * @param data 数据
	 */
	void TXFail(byte[] data);
}