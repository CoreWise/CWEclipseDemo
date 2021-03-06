package com.cw.netnfcreadidcardlib;

import android.content.Context;

import com.corewise.idcardreadersdk.support.NativeSupport;
import com.cw.netnfcreadidcardlib.Interface.ReadListener;
import com.cw.netnfcreadidcardlib.Interface.RegisterListener;

/**
 * 时间：2019/3/12
 * 描述：
 */
public class RegisterAPI {


    private RegisterListener registerListener;

    private static RegisterAPI instance = null;

    public RegisterAPI(RegisterListener registerListener) {
        this.registerListener = registerListener;

    }


    public void testRegisterDeive(Context context) {
        int ret = NativeSupport.registDeviceForTry(context.getApplicationContext());
        if (ret == ReadListener.RET_REGIST_DEVICE_SUCCESS) {
            registerListener.registerSuccess();
        } else {
            registerListener.error(ret);
        }

    }

    public void registerDevice(Context context, String registerCode) {
        int ret = NativeSupport.registDevice(context.getApplicationContext(), registerCode);
        if (ret == ReadListener.RET_REGIST_DEVICE_SUCCESS) {
            registerListener.registerSuccess();
        } else {
            registerListener.error(ret);
        }
    }

}
