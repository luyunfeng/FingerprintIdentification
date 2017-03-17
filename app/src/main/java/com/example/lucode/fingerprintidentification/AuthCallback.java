package com.example.lucode.fingerprintidentification;

import android.os.Handler;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;

/**
 * Created by lucode on 2017/3/16.
 */
// 实现类
public class AuthCallback extends FingerprintManagerCompat.AuthenticationCallback {
    private Handler handler = null;

    public AuthCallback(Handler handler) {
        super();
        this.handler = handler;
    }

    @Override
    public void onAuthenticationError(int errMsgId, CharSequence errString) {
        super.onAuthenticationError(errMsgId, errString);
        System.out.println("异常!----------");
        if (handler != null) {
            handler.obtainMessage(MainActivity.MSG_AUTH_ERROR, errMsgId, 0).sendToTarget();
        }
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
        super.onAuthenticationHelp(helpMsgId, helpString);

        if (handler != null) {
            handler.obtainMessage(MainActivity.MSG_AUTH_HELP, helpMsgId, 0).sendToTarget();
        }
    }

    @Override
    public void onAuthenticationSucceeded(FingerprintManagerCompat.AuthenticationResult result) {
        super.onAuthenticationSucceeded(result);
        System.out.println("成功!----------");
        if (handler != null) {
            handler.obtainMessage(MainActivity.MSG_AUTH_SUCCESS).sendToTarget();
        }
    }

    @Override
    public void onAuthenticationFailed() {
        super.onAuthenticationFailed();
        System.out.println("失败!----------");
        if (handler != null) {
            handler.obtainMessage(MainActivity.MSG_AUTH_FAILED).sendToTarget();
        }
    }

}
