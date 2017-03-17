package com.example.lucode.fingerprintidentification;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private FingerprintManagerCompat fingerprintManager = null;

    KeyguardManager keyguardManager = null;
    private TextView mResultInfo = null;
    private Button mCancelBtn = null;
    private Button mStartBtn = null;
    private AuthCallback authCallback = null;
    //private MyAuthCallback myAuthCallback = null;
    private CancellationSignal cancellationSignal = null;

    private Handler handler = null;
    public static final int MSG_AUTH_SUCCESS = 100;
    public static final int MSG_AUTH_FAILED = 101;
    public static final int MSG_AUTH_ERROR = 102;
    public static final int MSG_AUTH_HELP = 103;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 去掉标题
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);


        setContentView(R.layout.activity_main);


        mResultInfo = (TextView) this.findViewById(R.id.fingerprint_status);
        mCancelBtn = (Button) this.findViewById(R.id.cancel_button);
        mStartBtn = (Button) this.findViewById(R.id.start_button);
        // 初始化 按钮事件
        init();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {

                super.handleMessage(msg);
                Log.d(TAG, "msg: " + msg.what + " ,arg1: " + msg.arg1);
                switch (msg.what) {
                    case MSG_AUTH_SUCCESS:
                        setResultInfo(R.string.fingerprint_success);
                        mCancelBtn.setEnabled(false);
                        mStartBtn.setEnabled(true);
                        cancellationSignal = null;
                        break;
                    case MSG_AUTH_FAILED:
                        setResultInfo(R.string.fingerprint_not_recognized);
                        mCancelBtn.setEnabled(false);
                        mStartBtn.setEnabled(true);
                        cancellationSignal = null;
                        break;
                    case MSG_AUTH_ERROR:
                        handleErrorCode(msg.arg1);
                        break;
                    case MSG_AUTH_HELP:
                        handleHelpCode(msg.arg1);
                        break;
                }
            }
        };


        /**
         *1.AndroidManifest.xml中添加权限;
         *  add jurisdiction in AndroidManifest.xml;
         */


        /**
         * 2.获得FingerprintManager对象引用
         *  通过V4支持包获得兼容的对象引用，这是google推行的做法(目的是为了让6.0以上版本也能使用指纹识别)；
         *  init fingerprint.  get FingerprintManager obj
         */
        fingerprintManager = FingerprintManagerCompat.from(this);
        /*
         FingerprintManager fingerprintManager =this.getSystemService(FINGERPRINT_SERVICE);
         也可以采用这样方式来获取,但是 API 文档写了.这个只能在6.0以上版本玩.
         */


        /*
        *  3.运行条件检查
        *      3.1 系统版本,在 android 6.0 以上版本(用了v4包得到FingerprintManager对象,变成非必要条件)
        *         It must run API level 23 or higher
        *      3.2 硬件条件
        *      3.3 系统是否有指纹
        *      3.4 当前设备是不是处于安全保护中的,说白了就是,是否需要锁屏密码(没写,待完善...)
        *
        * */

        // 获取当前安全保护信息
        keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        check();


    }

    // 初始化按钮和按钮事件
    public void init(){


        // 先设置 取消按钮为
        mCancelBtn.setEnabled(false);
        mStartBtn.setEnabled(true);

        // 两个按钮监听事件
        // set button listeners
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // set button state
                mCancelBtn.setEnabled(false);
                mStartBtn.setEnabled(true);

                // cancel fingerprint auth here.
                cancellationSignal.cancel();
                cancellationSignal = null;
            }
        });
        // 开始 按钮的监听
        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // reset result info.
                mResultInfo.setText(R.string.fingerprint_hint);
                mResultInfo.setTextColor(getColor(R.color.hint_color));

                // start fingerprint auth here.
                /** 4.
                 * authenticate需要五个参数
                 *  authenticate(CryptoObject,         // 需要自定义一个类来得到这个对象
                 *                CancellationSignal,    // 是否取消 扫描 指纹,建议能取消(扫描耗电)
                 *                int,                   // 直接给0
                 *                AuthenticationCallback,  //指纹认证结果
                 *                Handler             // 通常设置为 null
                 *               )
                 *  一个注意点 上面的是谷歌官方文档中摘抄的,实际使用的是 v4包里面的方法,参数顺序不一样
                 * */
                try {

                    if (cancellationSignal == null) {
                        cancellationSignal = new CancellationSignal();
                    }

                     /* 4.1
                      * 自定义 CryptoObjectHelper类 ,
                      * 通过 buildCryptoObject()方法得到CryptoObject这个对象
                     * */
                    CryptoObjectHelper cryptoObjectHelper = new CryptoObjectHelper();
                    fingerprintManager.authenticate(cryptoObjectHelper.buildCryptoObject(), 0,
                            cancellationSignal, authCallback, null);
                    // 改变一下 按钮的状态
                    // set button state.
                    mStartBtn.setEnabled(false);
                    mCancelBtn.setEnabled(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Fingerprint init failed! Try again!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    // 检查
    public void check(){
        if (!fingerprintManager.isHardwareDetected()) {
            // 检查硬件 没有指纹传感器的话,把信息反馈给用户
            // no fingerprint sensor is detected, show dialog to tell user.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.no_sensor_dialog_title);
            builder.setMessage(R.string.no_sensor_dialog_message);
            builder.setIcon(android.R.drawable.stat_sys_warning);
            builder.setCancelable(false);
            builder.setNegativeButton(R.string.cancel_btn_dialog, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            // show this dialog.
            builder.create().show();
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            // 用户如果没有,录过指纹的话提醒用户,在系统上撸一下指纹
            // no fingerprint image has been enrolled.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.no_fingerprint_enrolled_dialog_title);
            builder.setMessage(R.string.no_fingerprint_enrolled_dialog_message);
            builder.setIcon(android.R.drawable.stat_sys_warning);
            builder.setCancelable(false);
            builder.setNegativeButton(R.string.cancel_btn_dialog, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            // show this dialog
            builder.create().show();
        } else if (!keyguardManager.isKeyguardSecure()) {
            System.out.println("你的设备处于非安全状态");
            // 如果是非安全状态,需要提醒一下用户
            // 待完善.....
        } else {
            try {
                // 如果以上都成功的 为第四步整备参数了
                //用户的指纹认证结果
                authCallback = new AuthCallback(handler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleHelpCode(int code) {
        switch (code) {
            case FingerprintManager.FINGERPRINT_ACQUIRED_GOOD:
                setResultInfo(R.string.AcquiredGood_warning);
                break;
            case FingerprintManager.FINGERPRINT_ACQUIRED_IMAGER_DIRTY:
                setResultInfo(R.string.AcquiredImageDirty_warning);
                break;
            case FingerprintManager.FINGERPRINT_ACQUIRED_INSUFFICIENT:
                setResultInfo(R.string.AcquiredInsufficient_warning);
                break;
            case FingerprintManager.FINGERPRINT_ACQUIRED_PARTIAL:
                setResultInfo(R.string.AcquiredPartial_warning);
                break;
            case FingerprintManager.FINGERPRINT_ACQUIRED_TOO_FAST:
                setResultInfo(R.string.AcquiredTooFast_warning);
                break;
            case FingerprintManager.FINGERPRINT_ACQUIRED_TOO_SLOW:
                setResultInfo(R.string.AcquiredToSlow_warning);
                break;
        }
    }

    private void handleErrorCode(int code) {
        switch (code) {
            case FingerprintManager.FINGERPRINT_ERROR_CANCELED:
                setResultInfo(R.string.ErrorCanceled_warning);
                break;
            case FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE:
                setResultInfo(R.string.ErrorHwUnavailable_warning);
                break;
            case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT:
                setResultInfo(R.string.ErrorLockout_warning);
                break;
            case FingerprintManager.FINGERPRINT_ERROR_NO_SPACE:
                setResultInfo(R.string.ErrorNoSpace_warning);
                break;
            case FingerprintManager.FINGERPRINT_ERROR_TIMEOUT:
                setResultInfo(R.string.ErrorTimeout_warning);
                break;
            case FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS:
                setResultInfo(R.string.ErrorUnableToProcess_warning);
                break;
        }
    }

    private void setResultInfo(int stringId) {
        if (mResultInfo != null) {
            if (stringId == R.string.fingerprint_success) {
                mResultInfo.setTextColor(getColor(R.color.success_color));
            } else {
                mResultInfo.setTextColor(getColor(R.color.warning_color));
            }
            System.out.println("认证消息:" + stringId);
            mResultInfo.setText(stringId);
        }
    }


    // 善后工作
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!mStartBtn.isEnabled() && cancellationSignal != null) {
            cancellationSignal.cancel();
        }
    }

}
