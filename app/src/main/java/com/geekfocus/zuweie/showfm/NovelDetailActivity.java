package com.geekfocus.zuweie.showfm;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
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

import de.hdodenhof.circleimageview.CircleImageView;


public class NovelDetailActivity extends Activity {

    public static ClientID mClientid = new ClientID();

    private Messenger mPlayback = null;
    private Messenger mItself = null;

    private NetBitMap mNovelCover;
    private NetBitMap mNjAvatar;
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
        mNovelCover = new NetBitMap(NovelDetailActivity.this, getIntent().getStringExtra("nvlposter"), R.drawable.playlist_df_bg);
        mNovel_im = (CircleImageView) this.findViewById(R.id.novel_im);
        mNovel_im.setBorderWidth(8);
        mNovel_im.setBorderColor(getResources().getColor(R.color.novel_detail_cim_border1));
        mNovel_im.setImageBitmap(mNovelCover.getBitmap(new NetBitMap.LoadBitmapCallback() {
            @Override
            public void done(Bitmap bm, Object error) {
                if (bm != null)
                    mNovel_im.setImageBitmap(bm);
            }
        }));

        mNj_im = (CircleImageView) this.findViewById(R.id.nj_avatar_im);
        mNj_im.setBorderWidth(6);
        mNj_im.setBorderColor(getResources().getColor(R.color.novel_detail_cim_border2));

        mNjAvatar = new NetBitMap(NovelDetailActivity.this, getIntent().getStringExtra("njavatar"), R.drawable.no_avatar);
        mNj_im.setImageBitmap(mNjAvatar.getBitmap(new NetBitMap.LoadBitmapCallback() {
            @Override
            public void done(Bitmap bm, Object error) {
                if (bm != null)
                    mNj_im.setImageBitmap(bm);
            }
        }));

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
                it.putExtra("nvlposter", getIntent().getStringExtra("nvlposter"));
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
