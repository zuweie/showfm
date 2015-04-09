package com.geekfocus.zuweie.showfm;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVUser;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;

import java.util.Timer;

public class LoginActivity2 extends Activity implements MyLogin.OnUpdateLoginUserListener{

    private ImageButton mLoginbt;
    /*comment the facebook code, can not be use in china,Fuck!!
    LoginButton mFacebook_loginButton;
    */
    private TextView mLogin_status;
    private TextView mSkip;
    private SsoHandler mSsoHandler;
    private Oauth2AccessToken mAccessToken;
    private AuthInfo mAuthInfo;
    /* comment the facebook code, can not be use in china,Fuck!!
    //private AccessToken mfbAccessToken;
    */
    private Timer FetchSSoUserTimeout;

    boolean SSO_DONE = false;
    boolean AVO_DONE = false;

    public int login_type = 0;

    /*comment the facebook code, can not be use in china,Fuck!!
    CallbackManager mCallbackManager;
    */

    public static int L_FACEBOOK = 1;
    public static int L_WEIBO    = 2;

    public final static int LS_AUTHING = 1;
    public final static int LS_FETCHING_USER = 2;
    public final static int LS_FETCHED_USER_ERR = 4;
    public final static int LS_AUTH_CANCEL = 5;
    public final static int LS_AUTH_ERR    = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        /*comment the facebook code, can not be use in china,Fuck!!
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        */

        setContentView(R.layout.activity_login_activity2);
        MyLogin.getInstance().setContext(this);
        MyLogin.getInstance().readLoginStatus();
        // init weibo
        mLoginbt = (ImageButton) findViewById(R.id.weibo_login_bt);
        mLogin_status = (TextView) findViewById(R.id.login_status);

        mAuthInfo = new AuthInfo(LoginActivity2.this, Myfunc.getValidText(MyConstant.WEIBO_APP_KEY), MyConstant.WEIBO_REDIRECT_URL, MyConstant.WEIBO_SCOPE);
        mSsoHandler = new SsoHandler(LoginActivity2.this, mAuthInfo);
        MyLogin.getInstance().setListener(this);
        mLoginbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Oauth2AccessToken accessToken = AccessTokenkeeper.readAccessToken(LoginActivity2.this);
                if (accessToken != null && accessToken.isSessionValid()){
                    MyLogin.getInstance().fetchAVOSUser();
                    MyLogin.getInstance().fetchSSoUser();
                    updatedUi(LS_FETCHING_USER);
                }else{
                    mSsoHandler.authorizeWeb(new AuthListener());
                    updatedUi(LS_AUTHING);
                }

            }
        });
        // end init weibo

        // init facebook
        /*comment the facebook code, can not be use in china,Fuck!!
        mCallbackManager = CallbackManager.Factory.create();
        mFacebook_loginButton = (LoginButton) findViewById(R.id.fb_login_bt);
        mFacebook_loginButton.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                //MyLogin.getInstance().setContext(LoginActivity2.this);
                MyLogin.getInstance().setLoginStatus(MyLogin.STA_FACEBOOK);
                MyLogin.getInstance().fetchAVOSUser();
                MyLogin.getInstance().fetchSSoUser();
                updatedUi(LS_FETCHING_USER);
            }

            @Override
            public void onCancel() {
                setResult(MyConstant.LOGIN_CANCEL);
                updatedUi(LS_AUTH_CANCEL);
            }

            @Override
            public void onError(FacebookException e) {
                setResult(MyConstant.LOGIN_ERROR);
                Toast.makeText(LoginActivity2.this,getResources().getString(R.string.login_fail)+" ("+e.getMessage()+")", Toast.LENGTH_SHORT).show();
                updatedUi(LS_AUTH_ERR);
            }
        });

        mFacebook_loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updatedUi(LS_AUTHING);
            }
        });
        // end init facebook
        */
        mSkip = (TextView)findViewById(R.id.skip);
        mSkip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent it = new Intent(LoginActivity2.this, MainActivity.class);
                //startActivity(it);
                setResult(MyConstant.LOGIN_CANCEL);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login_activity2, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // SSO 授权回调
        // 重要：发起 SSO 登陆的 Activity 必须重写 onActivityResult

        if (mSsoHandler != null) {
            // sina login reqponse;
            mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
        }
        /*comment the facebook code, can not be use in china,Fuck!!
        if (mCallbackManager != null){
            // facebook login response;
            mCallbackManager.onActivityResult(requestCode, resultCode, data);
        }
        */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onSSOUserUpdated(MyLogin.SSOUser ssOUser) {
        SSO_DONE = true;
        if (AVO_DONE && SSO_DONE){
            setResult(0);
            finish();
        }
    }

    @Override
    public void onSSOUserUpdatedError(String msg) {
        Toast.makeText(LoginActivity2.this, getResources().getString(R.string.login_fail) + "("+msg+")", Toast.LENGTH_LONG).show();
        updatedUi(LS_FETCHED_USER_ERR);
    }

    @Override
    public void onAVOSUserUpdated(AVUser avUser) {
        AVO_DONE = true;
        if (AVO_DONE && SSO_DONE){
            setResult(0);
            this.finish();
        }
    }

    @Override
    public void onAVOSUserUpdatedError(String msg) {
        Toast.makeText(LoginActivity2.this, getResources().getString(R.string.login_fail) + "("+msg+")", Toast.LENGTH_LONG).show();
        updatedUi(LS_FETCHED_USER_ERR);
    }

    public void updatedUi(int status){
        switch (status){
            case LS_AUTHING:
                mLoginbt.setVisibility(View.GONE);

                /* comment the facebook code, can not be use in china,Fuck!!
                mFacebook_loginButton.setVisibility(View.GONE);
                */
                mLogin_status.setText("登录中...");
                break;
            case LS_FETCHING_USER:
                mLoginbt.setVisibility(View.GONE);
                /* comment the facebook code, can not be use in china,Fuck!!
                mFacebook_loginButton.setVisibility(View.GONE);
                */
                //mSkip.setEnabled(false);
                mLogin_status.setText("获取用户信息中,请稍后...");
                break;
            case LS_AUTH_ERR:
            case LS_AUTH_CANCEL:
            case LS_FETCHED_USER_ERR:
                mLoginbt.setVisibility(View.VISIBLE);
                /* comment the facebook code, can not be use in china,Fuck!!
                mFacebook_loginButton.setVisibility(View.VISIBLE);
                */
                mLogin_status.setText("");
                //mSkip.setEnabled(true);
                break;
        }
    }

    class AuthListener implements WeiboAuthListener {

        @Override
        public void onComplete(Bundle values) {
            mAccessToken = Oauth2AccessToken.parseAccessToken(values);
            if (mAccessToken.isSessionValid()) {
                // 显示 Token
                //updateTokenView(false);

                // 保存 Token 到 SharedPreferences
                AccessTokenkeeper.writeAccessToken(LoginActivity2.this, mAccessToken);
                Toast.makeText(LoginActivity2.this, R.string.weibo_auth_success, Toast.LENGTH_SHORT).show();

                MyLogin.getInstance().setContext(LoginActivity2.this);
                MyLogin.getInstance().setLoginStatus(MyLogin.STA_WEIBO);
                MyLogin.getInstance().fetchSSoUser();
                MyLogin.getInstance().fetchAVOSUser();
                updatedUi(LS_FETCHING_USER);
                //finish();

            } else {
                // 以下几种情况，您会收到 Code：
                // 1. 当您未在平台上注册的应用程序的包名与签名时；
                // 2. 当您注册的应用程序包名与签名不正确时；
                // 3. 当您在平台上注册的包名和签名与您当前测试的应用的包名和签名不匹配时
                MyLogin.getInstance().setLoginStatus(MyLogin.STA_LOGOUT);
                String code = values.getString("code");
                String message = getResources().getString(R.string.weibo_auth_fail) + ":";
                if (!TextUtils.isEmpty(code)) {
                    message += code;
                }
                Toast.makeText(LoginActivity2.this, message, Toast.LENGTH_LONG).show();
                //setResult(Integer.parseInt(code));
                //finish();
               updatedUi(LS_AUTH_ERR);
            }
        }

        @Override
        public void onWeiboException(WeiboException e) {
            Log.e(MyConstant.TAG_WEIBO_AUTH, e.getMessage());
            updatedUi(LS_AUTH_ERR);
        }

        @Override
        public void onCancel() {
            Log.v(MyConstant.TAG_WEIBO_AUTH, getResources().getString(R.string.weibo_auth_cancel));
            updatedUi(LS_AUTH_CANCEL);
        }
    }

    public static class AdFragment extends Fragment{
        private AdView mAdView;

        public AdFragment() {
        }

        @Override
        public void onActivityCreated(Bundle bundle) {
            super.onActivityCreated(bundle);

            // Gets the ad view defined in layout/ad_fragment.xml with ad unit ID set in
            // values/strings.xml.
            mAdView = (AdView) getView().findViewById(R.id.adView);

            // Create an ad request. Check logcat output for the hashed device ID to
            // get test ads on a physical device. e.g.
            // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build();

            // Start loading the ad in the background.
            mAdView.loadAd(adRequest);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_ad, container, false);
        }

        /** Called when leaving the activity */
        @Override
        public void onPause() {
            if (mAdView != null) {
                mAdView.pause();
            }
            super.onPause();
        }

        /** Called when returning to the activity */
        @Override
        public void onResume() {
            super.onResume();
            if (mAdView != null) {
                mAdView.resume();
            }
        }

        /** Called before the activity is destroyed */
        @Override
        public void onDestroy() {
            if (mAdView != null) {
                mAdView.destroy();
            }
            super.onDestroy();
        }
    }
}
