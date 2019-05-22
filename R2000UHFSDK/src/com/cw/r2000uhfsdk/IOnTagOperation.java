package com.cw.r2000uhfsdk;


import com.cw.r2000uhfsdk.helper.OperateTagBuffer;

/**
 * 作者：李阳
 * 时间：2018/11/14
 * 描述：
 */
public interface IOnTagOperation {


    /**
     * 获取当前选中的标签回调接口
     * @param m_curOperateTagBuffer
     */
    void getAccessEpcMatch(OperateTagBuffer m_curOperateTagBuffer);

    /**
     * 读标签的结果回调
     */
    void readTagResult(OperateTagBuffer m_curOperateTagBuffer);

    /**
     * 写标签的结果回调
     */
    void writeTagResult(String mDataLen);

    /**
     * 锁标签的结果回调
     */
    void lockTagResult();

    /**
     * 销毁标签的结果回调
     */
    void killTagResult();

    void onLog(String strLog, int type);

}
