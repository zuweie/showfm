package com.geekfocus.zuweie.showfm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVUser;
import com.avos.avoscloud.LogInCallback;
import com.facebook.AccessToken;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.geekfocus.zuweie.showfm.oauth.UsersAPI;
import com.geekfocus.zuweie.showfm.oauth.models.User;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;

/**
 * Created by zuweie on 3/7/15.
 */
public class MyLogin {

    public static final String PREFERENCES_NAME = "login";
    public static final String KEY_STATUS = "log_status";


    public static final int STA_LOGOUT = 0;
    public static final int STA_WEIBO = 1;
    public static final int STA_QQ = 2;
    public static final int STA_FACEBOOK = 3;

    private static MyLogin login;
    private int mStatus = STA_LOGOUT;

    private AVUser mAVOSUser = null;
    //private User mSSOWeiboUser;
    private SSOUser mSSOUser = null;
    private Oauth2AccessToken mWeiboToken;
    private AccessToken mFacebookToken;
    private ProfileTracker mProfileTracker;

    private OnUpdateLoginUserListener listener;

    private Context mContext;

    public static MyLogin getInstance(){
        if (login == null){
            login = new MyLogin();
        }
        return login;
    }

    public void setContext(Context context) {
        mContext = context;
    }

    public void readLoginStatus (){
        if (mContext !=  null){
            SharedPreferences pref = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_APPEND);
            mStatus = pref.getInt(KEY_STATUS, STA_LOGOUT);
        }
    }

    public void writeLoginStatus () {
        if (mContext != null){
            SharedPreferences pref = mContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_APPEND);
            SharedPreferences.Editor editor = pref.edit();
            editor.putInt(KEY_STATUS, mStatus);
            editor.commit();
        }
    }

    public void setListener(OnUpdateLoginUserListener l){
        listener = l;
    }

    public void removedListener () {
        listener = null;
    }

    public int getLoginStatus () {
        return mStatus;
    }

    public void setLoginStatus (int status){
        if (mStatus != status){
            mStatus = status;
            writeLoginStatus();
        }

    }

    public SSOUser getSSOUser(){
       return mSSOUser;
    }

    public void setSSOUser(SSOUser ssoUser){
        mSSOUser = ssoUser;
    }

    public AVUser getAVOSUser () {
        return mAVOSUser;
    }

    public void loadToken() {
        if (mStatus == STA_WEIBO && mWeiboToken == null){
            mWeiboToken = AccessTokenkeeper.readAccessToken(mContext);
        }else if (mStatus == STA_FACEBOOK && mFacebookToken == null){
            mFacebookToken = AccessToken.getCurrentAccessToken();
        }
    }

    public boolean isSSOTokenvalid(){
        loadToken();
        if (mStatus == STA_WEIBO && mWeiboToken != null){
            return mWeiboToken.isSessionValid();
        }else if (mStatus == STA_FACEBOOK && mFacebookToken != null){
            return !mFacebookToken.isExpired();
        }
        return false;
    }

    public void logout() {
        if (getLoginStatus() == STA_WEIBO){
            AccessTokenkeeper.clear(mContext);
        }
        mSSOUser = null;
        mAVOSUser = null;
        setLoginStatus(STA_LOGOUT);
    }

    public boolean isLogin() {
        return (getLoginStatus() != STA_LOGOUT);
    }

    public String getToken(){
        if (mStatus == STA_WEIBO && mWeiboToken != null){
            mWeiboToken.getToken();
        }else if (mStatus == STA_FACEBOOK && mFacebookToken != null){
            mFacebookToken.getToken();
        }
        return "";
    }

    public String getTokenUid (){
        if (mStatus == STA_WEIBO && mWeiboToken != null){
            return mWeiboToken.getUid();
        }else if (mStatus == STA_FACEBOOK && mFacebookToken != null){
            return mFacebookToken.getUserId();
        }
        return "";
    }

    public String getSSOType (){
        if (mStatus == STA_WEIBO)
            return "weibo";
        else if (mStatus == STA_QQ)
            return "qq";
        else if (mStatus == STA_FACEBOOK)
            return "facebook";
        else
            return "";
    }

    public void fetchSSoUser (){
        if (mStatus == STA_WEIBO && isSSOTokenvalid()){
            UsersAPI usersAPI = new UsersAPI(mContext,Myfunc.getValidText(MyConstant.WEIBO_APP_KEY), mWeiboToken);
            long uid = Long.parseLong(mWeiboToken.getUid());
            usersAPI.show(uid, new RequestListener() {
                @Override
                public void onComplete(String s) {
                    // get the weibo user data;
                    User user  = User.parse(s);
                    SSOUser ssoUser = new SSOUser(user, mContext, MyLogin.STA_WEIBO);
                    Log.d(MyConstant.TAG_LOGIN, "sso login success : "+ssoUser.getNickName());
                    setSSOUser(ssoUser);
                    if (listener != null)
                        listener.onSSOUserUpdated(ssoUser);
                    Toast.makeText(mContext, mContext.getResources().getString(R.string.welcome_user) +" "+ssoUser.getNickName(), Toast.LENGTH_LONG ).show();
                }
                @Override
                public void onWeiboException(WeiboException e) {
                    Log.e(MyConstant.TAG_LOGIN, e.getMessage());
                    if (listener != null)
                        listener.onSSOUserUpdatedError(e.getMessage());
                }
            });
        }else if (mStatus == STA_FACEBOOK && isSSOTokenvalid()){

            if (FacebookSdk.isInitialized() && Profile.getCurrentProfile() == null) {
                if (mProfileTracker == null){
                    mProfileTracker = new ProfileTracker() {
                        @Override
                        protected void onCurrentProfileChanged(Profile profile, Profile profile2) {
                            SSOUser ssoUser = new SSOUser(mContext, MyLogin.STA_FACEBOOK);
                            MyLogin.getInstance().setSSOUser(ssoUser);
                            mProfileTracker.stopTracking();
                            if (listener != null)
                                listener.onSSOUserUpdated(ssoUser);
                        }
                    };
                }
                mProfileTracker.startTracking();
                Profile.fetchProfileForCurrentAccessToken();

            }else if (Profile.getCurrentProfile() != null){

                SSOUser ssoUser = new SSOUser(mContext, MyLogin.STA_FACEBOOK);
                MyLogin.getInstance().setSSOUser(ssoUser);
                if (listener != null)
                    listener.onSSOUserUpdated(ssoUser);

            }else{
                if (listener != null)
                    listener.onSSOUserUpdatedError("unknown!");
            }

        }
    }

    public void fetchAVOSUser (){
        if (isSSOTokenvalid()){
            AVUser.AVThirdPartyUserAuth userAuth = new AVUser.AVThirdPartyUserAuth(getToken(),"2020-03-04T07:53:48.298Z",getSSOType(), getTokenUid());
            AVUser.loginWithAuthData(AVUser.class, userAuth, new LogInCallback<AVUser>() {
                @Override
                public void done(AVUser avUser, AVException e) {
                    if (e == null){
                        mAVOSUser = avUser;
                        if (listener != null)
                            listener.onAVOSUserUpdated(mAVOSUser);
                    }else{
                        Log.e(MyConstant.TAG_LOGIN, e.getMessage());
                        if (listener != null)
                            listener.onAVOSUserUpdatedError(e.getMessage());
                    }
                }
            });
        }else{
            if (listener != null)
                listener.onAVOSUserUpdatedError("had not valid access token!");
        }
    }

    public boolean isSSoLogin(){
        return mSSOUser != null && mSSOUser.isLoginValid();
    }

    public boolean isAVOSLogin(){
        return mAVOSUser != null;
    }

    public static interface OnUpdateLoginUserListener {
        public void onSSOUserUpdated(SSOUser ssOUser);
        public void onSSOUserUpdatedError(String msg);
        public void onAVOSUserUpdated(AVUser avUser);
        public void onAVOSUserUpdatedError(String msg);
    }

    public static class SSOUser {

        public SSOUser (User weibouser, Context context, int logty) {
            mWeiboUser = weibouser;
            mUserType = logty;
            mContext = context;
            mUserAvatar = new NetBitMap(mContext, mWeiboUser.profile_image_url,  R.drawable.no_avatar);
        }

        public SSOUser(Context context, int logty){
            mUserType = logty;
            mContext = context;
        }

        public String getNickName () {
            if (mUserType == MyLogin.STA_WEIBO && mWeiboUser != null){
                return mWeiboUser.screen_name;
            }else if (mUserType == MyLogin.STA_FACEBOOK && Profile.getCurrentProfile() != null){
                return Profile.getCurrentProfile().getName();
            }
            return "";
        }

        public String getUserID() {
            if (mUserType == MyLogin.STA_WEIBO && mWeiboUser != null){
                return mWeiboUser.id;
            }else if (mUserType == MyLogin.STA_FACEBOOK && Profile.getCurrentProfile() != null){
                return Profile.getCurrentProfile().getId();
            }
            return "";
        }

        public String getAvatarUrl() {
            if (mUserType == MyLogin.STA_WEIBO && mWeiboUser != null){
                return mWeiboUser.profile_image_url;
            }else if (mUserType == MyLogin.STA_FACEBOOK && Profile.getCurrentProfile() != null){
                return Profile.getCurrentProfile().getProfilePictureUri(50, 50).toString();
            }
            return "";
        }

        public NetBitMap getAvatar() {
            if (mUserAvatar == null){
                // init you user Avatar here
                if (mUserType == MyLogin.STA_WEIBO){
                    mUserAvatar = new NetBitMap(mContext, mWeiboUser.profile_image_url,  R.drawable.no_avatar);
                }else if (mUserType == MyLogin.STA_FACEBOOK && Profile.getCurrentProfile() != null){
                    mUserAvatar = new NetBitMap(mContext, Profile.getCurrentProfile().getProfilePictureUri(50, 50).toString(), R.drawable.no_avatar);
                }
            }
            return mUserAvatar;
        }
        public boolean isLoginValid() {
            if (mUserType == MyLogin.STA_WEIBO)
                return mWeiboUser != null;
            else if (mUserType == MyLogin.STA_FACEBOOK)
                return Profile.getCurrentProfile() != null;
            else
                return false;
        }
        private int mUserType;
        private User mWeiboUser;
        private NetBitMap mUserAvatar;
        private Context mContext;
    }
}
