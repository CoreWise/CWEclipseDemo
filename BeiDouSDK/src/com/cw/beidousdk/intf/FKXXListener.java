package com.cw.beidousdk.intf;

/**
 * 反馈信息
 */
public interface FKXXListener {
	void FKXX(int mark, String cmd);
	//用于测试
	void testData(byte[] data);
}