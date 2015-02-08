package com.geekfocus.zuweie.showfm;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
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
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dodowaterfall.widget.ScaleImageView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.huewu.pla.lib.internal.PLA_AbsListView;
import com.paveldudka.util.FastBlur;
import com.umeng.analytics.MobclickAgent;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;


public class PlayingListActivity extends Activity {

    /* content data */
    public static ClientID mClientId  = new ClientID();
    private List<ContentValues> mPlaying_data;
    private Record mRecord = new Record();
    private Integer mPlayMode;
    private int mNovelId;
    private String mNovelFolder;

    /* UI data */
    private PullToRefreshListView mPullToRefreshListView;
    private ListView mPlayinglistView;
    private MyAdapter mMyadapter;
    private Integer mExtendItemPos = 0;
    private MenuItem mPlaybackItem;
    private HeadHolder mHeaderHolder;

    /* PlayBack data */
    private Messenger mPlayback;
    private Messenger mItSelf;
    private boolean mBindService = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPlayback = new Messenger(service);
            mBindService = true;

            try {
                // log to service
                Message msg = Message.obtain(null, PlayBackService.MSG_LOGIN);
                msg.replyTo = mItSelf;
                msg.arg1 = mClientId.getClientID();
                mPlayback.send(msg);

                // post request to get Record list
                if (isNovelMode()){
                    msg = Message.obtain(null, PlayBackService.MSG_LOAD_RECORD_LIST);
                    msg.arg1 = mNovelId;
                    mPlayback.send(msg);
                }

                msg = Message.obtain(null, PlayBackService.MSG_CURRENT_STATUS);
                mPlayback.send(msg);

                msg = Message.obtain(null, PlayBackService.MSG_START_PLAYER_PGROGRESS_UPDATER);
                mPlayback.send(msg);

            } catch (RemoteException e) {
                Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };
    /* PlayBack data */

    /* Handle Message define */
    public final static int MSG_ON_MP3STA_UPDATE = 1;
    public final static int MSG_ON_LOAD_RECORD_LIST_DONE = 2;
    public final static int MSG_ON_DOWNLOAD_PROGRESS = 3;
    public final static int MSG_ON_DOWNLOAD_DONE = 4;
    public final static int MSG_ON_DOWNLOAD_PAUSED = 6;
    public final static int MSG_ON_DOWNLOAD_ERR    = 7;
    public final static int MSG_ON_DOWNLOAD_STARTED = 8;
    public final static int MSG_ON_DOWNLOAD_DELETE = 9;
    public final static int MSG_ON_READY_DOWNLOAD = 5;
    public final static int MSG_ON_EXTEND_ITEM = 10;
    public final static int MSG_ON_MP3PROGRESS_UPDTED = 11;
    public final static int MSG_ON_MP3BUFFERING_UPDATED = 12;
    public final static int MSG_ON_CURRENT_STATUS = 13;
    public final static int MSG_ON_CLEAN_UP_ITEM_UI = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing_list);

        /* init content data*/
        mPlayMode = this.getIntent().getIntExtra("pm", -1);
        if (isNovelMode()) {
            mNovelId = this.getIntent().getIntExtra("nvl", -1);
            mNovelFolder = this.getIntent().getStringExtra("nvlf");
            this.getActionBar().setTitle(this.getIntent().getStringExtra("nvltitle"));
        }

        /* init the UI */
        //mHandler = new PlayingListHandler();
        this.getActionBar().setHomeButtonEnabled(true);
        this.getActionBar().setIcon(R.drawable.activity_back);

        mItSelf = new Messenger(new PlayingListHandler());
        mPullToRefreshListView = (PullToRefreshListView)this.findViewById(R.id.playing_list);
        mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<ListView>() {
            @Override
            public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
                if (mPlayback != null&&isNovelMode()){
                    Message msg = Message.obtain(null, PlayBackService.MSG_LOAD_RECORD_LIST);
                    msg.arg1 = mNovelId;
                    try {
                        mPlayback.send(msg);
                    } catch (RemoteException e) {
                        Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                    }
                }
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {

            }
        });
        /* init the playlist view */
        mPlayinglistView = mPullToRefreshListView.getRefreshableView();


        if (isNovelMode()){

            /* Playlist header just use a big pic */
            View header = this.getLayoutInflater().inflate(R.layout.playlist_header_2, null);
            mHeaderHolder = new HeadHolder();

            mHeaderHolder.nvl_bg_im = (ScaleImageView) header.findViewById(R.id.nvl_img_view);
            mHeaderHolder.nvl_wbg_im = (ScaleImageView) header.findViewById(R.id.white_img_view);

            mHeaderHolder.nj_name_tx = (TextView)header.findViewById(R.id.nj_name_tx);
            mHeaderHolder.nvl_author_tx = (TextView) header.findViewById(R.id.nvl_name_tx);

            String author = this.getIntent().getStringExtra("nvlauthor");
            String category = this.getIntent().getStringExtra("nvlcategory");
            mHeaderHolder.nvl_author_tx.setText(author + " / " + category);

            String njname = this.getIntent().getStringExtra("njname");
            String status = this.getIntent().getStringExtra("nvlstatus");
            mHeaderHolder.nj_name_tx.setText(njname + " [" + status + "]");

            /*
            mHeaderHolder.nvl_comments_bt = (ImageButton) header.findViewById(R.id.nvl_comments_bt);
            mHeaderHolder.nvl_comments_bt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    Uri content_url = Uri.parse("http://yjh.geekfocus.cc/index.php/Showfm/Public/novelComments/id/"+mNovelId);
                    intent.setData(content_url);
                    startActivity(intent);
                }
            });
            */
            /* 获取 NJ 的头像 */
            mHeaderHolder.nj_avatar_img = (CircleImageView)header.findViewById(R.id.nj_avatar_img_view);
            String njid = this.getIntent().getStringExtra("njid");
            String avatarfile = this.getFilesDir().getAbsolutePath()+"/avatar_"+njid+".jpg";
            File file = new File(avatarfile);
            if (file.exists()){
                mHeaderHolder.nj_avatar_img.setImageURI(Uri.parse(avatarfile));
            }else{
                mHeaderHolder.nj_avatar_img.setImageResource(R.drawable.no_avatar);
                new LoadAvatarTask(mHeaderHolder.nj_avatar_img, njid).execute(this.getIntent().getStringExtra("njavatar"));
            }
            mHeaderHolder.nj_avatar_img.setBorderWidth(3);
            mHeaderHolder.nj_avatar_img.setBorderColor(Color.WHITE);
            /* 获取 NJ 的头像 end */

            if (this.getIntent().getBooleanExtra("cover_exists", false)){
                String coverfile = getFilesDir().getAbsolutePath()+"/"+mNovelId + ".jpg";
                file = new File(coverfile);
                if (file.exists()){

                    int cover_width = getIntent().getIntExtra("cover_width", 400);
                    int cover_height = getIntent().getIntExtra("cover_height", 200);

                    float whk = (float)cover_width / (float)cover_height;

                    if (whk >= 1.4){
                        mHeaderHolder.nvl_bg_im.setImageWidth(cover_width);
                        mHeaderHolder.nvl_bg_im.setImageHeight(cover_height);
                    }else{
                        mHeaderHolder.nvl_bg_im.setImageWidth(cover_width);
                        float wk = 0.5f;
                        cover_height = (int)(wk * cover_width);
                        mHeaderHolder.nvl_bg_im.setImageHeight(cover_height);
                    }
                    Log.v(MyConstant.TAG_NOVEL, "cover_width : "+cover_width);
                    Log.v(MyConstant.TAG_NOVEL, "cover_height : "+cover_height);

                    File blurbg = new File (getFilesDir().getAbsolutePath()+"/blur_"+mNovelId+".jpg");
                    if (!blurbg.exists()){
                        /* had nod blur file*/
                        Bitmap cover = BitmapFactory.decodeFile(coverfile);

                        Bitmap blur =  blur(cover, mHeaderHolder.nvl_bg_im);
                        try {
                            ByteArrayOutputStream bos = new ByteArrayOutputStream();
                            FileOutputStream fos = openFileOutput("blur_"+mNovelId+".jpg", Context.MODE_PRIVATE);
                            blur.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                            fos.write(bos.toByteArray());
                            fos.close();
                        } catch (FileNotFoundException e) {
                            Log.e(MyConstant.TAG_NOVEL, e.getMessage());
                        } catch (IOException e) {
                            Log.e(MyConstant.TAG_NOVEL, e.getMessage());
                        }
                    }else{
                       mHeaderHolder.nvl_bg_im.setImageURI(Uri.parse(blurbg.toURI().toString()));
                    }

                    mHeaderHolder.nvl_wbg_im.setImageWidth(416);
                    float k = (float)(416) / (float)(cover_width);
                    float bgwk = 0.45f;

                    int bgw = (int)(( cover_height * bgwk ) * k);

                    mHeaderHolder.nvl_wbg_im.setImageHeight(bgw);

                    mHeaderHolder.nvl_wbg_im.setBackgroundResource(R.drawable.nvl_w_bg);

                }else{
                    mHeaderHolder.nvl_bg_im.setImageResource(R.drawable.nvl_def_bg);
                }
            }else{
                mHeaderHolder.nvl_bg_im.setImageResource(R.drawable.nvl_def_bg);
            }
            mPlayinglistView.addHeaderView(header);
            mPlayinglistView.setHeaderDividersEnabled(true);

        }


        mMyadapter = new MyAdapter();
        mPlayinglistView.setAdapter(mMyadapter);

        mPlayinglistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // get item data
                if (id >=0){
                    Adapter adapter = parent.getAdapter();
                    ContentValues data = null;
                    if (mExtendItemPos > 0){
                        // close the last item
                        data = (ContentValues)adapter.getItem(mExtendItemPos);
                        data.put("item_mode", 0);
                    }

                    // extend the new item
                    data = (ContentValues)adapter.getItem(position);
                    mExtendItemPos = position;
                    data.put("item_mode", 1);

                    Message msg = Message.obtain(null, MSG_ON_EXTEND_ITEM);
                    try {
                        mItSelf.send(msg);
                    } catch (RemoteException e) {
                        Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                    }
                }
            }
        });

        mPlayinglistView.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {

                if (mExtendItemPos >= 0){
                    Adapter adapter = view.getAdapter();
                    ContentValues data = (ContentValues)adapter.getItem(mExtendItemPos);
                    if (data != null){
                        // close the item
                        data.put("item_mode", 0);
                        mExtendItemPos = -1;
                    }
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
        });
    }
    /* init playing list view */
    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onResume (){
        bs();
        MobclickAgent.onResume(this);
        super.onResume();
    }

    @Override
    protected void onPause () {
        ubs();
        MobclickAgent.onPause(this);
        super.onPause();
    }


    @Override
    protected void onStop(){
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_playing_list, menu);
        mPlaybackItem = menu.findItem(R.id.action_player);
        mPlaybackItem.setVisible(false);
        mPlaybackItem.setIntent(new Intent());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*
        if (id == R.id.action_settings) {
            return true;
        }else if ((int)id != (int)android.R.id.home){
            return true;
        }
        */
        if (id == android.R.id.home){
            this.finish();
        }else if (id == R.id.action_player){
            if (mPlayback != null){
                Message msg = null;
                int status = item.getIntent().getIntExtra("player_status", -1);
                if (status == PlayBackService.STA_STARTED){
                    msg = Message.obtain(null, PlayBackService.MSG_PAUSE);
                }else if (status == PlayBackService.STA_PAUSED){
                    msg = Message.obtain(null, PlayBackService.MSG_START);
                    msg.arg1 = 1;
                }else if (status == PlayBackService.STA_COMPLETED){
                    msg = Message.obtain(null, PlayBackService.MSG_START);
                    msg.arg1 = 1;
                }

                try {
                    mPlayback.send(msg);
                } catch (RemoteException e) {
                    Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                }
            }

        }
        return super.onOptionsItemSelected(item);
    }

    private Bitmap blur(Bitmap bp, ImageView view){
        float radius = 5.0f;
        Bitmap overlay = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888);
        // canvas 我可以理解为一个画笔。
        Canvas canvas = new Canvas(overlay);
        // 此处的 translate 就是移动 canvas的 x y值. 这里填入负值，就是将其移动到左上角。
        canvas.translate(-view.getLeft(), -view.getTop());
        canvas.drawBitmap(bp, 0, 0, null);
        overlay = FastBlur.doBlur(overlay, (int)radius, true);
        view.setImageBitmap(overlay);
        return overlay;
    }

    private void bs(){
        if (!mBindService){
            Intent it = new Intent(PlayingListActivity.this, PlayBackService.class);
            bindService(it, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void ubs(){
        if (mBindService){
            try {
                Message msg = Message.obtain(null, PlayBackService.MSG_LOGOUT);
                msg.arg1 = mClientId.getClientID();
                mPlayback.send(msg);

                msg = Message.obtain(null, PlayBackService.MSG_STOP_PLAYER_PROGRESS_UPDATER);
                mPlayback.send(msg);
            } catch (RemoteException e) {
                Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
            }
            unbindService(mConnection);
            mPlayback = null;
            mBindService = false;
        }
    }

    private boolean isNovelMode () {
        return mPlayMode == MyConstant.PM_NOVEL;
    }

    private int getValidPos(int itemid , int itempos){
        if (mPlaying_data != null && itempos < mPlaying_data.size() && itempos >=0  && isNovelMode() ){
            if (mPlaying_data.get(itempos).getAsInteger(Record.ID) == itemid)
                return itempos;
            else{
                for(int i=0; i<mPlaying_data.size();++i){
                    ContentValues data = mPlaying_data.get(i);
                    if (data.getAsInteger(Record.ID) == itemid)
                        return i;
                }
            }
        }
        return -1;
    }


    private boolean isVisiblePosition(int position){

        int first = mPlayinglistView.getFirstVisiblePosition();
        int last  = mPlayinglistView.getLastVisiblePosition();

        if (position >= first && position <= last)
            return true;
        else
            return false;

    }

    private String coverMs2Str(int ms){
        int second = (ms / 1000) % 60;
        int min    = (ms / 1000) / 60;
        return ""+(min>=10?min:"0"+min)+":"+(second>=10?second:"0"+second);
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

    class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            if (mPlaying_data == null)
                return 0;
            else
                return mPlaying_data.size();
        }

        @Override
        public Object getItem(int position) {
            return mPlaying_data.get(position);
        }

        @Override
        public long getItemId(int position) {
            if (isNovelMode())
                return mPlaying_data.get(position).getAsInteger(Record.ID);
            else
                return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;
            ContentValues data = (ContentValues)this.getItem(position);


            if (convertView == null){
                LayoutInflater inflater = PlayingListActivity.this.getLayoutInflater();
                convertView = inflater.inflate(R.layout.playlist_item, null);
                holder = new Holder();

                // easy mode
                holder.esm = convertView.findViewById(R.id.esm_item);
                holder.esm_title = (TextView)convertView.findViewById(R.id.esm_title);
                holder.esm_status = (ImageView)convertView.findViewById(R.id.esm_status);

                // playing mode
                holder.plm = convertView.findViewById(R.id.plm_item);

                holder.plm_title = (TextView)convertView.findViewById(R.id.plm_title);
                holder.plm_rec_nj = (TextView)convertView.findViewById(R.id.plm_rec_nj);
                //holder.plm_rec_updated = (TextView)convertView.findViewById(R.id.plm_rec_updated);
                holder.plm_rec_timer = (TextView)convertView.findViewById(R.id.plm_rec_timer);

                holder.plm_seekbar = (SeekBar)convertView.findViewById(R.id.plm_player_skb);
                holder.plm_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        if (fromUser && ((ContentValues)(seekBar.getTag())).getAsInteger("player_status") == PlayBackService.STA_STARTED){
                            // TODO : confirm this progress bar to the playing item
                            Message msg = Message.obtain(null, PlayBackService.MSG_SEEKTO);
                            msg.arg1 = progress;
                            if (mPlayback != null){
                                try {
                                    mPlayback.send(msg);
                                } catch (RemoteException e) {
                                    Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                                }
                            }
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                    }
                });
                holder.plm_playerbt = (ImageButton)convertView.findViewById(R.id.plm_playerbt);
                holder.plm_playerbt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (isNovelMode()){
                            ContentValues rdata = (ContentValues)v.getTag();
                            if (rdata.getAsInteger("player_status") == PlayBackService.STA_IDLE
                              || rdata.getAsInteger("player_status") == PlayBackService.STA_ERROR){
                                // Status of idle or error, play it
                                String url = rdata.getAsString(Record.URL);
                                String[] param = new String[2];
                                try {
                                    url = Myfunc.getValidUrl(mNovelFolder+"/", url, 900);
                                    Message msg = Message.obtain(null, PlayBackService.MSG_PLAY);
                                    msg.obj = url+" "+rdata.getAsInteger(Record.NOVELID);
                                    msg.arg1 = rdata.getAsInteger(Record.ID);
                                    msg.arg2 = rdata.getAsInteger("item_pos");
                                    mPlayback.send(msg);
                                } catch (NoSuchAlgorithmException e) {
                                } catch (RemoteException e) {Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());}
                            }else if (rdata.getAsInteger("player_status") == PlayBackService.STA_PAUSED
                                    ||rdata.getAsInteger("player_status") == PlayBackService.STA_COMPLETED) {
                                // Status of paused, start it
                                Message msg = Message.obtain(null, PlayBackService.MSG_START);
                                msg.arg1 = 1;
                                try {
                                    mPlayback.send(msg);
                                } catch (RemoteException e) {
                                    Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                                }
                            }else if (rdata.getAsInteger("player_status") == PlayBackService.STA_STARTED){
                                // Status of playing, paused it
                                Message msg = Message.obtain(null, PlayBackService.MSG_PAUSE);
                                try {
                                    mPlayback.send(msg);
                                } catch (RemoteException e) {
                                    Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                                }
                            }
                        }
                    }
                });
                convertView.setTag(holder);
            }else{
                holder = (Holder)convertView.getTag();
            }
            // ui update
            if (isNovelMode()){
                if (data.getAsInteger("item_mode") == 1){

                    holder.esm.setVisibility(View.GONE);
                    // playback mode
                    holder.plm.setVisibility(View.VISIBLE);
                    holder.plm_playerbt.setTag(data);
                    holder.plm_seekbar.setTag(data);
                    //
                    holder.plm_title.setText(data.getAsString(Record.NAME));
                    holder.plm_rec_nj.setText(data.getAsString(Record.NJNAME));

                    int lefttime = data.getAsInteger("player_duration") - data.getAsInteger("player_curpos");
                    if (lefttime != 0) {
                        String playtime = "- " + coverMs2Str(lefttime);
                        holder.plm_rec_timer.setText(playtime);
                    }else
                        holder.plm_rec_timer.setText("-00:00");

                    // set imgb here
                    if (data.getAsInteger("player_status") == PlayBackService.STA_IDLE){
                        holder.plm_playerbt.setImageResource(R.drawable.play);
                    }else if (data.getAsInteger("player_status") == PlayBackService.STA_ERROR){
                        holder.plm_playerbt.setImageResource(R.drawable.play_err);
                    }else if (data.getAsInteger("player_status") == PlayBackService.STA_PAUSED
                            || data.getAsInteger("player_status") == PlayBackService.STA_COMPLETED){
                        holder.plm_playerbt.setImageResource(R.drawable.play);
                    }else if (data.getAsInteger("player_status") == PlayBackService.STA_STARTED){
                        holder.plm_playerbt.setImageResource(R.drawable.pause);
                    }else if (data.getAsInteger("player_status") == PlayBackService.STA_PREPARING){
                        holder.plm_playerbt.setImageResource(R.drawable.play_in_perpare);
                    }

                    holder.plm_seekbar.setMax(data.getAsInteger("player_duration"));
                    holder.plm_seekbar.setProgress(data.getAsInteger("player_curpos"));
                    holder.plm_seekbar.setSecondaryProgress(data.getAsInteger("player_buffer"));


                }else{
                    // easy mode

                    holder.esm.setVisibility(View.VISIBLE);

                    holder.esm_title.setText(data.getAsString(Record.NAME));
                    if (mExtendItemPos > 0){
                        // some item has extended grad it
                        holder.esm_title.setTextColor(getResources().getColor(R.color.esm_item_grey));
                    }else{
                        holder.esm_title.setTextColor(getResources().getColor(R.color.esm_item_black));
                    }

                    if (data.getAsInteger("player_status") == PlayBackService.STA_IDLE){
                        if (data.getAsInteger(Record.READ) == 0)
                            holder.esm_status.setImageResource(R.drawable.esm_unread);
                        else
                            holder.esm_status.setImageResource(R.drawable.esm_read);
                    }else if (data.getAsInteger("player_status") == PlayBackService.STA_ERROR){
                        holder.esm_status.setImageResource(R.drawable.esm_play_err);
                    }else{
                        holder.esm_status.setImageResource(R.drawable.esm_play);
                    }

                    holder.plm.setVisibility(View.GONE);

                    //holder.plm_title.setText(data.getAsString(Record.NAME));
                }
            }
            return convertView;
        }

        class Holder {
            View esm;
            View plm;
            TextView esm_title;
            ImageView esm_status;

            TextView plm_title;
            ImageButton plm_playerbt;
            SeekBar plm_seekbar;
            TextView plm_rec_nj;
            //TextView plm_rec_updated;
            TextView plm_rec_timer;
           // TextView plm_playtime;
        }

        //public Integer mExtendItemPos = 0;
    }

    class PlayingListHandler extends Handler {
        @Override
        public void handleMessage(Message msg){
            int itemid = 0;
            int itempos = 0;
            int validpos = 0;
            PlayBackService.Status status = null;
            ContentValues data = null;
            switch(msg.what){
                case MSG_ON_CURRENT_STATUS:
                    status = (PlayBackService.Status)msg.obj;
                    if (status.status == PlayBackService.STA_PAUSED){
                        mPlaybackItem.setVisible(true);
                        mPlaybackItem.setIcon(R.drawable.actionbar_start);
                    }else if (status.status == PlayBackService.STA_STARTED){
                        mPlaybackItem.setVisible(true);
                        mPlaybackItem.setIcon(R.drawable.actionbar_pause);
                    }else{
                        mPlaybackItem.setVisible(false);
                    }
                    mPlaybackItem.getIntent().putExtra("player_status", status.status);
                    break;
                case MSG_ON_MP3STA_UPDATE:
                    status = (PlayBackService.Status)msg.obj;
                    if (status.status == PlayBackService.STA_ERROR){
                        itemid = status.itemId;
                        itempos = status.itemPos;
                        validpos = getValidPos(itemid, itempos);
                        if (validpos >=0) {
                            data = mPlaying_data.get(validpos);
                            data.put("player_status", status.status);
                        }
                        // update listview
                        if (isVisiblePosition(validpos))
                            mMyadapter.notifyDataSetChanged();

                        mPlaybackItem.setVisible(false);

                        Toast.makeText(PlayingListActivity.this, R.string.dl_server_err, Toast.LENGTH_SHORT).show();
                    }else if (status.status == PlayBackService.STA_COMPLETED){

                        itemid = status.itemId;
                        itempos = status.itemPos;
                        validpos = getValidPos(itemid, itempos);
                        if (validpos >= 0) {
                            data = mPlaying_data.get(validpos);
                            data.put("player_status", status.status);
                        }
                        //update listview
                        if (isVisiblePosition(validpos))
                            mMyadapter.notifyDataSetChanged();
                        mPlaybackItem.setVisible(false);
                    }else if(status.status == PlayBackService.STA_READY){
                        // if get ready tell the playback start playing
                        Message ms = Message.obtain(null, PlayBackService.MSG_START);
                        // make the player progress bar work!
                        ms.arg1 = 1;
                        try {
                            mPlayback.send(ms);
                        } catch (RemoteException e) {
                            Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                        }

                    }else if (status.status == PlayBackService.STA_STARTED){

                        itemid = status.itemId;
                        itempos = status.itemPos;
                        validpos = getValidPos(itemid, itempos);

                        if (validpos >=0) {
                            data = mPlaying_data.get(validpos);
                            data.put("player_status",  status.status);
                            data.put("player_curpos", status.position);
                            data.put("player_duration", status.duration);
                            if (data.getAsInteger(Record.READ) == 0) {
                                data.put(Record.READ, 1);
                                new Record().updateRead(PlayingListActivity.this, data.getAsInteger(Record.ID));
                            }
                        }
                        // update listview
                        if (isVisiblePosition(validpos))
                            mMyadapter.notifyDataSetChanged();

                        mPlaybackItem.setVisible(true);
                        mPlaybackItem.setIcon(R.drawable.actionbar_pause);
                        mPlaybackItem.getIntent().putExtra("player_status", status.status);

                    }else if (status.status == PlayBackService.STA_PAUSED){
                        itemid = status.itemId;
                        itempos = status.itemPos;
                        validpos = getValidPos(itemid, itempos);
                        if (validpos >=0) {
                            data = mPlaying_data.get(validpos);
                            data.put("player_status", (Integer) status.status);
                        }

                        // update listview
                        if (isVisiblePosition(validpos))
                            mMyadapter.notifyDataSetChanged();

                        mPlaybackItem.setVisible(true);
                        mPlaybackItem.setIcon(R.drawable.actionbar_start);
                        mPlaybackItem.getIntent().putExtra("player_status", status.status);

                    }else if (status.status == PlayBackService.STA_PREPARING){
                        itemid = status.itemId;
                        itempos = status.itemPos;
                        validpos = getValidPos(itemid, itempos);
                        if (validpos >=0) {
                            data = mPlaying_data.get(validpos);
                            data.put("player_status", (Integer) status.status);
                        }
                        // update listview
                        if (isVisiblePosition(validpos))
                            mMyadapter.notifyDataSetChanged();
                        mPlaybackItem.setVisible(false);
                    }
                    break;
                case MSG_ON_CLEAN_UP_ITEM_UI:
                    itemid = msg.arg1;
                    itempos = msg.arg2;
                    validpos = getValidPos(itemid, itempos);
                    if (validpos >=0) {
                        data = mPlaying_data.get(validpos);
                        data.put("item_mode", 0);
                        data.put("player_status", PlayBackService.STA_IDLE);
                        data.put("player_curpos", 0);
                        data.put("player_duration", 0);
                        data.put("player_buffer", 0);
                    }
                    if (isVisiblePosition(validpos)){
                        mMyadapter.notifyDataSetChanged();
                    }
                    break;
                case MSG_ON_MP3PROGRESS_UPDTED:
                    status = (PlayBackService.Status) msg.obj;
                    itemid = status.itemId;
                    itempos = status.itemPos;
                    validpos = getValidPos(itemid, itempos);
                    if (validpos >=0) {
                        data = mPlaying_data.get(validpos);
                        data.put("player_curpos", status.position);
                        data.put("player_duration", status.duration);
                        int bufferpos = (int) (status.buffer_percent * 0.01 * status.duration);
                        data.put("player_buffer", bufferpos);
                    }
                    if (isVisiblePosition(validpos))
                        mMyadapter.notifyDataSetChanged();

                    break;
                case MSG_ON_LOAD_RECORD_LIST_DONE:
                    mPlaying_data = (List<ContentValues>)msg.obj;
                    mPullToRefreshListView.onRefreshComplete();
                    mMyadapter.notifyDataSetChanged();
                    mExtendItemPos = -1;
                    break;
                case MSG_ON_DOWNLOAD_PROGRESS:
                    itemid = msg.arg1;
                    itempos = msg.arg2;
                    validpos = getValidPos(itemid, itempos);
                    if (validpos >=0 ) {
                        data = mPlaying_data.get(validpos);
                        data.put("progress", (Integer) msg.obj);
                        data.put(Downloader.STATUS, Downloader.STA_STARTED);
                    }
                    if (isVisiblePosition(validpos))
                        mMyadapter.notifyDataSetChanged();
                    break;
                case MSG_ON_DOWNLOAD_DONE:
                    itemid = msg.arg1;
                    itempos = msg.arg2;
                    validpos = getValidPos(itemid, itempos);
                    if (validpos >= 0) {
                        data = mPlaying_data.get(validpos);
                        data.put(Downloader.STATUS, (Integer) msg.obj);
                    }
                    if (isVisiblePosition(validpos))
                        mMyadapter.notifyDataSetChanged();

                    break;
                case MSG_ON_DOWNLOAD_PAUSED:
                    itemid  = msg.arg1;
                    itempos = msg.arg2;
                    validpos = getValidPos(itemid, itempos);
                    if (validpos >=0 ) {
                        data = mPlaying_data.get(validpos);
                        data.put(Downloader.STATUS, (Integer) msg.obj);
                    }
                    if (isVisiblePosition(validpos)){
                        mMyadapter.notifyDataSetChanged();
                    }
                    break;
                case MSG_ON_DOWNLOAD_ERR:
                    itemid  = msg.arg1;
                    itempos = msg.arg2;
                    validpos = getValidPos(itemid, itempos);
                    if (validpos >=0 ) {
                        data = mPlaying_data.get(validpos);
                        data.put(Downloader.STATUS, (Integer) msg.obj);
                    }
                    if (isVisiblePosition(validpos)){
                        mMyadapter.notifyDataSetChanged();
                    }
                    break;
                case MSG_ON_DOWNLOAD_STARTED:
                    itemid  = msg.arg1;
                    itempos = msg.arg2;
                    validpos = getValidPos(itemid, itempos);
                    if (validpos >=0) {
                        data = mPlaying_data.get(validpos);
                        data.put(Downloader.STATUS, (Integer) msg.obj);
                    }
                    if (isVisiblePosition(validpos)){
                        mMyadapter.notifyDataSetChanged();
                    }
                    break;
                case MSG_ON_DOWNLOAD_DELETE:
                    mMyadapter.notifyDataSetChanged();
                    break;
                case MSG_ON_EXTEND_ITEM:
                    mMyadapter.notifyDataSetChanged();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    class LoadAvatarTask extends AsyncTask<String, Void, Integer> {

        public LoadAvatarTask(CircleImageView c, String njid){
            this.cimg = c;
            this.njid = njid;
        }

        @Override
        protected Integer doInBackground(String[] params) {

            try {
                URL url = new URL(params[0]);
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
                String avatar = PlayingListActivity.this.getFilesDir().getAbsolutePath()+"/avatar_"+njid+".jpg";
                Bitmap bmp = BitmapFactory.decodeFile(avatar);
                cimg.setImageBitmap(bmp);
            }
        }
        CircleImageView cimg;
        String njid;
    }

    class HeadHolder {
        ScaleImageView nvl_bg_im;
        ScaleImageView nvl_wbg_im;
        CircleImageView nj_avatar_img;
        TextView nvl_author_tx;
        TextView nj_name_tx;
        ImageButton nvl_comments_bt;
    }
}
