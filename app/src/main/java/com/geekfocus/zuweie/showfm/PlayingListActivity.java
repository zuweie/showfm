package com.geekfocus.zuweie.showfm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
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

import com.avos.avoscloud.AVObject;
import com.dodowaterfall.widget.ScaleImageView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.api.share.IWeiboHandler;
import com.sina.weibo.sdk.api.share.IWeiboShareAPI;
import com.sina.weibo.sdk.api.share.SendMultiMessageToWeiboRequest;
import com.sina.weibo.sdk.api.share.WeiboShareSDK;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.exception.WeiboException;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.modelmsg.WXMediaMessage;
import com.tencent.mm.sdk.modelmsg.WXMusicObject;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;
import com.umeng.analytics.MobclickAgent;

import java.security.NoSuchAlgorithmException;
import java.util.List;


public class PlayingListActivity extends Activity implements IWeiboHandler.Response{

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
    private Integer mExtendItemPos = -1;
    private Integer mscrolltoPos = -1;
    private MenuItem mPlaybackItem;

    // share data //
    IWeiboShareAPI mWeiboShareApi = null;
    IWXAPI         mWxApi         = null;
    //private HeadHolder mHeaderHolder;
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
                    // 让 service 自己决定是否 进行 refresh
                    msg.arg2 = 0;
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
    public final static int MSG_ON_PREPARING_DOWNLOAD = 5;
    public final static int MSG_ON_EXTEND_ITEM = 10;
    public final static int MSG_ON_MP3PROGRESS_UPDTED = 11;
    public final static int MSG_ON_MP3BUFFERING_UPDATED = 12;
    public final static int MSG_ON_CURRENT_STATUS = 13;
    public final static int MSG_ON_CLEAN_UP_ITEM_UI = 14;
    public final static int MSG_ON_SCROLLTO_POS = 15;
    public final static int MSG_SHOWUP_YYT = 16;
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

        /* init sina share data */
        mWeiboShareApi = WeiboShareSDK.createWeiboAPI(this, Myfunc.getValidText(MyConstant.WEIBO_APP_KEY));
        mWeiboShareApi.registerApp();
        if (savedInstanceState != null){
            mWeiboShareApi.handleWeiboResponse(getIntent(), this);
        }
        /* end init sina share data */

        /*init weixin api */
        mWxApi = WXAPIFactory.createWXAPI(this, MyConstant.WX_APP_ID);
        Log.d("wx_regist", String.valueOf(mWxApi.registerApp(MyConstant.WX_APP_ID)));
        /*end init weixin api */

        /* init the UI */
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
                    // 强制 service 去 refresh
                    msg.arg2 = 1;
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

    @Override
    protected  void onNewIntent(Intent intent){
        super.onNewIntent(intent);
        mWeiboShareApi.handleWeiboResponse(intent, this);
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
    protected void onDestroy () {
        mWxApi.unregisterApp();
        super.onDestroy();
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

        }else if (id == R.id.action_comment){
            Intent it = new Intent(PlayingListActivity.this, NovelCommentActivity.class);
            it.putExtra("nvl", getIntent().getIntExtra("nvl", -1));
            startActivity(it);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResponse(BaseResponse baseResponse) {
        // TODO : handle the response result
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

        if (position >= first-1 && position <= last)
            return true;
        else
            return false;

    }

    private String coverMs2Str(int ms){
        int second = (ms / 1000) % 60;
        int min    = (ms / 1000) / 60;
        return ""+(min>=10?min:"0"+min)+":"+(second>=10?second:"0"+second);
    }

    public static class DownloadSelectDialog extends DialogFragment{
        /*
        public CharSequence[] getSelectText(int r){

            CharSequence[] ts = getActivity().getResources().getTextArray(r);

            if (yyt == null){
                return ts;
            }else{
                CharSequence[] newtx = new CharSequence[ts.length+1];

                for(int i=0; i<ts.length; ++i){
                    newtx[0] = ts[i];
                }
                newtx[ts.length] = yyt.getAsString("name");
                return newtx;
            }

        }
        */
        @Override
        public Dialog onCreateDialog(Bundle saveInstanceState){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle(R.string.download_select_dlg_title);
            builder.setNegativeButton(R.string.negative, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            if (rdata.getAsInteger(Record.DOWNLOADID) < 0){
                // new a downloadtask
                builder.setItems(R.array.download_idle_option, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Record r = new Record();
                        r.createDownloader(getActivity(), rdata);
                        //rdata.put(Downloader.STATUS, Downloader.STA_PREPARING);
                        Message msg = Message.obtain();
                        try {
                            switch (which){
                                case 0:
                                    // start download
                                    msg = Message.obtain();
                                    params[0] = rdata.getAsInteger(Record.NOVELID);
                                    params[1] = rdata.getAsInteger(Record.DOWNLOADID);
                                    params[2] = 0;
                                    msg.obj = params;
                                    msg.arg1 = rdata.getAsInteger(Record.ID);
                                    msg.arg2 = rdata.getAsInteger("item_pos");
                                    msg.what = PlayBackService.MSG_START_DOWNLOAD_REC;
                                    playback.send(msg);
                                    downloadtx.setClickable(false);
                                    downloadtx.setTextColor(getActivity().getResources().getColor(R.color.plm_download_tx_disable));
                                    break;
                                case 1:
                                    msg = Message.obtain(null, MSG_SHOWUP_YYT);
                                    itslft.send(msg);
                                    break;
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }else if(rdata.getAsInteger(Downloader.STATUS) == Downloader.STA_STARTED){
                builder.setItems(R.array.download_started_option, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try{
                            Message msg = Message.obtain();
                            switch (which){
                                case 0:
                                    // TODO : paused
                                    msg.obj = new Integer(rdata.getAsString(Record.DOWNLOADID));
                                    msg.what = PlayBackService.MSG_PAUSE_DOWNLOAD_REC;
                                    playback.send(msg);
                                    break;
                                /*
                                case 1:
                                    // TODO : delete the file
                                    msg.obj = new Integer(rdata.getAsInteger(Record.DOWNLOADID));
                                    msg.arg1 = rdata.getAsInteger(Record.ID);
                                    msg.arg2 = rdata.getAsInteger("item_pos");
                                    msg.what = PlayBackService.MSG_DELETE_DOWNLOAD_REC;
                                    break;
                                    */
                                case 1:
                                    msg = Message.obtain(null, MSG_SHOWUP_YYT);
                                    itslft.send(msg);
                                    break;
                            }

                        }catch(RemoteException e){}
                    }
                });
            }else if (rdata.getAsInteger(Downloader.STATUS) == Downloader.STA_PAUSED || rdata.getAsInteger(Downloader.STATUS) == Downloader.STA_IDLE){
                builder.setItems(R.array.download_paused_option, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Message msg = Message.obtain();
                        try{
                            switch (which){
                                case 0:
                                    // TODO : started
                                    params[0] = rdata.getAsInteger(Record.NOVELID);
                                    params[1] = rdata.getAsInteger(Record.DOWNLOADID);
                                    params[2] = 0;
                                    msg.obj = params;
                                    msg.arg1 = rdata.getAsInteger(Record.ID);
                                    msg.arg2 = rdata.getAsInteger("item_pos");
                                    msg.what = PlayBackService.MSG_START_DOWNLOAD_REC;
                                    downloadtx.setClickable(false);
                                    downloadtx.setTextColor(getActivity().getResources().getColor(R.color.plm_download_tx_disable));
                                    playback.send(msg);
                                    break;
                                case 1:
                                    // TODO : delete the file
                                    msg.obj = new Integer(rdata.getAsInteger(Record.DOWNLOADID));
                                    msg.arg1 = rdata.getAsInteger(Record.ID);
                                    msg.arg2 = rdata.getAsInteger("item_pos");
                                    msg.what = PlayBackService.MSG_DELETE_DOWNLOAD_REC;
                                    playback.send(msg);
                                    break;
                                case 2:
                                    msg = Message.obtain(null, MSG_SHOWUP_YYT);
                                    itslft.send(msg);
                                    break;
                            }

                            //dialog.dismiss();
                        }catch (RemoteException e){}
                    }
                });
            }else if (rdata.getAsInteger(Downloader.STATUS) == Downloader.STA_DONE){

                builder.setItems(R.array.download_done_option, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // TODO : delete

                        try {
                            switch (which) {
                                case 0:
                                    Message msg = Message.obtain();
                                    msg.obj = new Integer(rdata.getAsInteger(Record.DOWNLOADID));
                                    msg.arg1 = rdata.getAsInteger(Record.ID);
                                    msg.arg2 = rdata.getAsInteger("item_pos");
                                    msg.what = PlayBackService.MSG_DELETE_DOWNLOAD_REC;
                                    playback.send(msg);
                                    break;
                                case 1:
                                    msg = Message.obtain(null, MSG_SHOWUP_YYT);
                                    itslft.send(msg);
                                    break;
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }else if (rdata.getAsInteger(Downloader.STATUS) == Downloader.STA_ERR){

                builder.setItems(R.array.download_err_option, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Message msg = Message.obtain();
                        try{
                            switch (which){
                                case 0:
                                    // TODO : started again
                                    params[0] = rdata.getAsInteger(Record.NOVELID);
                                    params[1] = rdata.getAsInteger(Record.DOWNLOADID);
                                    params[2] = 1;
                                    msg.obj = params;
                                    msg.arg1 = rdata.getAsInteger(Record.ID);
                                    msg.arg2 = rdata.getAsInteger("item_pos");
                                    msg.what = PlayBackService.MSG_START_DOWNLOAD_REC;
                                    playback.send(msg);
                                    downloadtx.setClickable(false);
                                    downloadtx.setTextColor(getActivity().getResources().getColor(R.color.plm_download_tx_disable));
                                    break;
                                case 1:
                                    // TODO : delete the file
                                    msg.obj = new Integer(rdata.getAsInteger(Record.DOWNLOADID));
                                    msg.arg1 = rdata.getAsInteger(Record.ID);
                                    msg.arg2 = rdata.getAsInteger("item_pos");
                                    msg.what = PlayBackService.MSG_DELETE_DOWNLOAD_REC;
                                    playback.send(msg);
                                    break;
                                case 2:
                                    msg = Message.obtain(null, MSG_SHOWUP_YYT);
                                    itslft.send(msg);
                                    break;
                            }
                        }catch (RemoteException e){}
                    }
                });
            }
            return builder.create();
        }

        //public int download_status;
        //public int downloadid;
        public Messenger playback;
        public Messenger itslft;
        public ContentValues rdata;
        public ContentValues yyt;
        public TextView downloadtx;
        Integer[] params = new Integer[3];
    }

    public static class YytDialogFragmeng extends  DialogFragment {

        @Override
        public Dialog onCreateDialog (Bundle saveInstanceState){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.yyt_dlg, null);
            poster = (ScaleImageView) view.findViewById(R.id.yyt_poster);
            poster.setImageHeight(600);
            poster.setImageWidth(400);
            TextView  desc   = (TextView) view.findViewById(R.id.yyt_desc);
            builder.setTitle(yyt.getAsString("name")+"(广告)");

            NetBitMap netbmp = new NetBitMap(getActivity(), yyt.getAsString("poster"), R.drawable.yyt_logo);
            poster.setImageBitmap(netbmp.getBitmap(new NetBitMap.LoadBitmapCallback() {
                @Override
                public void done(Bitmap bm, Object error) {
                    if (bm != null)
                    poster.setImageBitmap(bm);
                }
            }));
            poster.setTag(yyt.getAsString("wdi"));
            poster.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = (String) v.getTag();
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    Uri content_url = Uri.parse(url);
                    intent.setData(content_url);
                    getActivity().startActivity(intent);
                    YytDialogFragmeng.this.dismiss();
                }
            });

            desc.setText("      " + yyt.getAsString("desc"));
            desc.setTag(yyt.getAsString("wdi"));
            desc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = (String) v.getTag();
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    Uri content_url = Uri.parse(url);
                    intent.setData(content_url);
                    getActivity().startActivity(intent);
                    YytDialogFragmeng.this.dismiss();
                }
            });
            builder.setNegativeButton(R.string.negative, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.setView(view);
            return builder.create();
        }
        ContentValues yyt;
        // ui
        ScaleImageView poster;

    }

    public static class ShareDialogFragment extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View view = inflater.inflate(R.layout.share_dlg, null);

            // weibo share
            ImageButton weibo_bt = (ImageButton) view.findViewById(R.id.weibo_share);
            weibo_bt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Oauth2AccessToken accessToken = AccessTokenkeeper.readAccessToken(getActivity().getApplicationContext());

                    if (accessToken != null && accessToken.isSessionValid()){

                        AlertDialog dialog = (AlertDialog)v.getTag();
                        WeiboMultiMessage message = new WeiboMultiMessage();

                        String url = "http://yjh.geekfocus.cc/index.php/Showfm/Public/record?rid="+rec_id;
                        String app_url = "http://www.showfm.net/app/app_down.asp";
                        String format = getResources().getString(R.string.share_format);
                        String sharetext = String.format(format, nvl_name+" - "+rec_name, url, app_url);
                        //Log.d("weiboapi", String.valueOf(weiboShareAPI.getWeiboAppSupportAPI()));
                        if (weiboShareAPI.getWeiboAppSupportAPI() >= 10351){
                            // multi message
                            // create the text obj
                            TextObject textObject = new TextObject();
                            textObject.text = sharetext;
                            message.textObject = textObject;

                            // create the img
                            try {
                                String fbmp = getActivity().getCacheDir() + "/" + Myfunc.md5(nvl_pic) + ".jpg";
                                Bitmap bitmap = BitmapFactory.decodeFile(fbmp);
                                if (bitmap != null) {
                                    ImageObject imageObject = new ImageObject();
                                    imageObject.setThumbImage(bitmap);
                                    imageObject.actionUrl = url;
                                    message.imageObject = imageObject;
                                }
                            }catch (NoSuchAlgorithmException e){}

                        }else{
                            // single message
                            TextObject textObject = new TextObject();
                            textObject.text = sharetext;
                            message.mediaObject = textObject;
                        }


                        SendMultiMessageToWeiboRequest request = new SendMultiMessageToWeiboRequest();
                        request.transaction = String.valueOf(System.currentTimeMillis());
                        request.multiMessage = message;

                        AuthInfo authInfo = new AuthInfo(getActivity(), Myfunc.getValidText(MyConstant.WEIBO_APP_KEY), MyConstant.WEIBO_REDIRECT_URL, MyConstant.WEIBO_SCOPE);

                        weiboShareAPI.sendRequest(getActivity(), request, authInfo, accessToken.getToken(), new WeiboAuthListener(){
                            @Override
                            public void onComplete(Bundle bundle) {
                                Oauth2AccessToken newToken = Oauth2AccessToken.parseAccessToken(bundle);
                                AccessTokenkeeper.writeAccessToken(getActivity().getApplicationContext(), newToken);
                            }

                            @Override
                            public void onWeiboException(WeiboException e) {

                            }

                            @Override
                            public void onCancel() {

                            }
                        });
                        dialog.dismiss();
                    }else{
                        // TODO : showfm had no login sina weibo
                        Toast.makeText(getActivity(), R.string.login_with_sina, Toast.LENGTH_SHORT).show();
                    }
                }
            });

            // wx share
            ImageButton wx_bt_tl = (ImageButton) view.findViewById(R.id.wx_share_timeline);
            wx_bt_tl.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    AlertDialog dialog = (AlertDialog)v.getTag();
                    //TODO : share the rec to weixin
                    //Log.d("external_storage", state);
                    WXMusicObject musicObject = new WXMusicObject();
                    musicObject.musicUrl = "yjh.geekfocus.cc/index.php/Showfm/Public/record?rid="+rec_id;
                    long etime = 60 * 60 * 24 * 3; // 3天的有效期
                    try {
                        musicObject.musicDataUrl = Myfunc.getValidUrl(nvl_folder+"/", rec_url, (int)etime);
                    } catch (NoSuchAlgorithmException e) {}

                    WXMediaMessage message = new WXMediaMessage();
                    message.mediaObject = musicObject;
                    message.description = nvl_name + " - "+rec_name;


                    Bitmap bmp = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.showfm_64);
                    if (bmp != null)
                        message.thumbData = Myfunc.bmpToByteArray(bmp);

                    SendMessageToWX.Req req = new SendMessageToWX.Req();
                    req.transaction = "music"+String.valueOf(System.currentTimeMillis());
                    req.scene = SendMessageToWX.Req.WXSceneTimeline;
                    req.message = message;
                    Log.d("wx_send_request", String.valueOf(wxApi.sendReq(req)));
                    dialog.dismiss();
                }
            });

            ImageButton wx_bt = (ImageButton) view.findViewById(R.id.wx_share);
            wx_bt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog dialog = (AlertDialog)v.getTag();
                    //TODO : share the rec to weixin
                    //Log.d("external_storage", state);
                    WXMusicObject musicObject = new WXMusicObject();
                    musicObject.musicUrl = "yjh.geekfocus.cc/index.php/Showfm/Public/record?rid="+rec_id;
                    long etime = 60 * 60 * 24 * 3; // 3天的有效期
                    try {
                        musicObject.musicDataUrl = Myfunc.getValidUrl(nvl_folder+"/", rec_url, (int)etime);
                    } catch (NoSuchAlgorithmException e) {}

                    WXMediaMessage message = new WXMediaMessage();
                    message.mediaObject = musicObject;
                    message.description = nvl_name + " - "+rec_name;


                    Bitmap bmp = BitmapFactory.decodeResource(getActivity().getResources(), R.drawable.showfm_64);
                    if (bmp != null)
                        message.thumbData = Myfunc.bmpToByteArray(bmp);

                    SendMessageToWX.Req req = new SendMessageToWX.Req();
                    req.transaction = "music"+String.valueOf(System.currentTimeMillis());
                    req.scene = SendMessageToWX.Req.WXSceneSession;
                    req.message = message;
                    Log.d("wx_send_request", String.valueOf(wxApi.sendReq(req)));
                    dialog.dismiss();
                }
            });

            // facebook share
            /* comment the facebook code, can not be use in china,Fuck!!
            ShareButton fb_bt = (ShareButton) view.findViewById(R.id.fb_share);
            ShareLinkContent shareLinkContent = new ShareLinkContent.Builder()
                                               .setContentDescription(rec_name)
                                               .setContentTitle(nvl_name)
                                               .setImageUrl(Uri.parse(nvl_pic))
                                               .setContentUrl(Uri.parse("http://yjh.geekfocus.cc/index.php/Showfm/Public/record?rid="+rec_id))
                                               .build();
            fb_bt.setShareContent(shareLinkContent);
            fb_bt.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog dialog = (AlertDialog)v.getTag();
                    dialog.dismiss();
                }
            });
            //facebook share
            */
            builder.setView(view)
            .setNegativeButton(R.string.negative, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
            .setTitle(R.string.share_dlg_title);
            AlertDialog dialog = builder.create();
            weibo_bt.setTag(dialog);
            wx_bt.setTag(dialog);
            wx_bt_tl.setTag(dialog);
            /* comment the facebook code, can not be use in china,Fuck!!
            fb_bt.setTag(dialog);
            */
            return dialog;
        }

        public String rec_name;
        public int rec_id;
        public String rec_url;
        public String nvl_name;
        public String nvl_pic;
        public String nvl_folder;
        //public Activity activity;
        IWeiboShareAPI weiboShareAPI;
        IWXAPI         wxApi;
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
                // share text
                holder.plm_share_tx  = (TextView)convertView.findViewById(R.id.plm_share_tx);
                holder.plm_share_tx.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // todo new show a share dialog here
                        ContentValues data = (ContentValues) v.getTag();
                        ShareDialogFragment dialogFragment = new ShareDialogFragment();
                        dialogFragment.rec_id = data.getAsInteger("rec_id");
                        dialogFragment.rec_name = data.getAsString("rec_name");
                        dialogFragment.rec_url  = data.getAsString("rec_url");
                        dialogFragment.nvl_name = data.getAsString("nvl_name");
                        dialogFragment.nvl_pic = data.getAsString("nvl_pic");
                        dialogFragment.nvl_folder = data.getAsString("nvl_folder");
                        dialogFragment.weiboShareAPI = mWeiboShareApi;
                        dialogFragment.wxApi         = mWxApi;
                        dialogFragment.show(getFragmentManager(), "shareDlg");
                    }
                });
                holder.plm_share_tx.setTag(new ContentValues());
                holder.plm_share_tx.getPaint().setUnderlineText(true);
                // share text

                // download text
                holder.plm_download_tx = (TextView)convertView.findViewById(R.id.plm_download_tx);
                holder.plm_download_tx.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ContentValues rdata = (ContentValues)v.getTag();
                        // show up the dialog. tell what to do next
                        if (rdata.getAsInteger(Record.DOWNLOADID) > 0 && rdata.getAsInteger(Downloader.STATUS) == Downloader.STA_PREPARING){
                            Toast.makeText(PlayingListActivity.this, R.string.download_preparing, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        DownloadSelectDialog downloadDlgFragment = new DownloadSelectDialog();
                        downloadDlgFragment.playback = mPlayback;
                        downloadDlgFragment.itslft   = mItSelf;
                        downloadDlgFragment.rdata = rdata;
                        downloadDlgFragment.downloadtx = (TextView)v;
                        //downloadDlgFragment.yyt = Myfunc.getYytRandom();
                        downloadDlgFragment.show(getFragmentManager(), "downloadDlg");
                    }
                });
                holder.plm_download_tx.getPaint().setUnderlineText(true);
                // end download text
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


                        if (isNovelMode()) {
                            ContentValues rdata = (ContentValues) v.getTag();
                            if (rdata.getAsInteger("player_status") == PlayBackService.STA_IDLE
                                    || rdata.getAsInteger("player_status") == PlayBackService.STA_ERROR) {

                                // showup yyt
                                ContentValues yyt = Myfunc.getYytRandom();
                                if (yyt != null){
                                    YytDialogFragmeng yytDlg = new YytDialogFragmeng();
                                    yytDlg.yyt = yyt;
                                    yytDlg.show(getFragmentManager(), "yytdlg");
                                }

                                try {
                                    Message msg = null;
                                    if (rdata.getAsInteger(Record.DOWNLOADID) > 0) {
                                        // it can be played locally
                                        if (rdata.getAsInteger(Downloader.STATUS) == Downloader.STA_DONE) {
                                            msg = Message.obtain(null, PlayBackService.MSG_PLAY_LOCAL);
                                            msg.obj = rdata.getAsString(Downloader.PATH) + rdata.getAsString(Downloader.FILENAME) + " " + rdata.getAsInteger(Record.NOVELID);
                                            msg.arg1 = rdata.getAsInteger(Record.ID);
                                            msg.arg2 = rdata.getAsInteger("item_pos");
                                            mPlayback.send(msg);
                                            Toast.makeText(PlayingListActivity.this, R.string.play_local, Toast.LENGTH_SHORT).show();
                                            return;
                                        } else if (rdata.getAsInteger(Downloader.STATUS) == Downloader.STA_STARTED
                                                || rdata.getAsInteger(Downloader.STATUS) == Downloader.STA_PAUSED) {
                                            Toast.makeText(PlayingListActivity.this, R.string.downloading_tips, Toast.LENGTH_SHORT).show();
                                            return;
                                        }
                                    }

                                    // Status of idle or error, play it
                                    String url = rdata.getAsString(Record.URL);
                                    String[] param = new String[2];

                                    url = Myfunc.getValidUrl(mNovelFolder + "/", url, 1800);
                                    msg = Message.obtain(null, PlayBackService.MSG_PLAY);
                                    msg.obj = url + " " + rdata.getAsInteger(Record.NOVELID);
                                    msg.arg1 = rdata.getAsInteger(Record.ID);
                                    msg.arg2 = rdata.getAsInteger("item_pos");
                                    mPlayback.send(msg);
                                    Toast.makeText(PlayingListActivity.this, R.string.play_remote, Toast.LENGTH_SHORT).show();
                                    // add the play item count
                                    AVObject playingcount = new AVObject("Playing_counter");
                                    playingcount.put("novelId", mNovelId);
                                    playingcount.put("recordId", rdata.getAsInteger(Record.ID));

                                    if (MyLogin.getInstance().getAVOSUser() != null) {
                                        playingcount.put("user", MyLogin.getInstance().getAVOSUser());
                                    }
                                    playingcount.saveInBackground();

                                } catch (NoSuchAlgorithmException e) {
                                } catch (RemoteException e) {
                                    Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                                }
                            } else if (rdata.getAsInteger("player_status") == PlayBackService.STA_PAUSED
                                    || rdata.getAsInteger("player_status") == PlayBackService.STA_COMPLETED) {
                                // Status of paused, start it
                                Message msg = Message.obtain(null, PlayBackService.MSG_START);
                                msg.arg1 = 1;
                                try {
                                    mPlayback.send(msg);
                                } catch (RemoteException e) {
                                    Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                                }
                            } else if (rdata.getAsInteger("player_status") == PlayBackService.STA_STARTED) {
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
                        // stop the loading icon
                        stopLoadingIcon(holder);
                        holder.plm_playerbt.setImageResource(R.drawable.play);
                    }else if (data.getAsInteger("player_status") == PlayBackService.STA_ERROR){
                        // stop the loading icon
                        stopLoadingIcon(holder);
                        holder.plm_playerbt.setImageResource(R.drawable.play_err);
                    }else if (data.getAsInteger("player_status") == PlayBackService.STA_PAUSED
                            || data.getAsInteger("player_status") == PlayBackService.STA_COMPLETED){
                        // stop the loading icon
                        stopLoadingIcon(holder);
                        holder.plm_playerbt.setImageResource(R.drawable.play);
                    }else if (data.getAsInteger("player_status") == PlayBackService.STA_STARTED){
                        // stop the loading icon
                        stopLoadingIcon(holder);
                        holder.plm_playerbt.setImageResource(R.drawable.pause);
                    }else if (data.getAsInteger("player_status") == PlayBackService.STA_PREPARING){
                       // holder.plm_playerbt.setImageResource(R.drawable.play_in_perpare);
                        if (holder.recLoading == null){
                            holder.plm_playerbt.setImageResource(R.drawable.rec_load);
                            holder.recLoading = (AnimationDrawable) holder.plm_playerbt.getDrawable();
                            holder.recLoading.start();
                        }
                    }

                    holder.plm_seekbar.setMax(data.getAsInteger("player_duration"));
                    holder.plm_seekbar.setProgress(data.getAsInteger("player_curpos"));
                    holder.plm_seekbar.setSecondaryProgress(data.getAsInteger("player_buffer"));

                    // share data update
                    ContentValues values = (ContentValues) holder.plm_share_tx.getTag();
                    values.put("rec_id", data.getAsInteger(Record.ID));
                    values.put("rec_name", data.getAsString(Record.NAME));
                    values.put("rec_url", data.getAsString(Record.URL));
                    values.put("nvl_name", getIntent().getStringExtra("nvltitle"));
                    values.put("nvl_pic", getIntent().getStringExtra("nvlposter"));
                    values.put("nvl_folder", getIntent().getStringExtra("nvlf"));
                    // end share data

                    // download data update
                    holder.plm_download_tx.setTag(data);
                    int downloadid = data.getAsInteger(Record.DOWNLOADID);
                    if (downloadid <=0){
                        // do not started download yet
                        holder.plm_download_tx.setText(R.string.download);
                    }else{
                        if (data.getAsInteger(Downloader.STATUS) == Downloader.STA_DONE){
                            holder.plm_download_tx.setText(R.string.download_done);
                            holder.plm_download_tx.setClickable(true);
                            holder.plm_download_tx.setTextColor(getResources().getColor(R.color.plm_download_tx_enable));
                        }else if (data.getAsInteger(Downloader.STATUS) == Downloader.STA_ERR){
                            String format = getResources().getString(R.string.download_err);
                            String text = String.format(format, data.getAsString(Downloader.ERR));
                            holder.plm_download_tx.setText(text);
                            holder.plm_download_tx.setClickable(true);
                            holder.plm_download_tx.setTextColor(getResources().getColor(R.color.plm_download_tx_enable));
                        }else if (data.getAsInteger(Downloader.STATUS) == Downloader.STA_PAUSED){
                            String format = getResources().getString(R.string.download_paused);
                            String text = String.format(format, data.containsKey("download_progress")?data.getAsInteger("download_progress"):"0");
                            holder.plm_download_tx.setText(text);
                            holder.plm_download_tx.setClickable(true);
                            holder.plm_download_tx.setTextColor(getResources().getColor(R.color.plm_download_tx_enable));
                        }else if (data.getAsInteger(Downloader.STATUS) == Downloader.STA_STARTED){
                            String format = getResources().getString(R.string.download_started);
                            String text = String.format(format, data.containsKey("download_progress")?data.getAsInteger("download_progress"):"0");
                            holder.plm_download_tx.setText(text);
                            holder.plm_download_tx.setClickable(true);
                            holder.plm_download_tx.setTextColor(getResources().getColor(R.color.plm_download_tx_enable));
                        }else if (data.getAsInteger(Downloader.STATUS) == Downloader.STA_PREPARING){
                            holder.plm_download_tx.setText(R.string.download_preparing);
                        }
                    }
                    // end download data update
                }else{
                    // easy mode

                    holder.esm.setVisibility(View.VISIBLE);
                    if(Myfunc.diffDay(data.getAsLong(Record.UPDATED)) < 1){
                        holder.esm_title.setText(data.getAsString(Record.NAME)+" ("+getResources().getString(R.string.new_novel)+")");
                    }else{
                        holder.esm_title.setText(data.getAsString(Record.NAME));
                    }

                    if (mExtendItemPos > 0){
                        // some item has extended grad it
                        holder.esm_title.setTextColor(getResources().getColor(R.color.esm_item_grey));
                    }else if (Myfunc.diffDay(data.getAsLong(Record.UPDATED))< 1){
                        holder.esm_title.setTextColor(getResources().getColor(R.color.novel_item_new_updated));
                    }else{
                        holder.esm_title.setTextColor(getResources().getColor(R.color.esm_item_black));
                    }

                    if (data.getAsInteger(Record.DOWNLOADID) > 0 && data.getAsInteger(Downloader.STATUS) == Downloader.STA_STARTED){
                        holder.esm_status.setImageResource(R.drawable.esm_download_started);
                    }else if (data.getAsInteger(Record.DOWNLOADID) > 0 && data.getAsInteger(Downloader.STATUS) == Downloader.STA_PAUSED){
                        holder.esm_status.setImageResource(R.drawable.esm_download_paused);
                    }else if (data.getAsInteger(Record.DOWNLOADID) > 0 && data.getAsInteger(Downloader.STATUS) == Downloader.STA_ERR){
                        holder.esm_status.setImageResource(R.drawable.esm_download_err);
                    }else{
                        if (data.getAsInteger("player_status") == PlayBackService.STA_IDLE){
                            if (data.getAsInteger(Record.READ) == 0) {
                                if (data.getAsInteger(Record.DOWNLOADID) > 0 && data.getAsInteger(Downloader.STATUS) == Downloader.STA_DONE){
                                    holder.esm_status.setImageResource(R.drawable.down_unread);
                                }else {
                                    holder.esm_status.setImageResource(R.drawable.esm_unread);
                                }
                            }else {
                                if (data.getAsInteger(Record.DOWNLOADID) > 0 && data.getAsInteger(Downloader.STATUS) == Downloader.STA_DONE){
                                    holder.esm_status.setImageResource(R.drawable.down_read);
                                }else {
                                    holder.esm_status.setImageResource(R.drawable.esm_read);
                                }
                            }
                        }else if (data.getAsInteger("player_status") == PlayBackService.STA_ERROR){
                            holder.esm_status.setImageResource(R.drawable.esm_play_err);
                        }else{
                            holder.esm_status.setImageResource(R.drawable.esm_play);
                        }
                    }


                    holder.plm.setVisibility(View.GONE);

                    //holder.plm_title.setText(data.getAsString(Record.NAME));
                }
            }
            return convertView;
        }
        public void stopLoadingIcon(Holder holder){
            if (holder.recLoading != null && holder.recLoading.isRunning()){
                holder.recLoading.stop();
                holder.recLoading = null;
            }
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
            TextView plm_share_tx;
            //TextView plm_rec_updated;
            TextView plm_rec_timer;
            TextView plm_download_tx;
            AnimationDrawable recLoading;
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
            ContentValues download = null;
            switch(msg.what){
                case MSG_ON_CURRENT_STATUS:
                    status = (PlayBackService.Status)msg.obj;
                    if (status.status == PlayBackService.STA_PAUSED){
                        mPlaybackItem.setVisible(true);
                        mPlaybackItem.setIcon(R.drawable.actionbar_start);
                        mscrolltoPos = status.itemPos;
                    }else if (status.status == PlayBackService.STA_STARTED){
                        mPlaybackItem.setVisible(true);
                        mPlaybackItem.setIcon(R.drawable.actionbar_pause);
                        mscrolltoPos = status.itemPos;
                    }else{
                        mPlaybackItem.setVisible(false);
                        mscrolltoPos = -1;
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
                    if (PlayingListActivity.this.getIntent().getBooleanExtra("scrollto", false) ){
                        Message locateMsag = Message.obtain();
                        locateMsag.what = MSG_ON_SCROLLTO_POS;
                        try {
                            mItSelf.send(locateMsag);
                        } catch (RemoteException e) {
                            Log.e(MyConstant.TAG_NOVEL, e.getMessage());
                        }
                    }
                    break;
                case MSG_ON_SCROLLTO_POS:
                    if (mscrolltoPos > 0){
                        mPlayinglistView.setSelection(mscrolltoPos);
                    }
                    break;
                case MSG_ON_DOWNLOAD_PROGRESS:
                    itemid = msg.arg1;
                    itempos = msg.arg2;
                    validpos = getValidPos(itemid, itempos);
                    if (validpos >=0 ) {
                        data = mPlaying_data.get(validpos);
                        data.put("download_progress", (Integer) msg.obj);
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
                        download = (ContentValues) msg.obj;
                        data.putAll(download);
                        //data.put(Downloader.STATUS, (Integer) msg.obj);
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
                        download = (ContentValues)msg.obj;
                        data.putAll(download);
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
                        download = (ContentValues)msg.obj;
                        data.putAll(download);
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
                        download = (ContentValues)msg.obj;
                        data.putAll(download);
                    }
                    if (isVisiblePosition(validpos)){
                        mMyadapter.notifyDataSetChanged();
                    }
                    break;
                case MSG_ON_PREPARING_DOWNLOAD:
                    itemid = msg.arg1;
                    itempos = msg.arg2;
                    validpos = getValidPos(itemid, itempos);
                    if (validpos >=0){
                        data = mPlaying_data.get(validpos);
                        data.put(Downloader.STATUS, (Integer)msg.obj);
                    }
                    if (isVisiblePosition(validpos)){
                        mMyadapter.notifyDataSetChanged();
                    }
                    break;
                case MSG_ON_DOWNLOAD_DELETE:
                    itemid = msg.arg1;
                    itempos = msg.arg2;
                    validpos = getValidPos(itemid, itempos);
                    if (validpos >=0){
                        data = mPlaying_data.get(validpos);
                        Record r = new Record();
                        r.deleteDownloader(PlayingListActivity.this, data);
                        data.remove(Downloader.FILENAME);
                        data.remove(Downloader.STATUS);
                        data.remove(Downloader.ERR);
                        data.remove(Downloader.FILESZ);
                        data.remove(Downloader.ID);
                        data.remove(Downloader.LASTPOS);
                        data.remove(Downloader.PATH);
                        data.remove(Downloader.URL);
                    }
                    if (isVisiblePosition(validpos)){
                        mMyadapter.notifyDataSetChanged();
                    }
                    mMyadapter.notifyDataSetChanged();
                    break;
                case MSG_ON_EXTEND_ITEM:
                    mMyadapter.notifyDataSetChanged();
                    break;
                case MSG_SHOWUP_YYT:
                    ContentValues yyt = Myfunc.getYytRandom();
                    if (yyt != null){
                        YytDialogFragmeng yytDialogFragmeng = new YytDialogFragmeng();
                        yytDialogFragmeng.yyt = yyt;
                        yytDialogFragmeng.show(getFragmentManager(), "yytdlg");
                    }

                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }
}
