package com.geekfocus.zuweie.showfm;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.umeng.analytics.MobclickAgent;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import de.hdodenhof.circleimageview.CircleImageView;


public class NovelDetailActivity extends Activity {

    public static ClientID mClientid = new ClientID();

    private Messenger mPlayback = null;
    private Messenger mItself = null;

    private CircleImageView mNovel_im;
    private CircleImageView mNj_im;
    private ImageButton mPl_bt;
    private TextView mNovel_body_tx;
    private TextView mNovel_nj_tx;
    private TextView mNovel_nvl_name_tx;

    private MenuItem mMenuItem;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPlayback = new Messenger(service);
            try{
                Message msg = Message.obtain(null, PlayBackService.MSG_LOGIN);
                msg.replyTo = mItself;
                // client id;
                msg.arg1 = mClientid.getClientID();

                mPlayback.send(msg);

                msg = Message.obtain(null, PlayBackService.MSG_CURRENT_STATUS);
                mPlayback.send(msg);
            }catch (RemoteException e){
                Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel_detail);


        // init the Message Handler
        mItself = new Messenger(new NovelDetaileHandler());

        this.getActionBar().setHomeButtonEnabled(true);
        this.getActionBar().setIcon(R.drawable.activity_back);
        this.getActionBar().setTitle(this.getIntent().getStringExtra("nvltitle"));
        // init the Ui

        mNovel_im = (CircleImageView) this.findViewById(R.id.novel_im);
        mNovel_im.setBorderWidth(8);
        mNovel_im.setBorderColor(getResources().getColor(R.color.novel_detail_cim_border1));
        int novelid = this.getIntent().getIntExtra("nvl", -1);
        File fpic = new File(getFilesDir().getAbsolutePath()+"/"+novelid+".jpg");
        if (fpic.exists()){
            mNovel_im.setImageURI(Uri.parse(fpic.toURI().toString()));
        }else{
            mNovel_im.setImageResource(R.drawable.playlist_df_bg);
        }

        mNj_im = (CircleImageView) this.findViewById(R.id.nj_avatar_im);
        mNj_im.setBorderWidth(6);
        mNj_im.setBorderColor(getResources().getColor(R.color.novel_detail_cim_border2));

        String njid = this.getIntent().getStringExtra("njid");
        fpic = new File(getFilesDir().getAbsolutePath()+"/avatar_"+njid+".jpg");
        if (fpic.exists()){
            mNj_im.setImageURI(Uri.parse(fpic.toURI().toString()));
        }else{
            mNj_im.setImageResource(R.drawable.no_avatar);
            new LoadAvatarTask().execute(this.getIntent().getStringExtra("njavatar"), this.getIntent().getStringExtra("njid"));
        }

        //mPl_bt = (ImageButton) this.findViewById(R.id.pl_bt);
        mNovel_body_tx = (TextView)this.findViewById(R.id.nvl_body_tx);
        mNovel_body_tx.setText("        "+this.getIntent().getStringExtra("nvlbody"));

        mNovel_nvl_name_tx = (TextView)this.findViewById(R.id.novel_name_tx);
        mNovel_nvl_name_tx.setText("-"+this.getIntent().getStringExtra("nvltitle")+"-");

        mNovel_nj_tx = (TextView)this.findViewById(R.id.nj_name_tx);
        mNovel_nj_tx.setText("NJ : "+this.getIntent().getStringExtra("njname"));

        mPl_bt = (ImageButton)findViewById(R.id.pl_bt);
        mPl_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent it = new Intent(NovelDetailActivity.this, PlayingListActivity.class);
                it.putExtra("nvl", getIntent().getIntExtra("nvl", -1));
                it.putExtra("nvlf", getIntent().getStringExtra("nvlf"));
                it.putExtra("nvltitle", getIntent().getStringExtra("nvltitle"));
                it.putExtra("pm", getIntent().getIntExtra("pm", MyConstant.PM_NOVEL));
                startActivity(it);
                NovelDetailActivity.this.finish();
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_novel_detail, menu);
        mMenuItem = menu.findItem(R.id.action_play);
        mMenuItem.setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home){
            this.finish();
        }else if (id == R.id.action_play){
            if (mPlayback != null){
                Integer status = item.getIntent().getIntExtra("player_status", -1);
                try{
                    Message msg = null;
                    if (status == PlayBackService.STA_STARTED){
                        msg = Message.obtain(null, PlayBackService.MSG_PAUSE);
                    }else if (status == PlayBackService.STA_PAUSED){
                        msg = Message.obtain(null, PlayBackService.MSG_START);
                    }
                    mPlayback.send(msg);
                }catch (RemoteException e){
                    Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                }
            }
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        bs();
    }

    @Override
    protected  void onPause() {
        ubs();
        MobclickAgent.onPause(this);
        super.onPause();
    }

    public boolean connectService(){
        return mPlayback != null;
    }

    public void updateActionBarMenu(int what){
        switch(what){
            case PlayBackService.STA_STARTED:
                mMenuItem.setIcon(R.drawable.actionbar_pause);
                if (mMenuItem.getIntent() == null){
                    mMenuItem.setIntent(new Intent());
                }
                mMenuItem.getIntent().putExtra("player_status", what);
                mMenuItem.setVisible(true);
                break;
            case PlayBackService.STA_PAUSED:
                mMenuItem.setIcon(R.drawable.actionbar_start);
                if (mMenuItem.getIntent() == null){
                    mMenuItem.setIntent(new Intent());
                }
                mMenuItem.getIntent().putExtra("player_status", what);
                mMenuItem.setVisible(true);
                break;
            default:
                mMenuItem.setVisible(false);
        }
    }

    public void bs() {
        if (!connectService()){
            Intent it = new Intent(NovelDetailActivity.this, PlayBackService.class);
            bindService(it, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    public void ubs () {
        if (connectService()){
            Message msg = Message.obtain(null, PlayBackService.MSG_LOGOUT);
            msg.arg1 = mClientid.getClientID();
            try {
                mPlayback.send(msg);
            } catch (RemoteException e) {
                Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
            }
            unbindService(mConnection);
            mPlayback = null;
        }
    }

    class NovelDetaileHandler extends Handler {
        @Override
        public void handleMessage(Message msg){
            PlayBackService.Status status = null;
            switch(msg.what){
                case PlayingListActivity.MSG_ON_CURRENT_STATUS:
                case PlayingListActivity.MSG_ON_MP3STA_UPDATE:
                    // TODO : update icon of the actionbar
                    status = (PlayBackService.Status)msg.obj;
                    updateActionBarMenu(status.status);
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    class LoadAvatarTask extends AsyncTask<String, Void, Integer> {

        @Override
        protected Integer doInBackground(String[] params) {

            try {
                URL url = new URL(params[0]);
                njid = params[1];
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5 * 1000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
                connection.setRequestProperty("Accept-Language", "zh-CN");
                connection.setRequestProperty("Charset", "UTF-8");
                connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
                connection.setRequestProperty("Connection", "Keep-Alive");
                Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
                if (bitmap != null){
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    FileOutputStream fos = openFileOutput("avatar_"+njid+".jpg", Context.MODE_PRIVATE);
                    fos.write(bos.toByteArray());
                    fos.close();
                    return 0;
                }
            } catch (MalformedURLException e) {
                Log.e(MyConstant.TAG_NOVEL, e.getMessage());
            } catch (ProtocolException e) {
                Log.e(MyConstant.TAG_NOVEL, e.getMessage());
            } catch (IOException e) {
                Log.e(MyConstant.TAG_NOVEL, e.getMessage());
            }

            return -1;
        }

        @Override
        protected void onPostExecute(Integer result) {
            if (result == 0){
                String avatar = getFilesDir().getAbsolutePath()+"/avatar_"+njid+".jpg";
                Bitmap bmp = BitmapFactory.decodeFile(avatar);
                mNj_im.setImageBitmap(bmp);
            }
        }
        //CircleImageView cimg;
        String njid;
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
