package com.geekfocus.zuweie.showfm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.feedback.FeedbackAgent;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class SettingActivity extends Activity {

    CircleImageView mAvatar = null;
    Button mLogin = null;
    TextView mLogin_tx = null;
    /*
    LoginButton mLogin_bt = null;
    */
    String mV_addr;
    //AccessTokenTracker maccessTokenTracker;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        /*init ui */
        /*comment the facebook code, can not be use in china,Fuck!!
        FacebookSdk.sdkInitialize(getApplicationContext());
        */
        // action_ban
        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setIcon(R.drawable.activity_back);

        /* comment the facebook code, can not be use in china,Fuck!!
        maccessTokenTracker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken accessToken, AccessToken accessToken2) {
                if (accessToken2 == null){
                    MyLogin.getInstance().logout();
                    updatedUi();
                }
            }
        };
        */

        mLogin_tx = (TextView)findViewById(R.id.login_tx);
        mAvatar = (CircleImageView)findViewById(R.id.user_avatar_im);
        /*
        mLogin_bt = (LoginButton)findViewById(R.id.fb_login_bt);
        */
        mLogin = (Button) findViewById(R.id.login_bt);
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (MyLogin.getInstance().getSSOUser() != null
                    || MyLogin.getInstance().getAVOSUser() != null){
                    // TODO : logout
                    AccessTokenkeeper.clear(SettingActivity.this);
                    MyLogin.getInstance().logout();
                    updatedUi();
                }else{
                    // TODO : login
                    Intent it = new Intent(SettingActivity.this, LoginActivity2.class);
                    startActivityForResult(it, MyConstant.LOGIN_REQ_CODE);
                }
            }
        });
        updatedUi();
        Button versionbt = (Button) findViewById(R.id.version_bt);
        String currentversion = getResources().getString(R.string.current_version) + " "+Myfunc.getVersionName(SettingActivity.this);
        currentversion += " "+getResources().getString(R.string.click_to_check);

        versionbt.setText(currentversion);
        versionbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Toast.makeText(SettingActivity.this, R.string.checking_version, Toast.LENGTH_LONG).show();

                AVQuery<AVObject> query = new AVQuery<AVObject>("android_version");
                query.orderByDescending("updatedAt");
                query.limit(1);
                query.findInBackground(new FindCallback<AVObject>() {
                    @Override
                    public void done(List<AVObject> avObjects, AVException e) {
                        int versionCode = 0;
                        String versionName = "";
                        String versionMsg = "";
                        for (int i=0; i<avObjects.size(); ++i){
                            AVObject version = avObjects.get(i);
                            versionCode = version.getInt("versionCode");
                            versionName = version.getString("versionName");
                            versionMsg = version.getString("v_log");
                            mV_addr = version.getString("v_addr");
                        }
                        if (Myfunc.getVersionCode(SettingActivity.this) != versionCode
                           || !Myfunc.getVersionName(SettingActivity.this).equals(versionName)){

                            // TODO : show update info
                            AlertDialog.Builder db = new AlertDialog.Builder(SettingActivity.this);
                            String version = getResources().getString(R.string.version_dlg_title_new) + " ["+versionName+"("+versionCode+")]";
                            db.setTitle(version);
                            db.setMessage(versionMsg);

                            db.setPositiveButton(R.string.positive, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // TODO :
                                    Intent it = new Intent();
                                    it.setAction("android.intent.action.VIEW");
                                    Uri uri = Uri.parse(mV_addr);
                                    it.setData(uri);
                                    startActivity(it);
                                    dialog.dismiss();
                                }
                            });
                            db.setNegativeButton(R.string.negative, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            db.create().show();
                        }else{
                            AlertDialog.Builder db = new AlertDialog.Builder(SettingActivity.this);
                            db.setTitle(R.string.version_dlg_title);
                            db.setMessage(R.string.version_dlg_content_newest);
                            db.setPositiveButton(R.string.positive, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                            db.create().show();
                        }
                    }

                });
            }
        });

        Button exitbt = (Button) findViewById(R.id.exit);
        exitbt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(MyConstant.ME_RESULT_EXIT);
                finish();
            }
        });

        Button feedback = (Button) findViewById(R.id.feedback);
        feedback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FeedbackAgent agent = new FeedbackAgent(SettingActivity.this);
                agent.startDefaultThreadActivity();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultcode, Intent data){
        if (requestCode == MyConstant.LOGIN_REQ_CODE && resultcode == MyConstant.LOGIN_OK){
            // update the ui
            updatedUi();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_setting, menu);
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
        }else if (id == android.R.id.home){
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy(){
        /* comment the facebook code, can not be use in china,Fuck!!
        maccessTokenTracker.stopTracking();
        maccessTokenTracker = null;
        */
        super.onDestroy();
    }

    public void updatedUi(){
        if (MyLogin.getInstance().isLogin() && MyLogin.getInstance().getSSOUser() != null){
            // login status
            // init avatar
            mAvatar.setImageBitmap(MyLogin.getInstance().getSSOUser().getAvatar().getBitmap(new NetBitMap.LoadBitmapCallback() {
                @Override
                public void done(Bitmap bm, Object error) {
                    if (bm != null)
                        mAvatar.setImageBitmap(bm);
                }

            }));

            mAvatar.setBorderWidth(6);
            mAvatar.setBorderColor(getResources().getColor(R.color.novel_detail_cim_border2));

            // init botton
            mLogin.setText(R.string.logout);

            if (MyLogin.getInstance().getLoginStatus() == MyLogin.STA_FACEBOOK){
                mLogin.setVisibility(View.GONE);
                /* comment the facebook code, can not be use in china,Fuck!!
                mLogin_bt.setVisibility(View.VISIBLE);
                */
            }else{
                mLogin.setVisibility(View.VISIBLE);
                mLogin.setText(R.string.logout);
                /* comment the facebook code, can not be use in china,Fuck!!
                mLogin_bt.setVisibility(View.GONE);
                */
            }
            mLogin_tx.setText(MyLogin.getInstance().getSSOUser().getNickName());
        }else{
            mLogin.setVisibility(View.VISIBLE);
            mLogin.setText(R.string.login);
            /* comment the facebook code, can not be use in china,Fuck!!
            mLogin_bt.setVisibility(View.GONE);
            */
            mLogin_tx.setText(R.string.no_login);
            mAvatar.setImageResource(R.drawable.no_avatar);
        }
    }

    public static class AdFragment extends Fragment {
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
