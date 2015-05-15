package com.geekfocus.zuweie.showfm;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimationDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVInstallation;
import com.avos.avoscloud.PushService;
import com.avos.avoscloud.SaveCallback;
import com.dodowaterfall.widget.ScaleImageView;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.huewu.pla.lib.internal.PLA_AdapterView;
import com.umeng.analytics.MobclickAgent;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import me.maxwin.view.XListView;


public class MainActivity extends Activity implements XListView.IXListViewListener {

    /* data define */
    public static ClientID mClientid = new ClientID();
    public static boolean isRefreshing = false;

    /* 使用 waterfall 替代 PullToRefreshGridView */
    //private PullToRefreshGridView mPullToRefreshGridView;
    //private GridView mGridView;

    /* 使用 waterfall */
    private XListView mXListView;
    private StaggeredAdapter mAdapter;

    private List<ContentValues> mNovel_data = null;
    private List<ContentValues> mPickup_data = null;
    SparseArray<NetBitMap> mNovelCover = null;

    //private MyAdapter mAdapter;
    private MenuItem mPlaybackItem;
    private CharSequence mPickUp;
    private List<String> mCategory;

    private Integer mCurrPlayingNovelId = -1;
    private AnimationDrawable mHeaderIcon = null;
    /* service data */
    private Messenger mPlayback = null;
    private Messenger mItself = null;
    //private boolean mBindService = false;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPlayback = new Messenger(service);
            //mBindService = true;

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
        public void onServiceDisconnected(ComponentName name) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);

        // 孟友数据平台分析
        MobclickAgent.updateOnlineConfig( this );

        // leancloud 消息推送服务
        AVInstallation.getCurrentInstallation().saveInBackground(new SaveCallback() {
            @Override
            public void done(AVException e) {
                if (e == null){
                    Log.v("leancloud_push", AVInstallation.getCurrentInstallation().getInstallationId());
                }else{
                    Log.e("leancloud_push", e.getMessage());
                }
            }
        });

        //PushService.setDefaultPushCallback(this, MainActivity.class);
        PushService.subscribe(this, "showfm", MainActivity.class);

            // Init the Data
        Novel novel = new Novel();
        mNovel_data = novel.loadData(this, null, null, null, "updated desc");
        mPickup_data = new LinkedList<ContentValues>();
        mPickup_data.addAll(mNovel_data);

        // get the novel category
        mCategory = new LinkedList<String>();
        for (int i=0; i<mNovel_data.size(); ++i){
            if (!mCategory.contains(mNovel_data.get(i).getAsString(Novel.CATEGORY)))
                mCategory.add(mNovel_data.get(i).getAsString(Novel.CATEGORY));
        }

        // Init the Ui data
        this.getActionBar().setHomeButtonEnabled(true);

        mNovelCover = new SparseArray<NetBitMap>();


        for(int i=0; i<mNovel_data.size(); ++i){
            ContentValues data = mNovel_data.get(i);
            data.put("item_pos", i);
            NetBitMap cover = new NetBitMap(MainActivity.this, data.getAsString(Novel.POSTER), R.drawable.nvl_def_bg,  1024*5);
            mNovelCover.append(data.getAsInteger(Novel.ID), cover);
        }

        // if some cover need to load then create the load task to load it

        mItself = new Messenger(new MainHandler());
        /* Start the PlaybackService*/
        Intent it = new Intent(MainActivity.this, PlayBackService.class);
        startService(it);
        /* Start the PlaybackService*/

        /* Ui init use waterfall list view */
        mPickUp = this.getResources().getString(R.string.category_all);
        mXListView = (XListView)findViewById(R.id.waterfall_listview);
        mXListView.setPullLoadEnable(false);
        mXListView.setXListViewListener(this);
        mXListView.setOnItemClickListener(new PLA_AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(PLA_AdapterView<?> parent, View view, int position, long id) {
                Adapter adapter = parent.getAdapter();
                ContentValues data = (ContentValues)adapter.getItem(position);
                Intent it = new Intent(MainActivity.this, NovelDetailActivity.class);
                it.putExtra("nvl", data.getAsInteger(Novel.ID));
                it.putExtra("pm", MyConstant.PM_NOVEL);
                it.putExtra("nvlf", data.getAsString(Novel.URL));
                it.putExtra("nvltitle", data.getAsString(Novel.NAME));
                it.putExtra("nvlbody", data.getAsString(Novel.BODY));
                it.putExtra("nvlposter", data.getAsString(Novel.POSTER));
                it.putExtra("nvlupdated",data.getAsLong(Novel.UPDATED));
                it.putExtra("njname", data.getAsString(Novel.NJNAME));
                it.putExtra("nvlauthor", data.getAsString(Novel.AUTHOR));
                it.putExtra("nvlcategory", data.getAsString(Novel.CATEGORY));
                it.putExtra("njid", data.getAsString(Novel.NJID));
                it.putExtra("njavatar", data.getAsString(Novel.NJAVATAR));
                MainActivity.this.startActivity(it);
            }
        });
        mAdapter = new StaggeredAdapter();
        mXListView.setAdapter(mAdapter);
        /* Ui init use waterfall list view */
    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onResume (){
        super.onResume();
        bs();
        MobclickAgent.onResume(this);
        /* comment facebook code, can not be used in china fuck
        //AppEventsLogger.activateApp(this);
        */

    }

    @Override
    protected void onPause () {
        ubs();
        MobclickAgent.onPause(this);

        /* comment facebook code, can not be used in china fuck
        //AppEventsLogger.deactivateApp(this);
        */

        super.onPause();

    }

    @Override
    protected void onStop(){
        super.onStop();
    }


    @Override
    protected void onDestroy() {

        /* stop the PlayBackService */
        Intent it = new Intent(MainActivity.this, PlayBackService.class);
        stopService(it);
        /* stop the PlayBackService */
        super.onDestroy();

        //System.exit(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        mPlaybackItem = menu.findItem(R.id.action_play);
        mPlaybackItem.setVisible(false);
        MenuItem CategoryItem = menu.findItem(R.id.action_category);
        SubMenu subMenu = CategoryItem.getSubMenu();
        subMenu.add(R.string.category_all);
        for(int i=0; i<mCategory.size(); ++i){
            subMenu.add(mCategory.get(i));
        }
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
            ContentValues data = this.getCurrentPlayingNovel();
            if (data != null){
                Intent it = new Intent(MainActivity.this, PlayingListActivity.class);
                it.putExtra("nvl", data.getAsInteger(Novel.ID));
                it.putExtra("pm", MyConstant.PM_NOVEL);
                it.putExtra("nvlf", data.getAsString(Novel.URL));
                it.putExtra("nvltitle", data.getAsString(Novel.NAME));
                it.putExtra("nvlbody", data.getAsString(Novel.BODY));
                it.putExtra("nvlupdated",data.getAsLong(Novel.UPDATED));
                it.putExtra("nvlstatus", data.getAsInteger(Novel.STATUS) == 1? "连载中":"已完结");
                it.putExtra("njname", data.getAsString(Novel.NJNAME));
                it.putExtra("nvlauthor", data.getAsString(Novel.AUTHOR));
                it.putExtra("nvlcategory", data.getAsString(Novel.CATEGORY));
                it.putExtra("njid", data.getAsString(Novel.NJID));
                it.putExtra("njavatar", data.getAsString(Novel.NJAVATAR));
                it.putExtra("cover_exists", data.getAsBoolean("cover_exists"));
                it.putExtra("cover_height", data.getAsInteger("cover_height"));
                it.putExtra("cover_width", data.getAsInteger("cover_width"));
                it.putExtra("scrollto",true);
                MainActivity.this.startActivity(it);
            }
        }else if (id == R.id.action_me){
            //this.finish();
            Intent it = new Intent(MainActivity.this, SettingActivity.class);
            startActivityForResult(it, MyConstant.ME_REQ_CODE);
        }else if (id == R.id.action_play){
            if (mPlayback != null){
                Integer status = item.getIntent().getIntExtra("player_status", -1);
                try{
                    Message msg = null;
                    if (status == PlayBackService.STA_STARTED){
                        msg = Message.obtain(null, PlayBackService.MSG_PAUSE);
                    }else if (status == PlayBackService.STA_PAUSED){
                        msg = Message.obtain(null, PlayBackService.MSG_START);
                        // do not start player progress updater
                        msg.arg1 = 0;
                    }
                    mPlayback.send(msg);
                }catch (RemoteException e){
                    Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                }
            }
        }else if (id == 0){
            CharSequence category = item.getTitle();
            if (!mPickUp.equals(category)){
                mPickUp = item.getTitle();
                mPickup_data.clear();
                if (mPickUp.equals(getResources().getString(R.string.category_all))){
                    mPickup_data.addAll(mNovel_data);
                }else{
                    for (int i=0; i<mNovel_data.size(); ++i){
                        if (mPickUp.equals(mNovel_data.get(i).getAsString(Novel.CATEGORY))){
                            mPickup_data.add(mNovel_data.get(i));
                        }
                    }
                }
                mAdapter.notifyDataSetChanged();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == MyConstant.ME_REQ_CODE){
            if (resultCode == MyConstant.ME_RESULT_EXIT){
                finish();
            }
        }
    }

    @Override
    public void onBackPressed(){
        this.moveTaskToBack(false);
        return;
    }

    @Override
    public void onRefresh() {
        //Toast.makeText(MainActivity.this, R.string.novel_newest, Toast.LENGTH_LONG).show();
        if (isRefreshing == false)
            new RefreshNovelTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        else
            mXListView.stopRefresh();
    }

    @Override
    public void onLoadMore() {
        return;
    }

    public boolean connectService(){
        return mPlayback != null;
    }

    public void bs (){
        if (!connectService()){
            Intent it = new Intent(MainActivity.this, PlayBackService.class);
            bindService(it, mConnection, Context.BIND_AUTO_CREATE);
        }
    }
    public void ubs (){
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
            //mBindService = false;
        }
    }

    private ContentValues getCurrentPlayingNovel(){
        if (mCurrPlayingNovelId > 0) {
            for (int i = 0; i < mNovel_data.size(); ++i) {
                if (mCurrPlayingNovelId == mNovel_data.get(i).getAsInteger(Novel.ID)) {
                    return mNovel_data.get(i);
                }
            }
        }
        return  null;
    }

    private void updateActionBarMenu(int what){

        switch(what){
            case PlayBackService.STA_STARTED:
                // set the header icon
                if (mHeaderIcon == null) {
                    mHeaderIcon = (AnimationDrawable) this.getResources().getDrawable(R.drawable.cd_rotate);
                    this.getActionBar().setIcon(mHeaderIcon);
                }
                mHeaderIcon.start();
                mPlaybackItem.setIcon(R.drawable.actionbar_pause);
                if (mPlaybackItem.getIntent() == null){
                    mPlaybackItem.setIntent(new Intent());
                }
                mPlaybackItem.getIntent().putExtra("player_status", PlayBackService.STA_STARTED);
                mPlaybackItem.setVisible(true);
                break;
            case PlayBackService.STA_PAUSED:
                if (mHeaderIcon == null){
                    mHeaderIcon = (AnimationDrawable) this.getResources().getDrawable(R.drawable.cd_rotate);
                    this.getActionBar().setIcon(mHeaderIcon);
                }
                mHeaderIcon.stop();
                mPlaybackItem.setIcon(R.drawable.actionbar_start);
                if (mPlaybackItem.getIntent() == null){
                    mPlaybackItem.setIntent(new Intent());
                }
                mPlaybackItem.getIntent().putExtra("player_status", PlayBackService.STA_PAUSED);
                mPlaybackItem.setVisible(true);
                break;
            default:
                if (mHeaderIcon != null) {
                    mHeaderIcon.stop();
                    mHeaderIcon = null;
                }
                this.getActionBar().setIcon(R.drawable.showfm);
                mPlaybackItem.setVisible(false);
        }
        return;
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
    // 用于下拉更新小说列表
    private class RefreshNovelTask extends AsyncTask<Void, Integer, List<ContentValues>> {

        @Override
        protected List<ContentValues> doInBackground(Void... params) {
            isRefreshing = true;
            Novel novel = new Novel();
            String date = Myfunc.ltime2Sdate(novel.getMark(MainActivity.this));
            String api = Myfunc.getNovelApi()+"?after="+ Uri.encode(date)+"state0=1&state1=2";
            //String api = "http://www.showfm.net/api/novel.asp?after=2014%2F01%2F01%2008%3A00%3A00&state0=1&state1=2";
            List<ContentValues> result = null;
            try {
                List<ContentValues> datas = novel.getData(api);
                if (datas != null && !datas.isEmpty()) {
                    //novel.saveData(MainActivity.this, datas);
                    novel.setMark(MainActivity.this);
                    List<ContentValues> vs = new ArrayList<ContentValues>();

                    for(int i=0; i<datas.size(); ++i){
                        ContentValues v = new ContentValues();
                        ContentValues data = datas.get(i);
                        v.put(Novel.ID, data.getAsInteger(Novel.ID));
                        v.put(Novel.UPDATED, data.getAsLong(Novel.UPDATED));
                        vs.add(v);
                    }
                    novel.updataNovelDate(MainActivity.this, vs);
                    // reload data from database;
                    result = novel.loadData(MainActivity.this, null, null, null, "updated desc");
                }
            } catch (IOException e) {}
            catch (JSONException e) {
                e.printStackTrace();
            }
            isRefreshing = false;
            return result;
        }

        @Override
        protected void onPostExecute(List<ContentValues> result){
            mXListView.stopRefresh();
            if (result != null){
                mNovel_data.clear();
                mNovel_data.addAll(result);
                mPickup_data.clear();
                mPickup_data.addAll(mNovel_data);
            }
            mAdapter.notifyDataSetChanged();
        }
    }


    class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg){
            PlayBackService.Status status = null;
            switch(msg.what){
                case PlayingListActivity.MSG_ON_CURRENT_STATUS:
                case PlayingListActivity.MSG_ON_MP3STA_UPDATE:
                    // TODO : update icon of the actionbar
                    status = (PlayBackService.Status)msg.obj;
                    updateActionBarMenu(status.status);
                    if (status.status == PlayBackService.STA_PAUSED
                       || status.status == PlayBackService.STA_STARTED){
                        mCurrPlayingNovelId = status.contentId;
                    }else{
                        mCurrPlayingNovelId = -1;
                    }
                    break;
                default:
                super.handleMessage(msg);
            }

        }
    }


    class StaggeredAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            if (mPickup_data == null)
                return 0;
            else
                return mPickup_data.size();
        }

        @Override
        public Object getItem(int position) {
            return mPickup_data.get(position);
        }

        @Override
        public long getItemId(int position) {
            ContentValues values = (ContentValues)this.getItem(position);
            return values.getAsInteger(Novel.ID);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;
            if (convertView == null){
                holder = new Holder();
                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                convertView = inflater.inflate(R.layout.novel_item, null);
                holder.nvl_item  = (View)convertView.findViewById(R.id.nvl_item);
                holder.imageView = (ScaleImageView)convertView.findViewById(R.id.nvl_img_view);
                holder.textView  = (TextView)convertView.findViewById(R.id.nvl_text_view);
                holder.nvl_nj    = (TextView)convertView.findViewById(R.id.nvl_nj);
                convertView.setTag(holder);
            }else{
                holder = (Holder)convertView.getTag();
            }

            ContentValues data = (ContentValues)this.getItem(position);
            String name;
            if (Myfunc.diffDay(data.getAsLong(Novel.UPDATED)) < 1) {
                name = data.getAsInteger(Novel.ID) + "." + data.getAsString(Novel.NAME)+"("+getResources().getString(R.string.new_novel)+")";
                holder.textView.setTextColor(getResources().getColor(R.color.novel_item_new_updated));
            }else{
                name = data.getAsInteger(Novel.ID) + "." + data.getAsString(Novel.NAME);
                holder.textView.setTextColor(getResources().getColor(R.color.novel_item__normal));
            }
            holder.textView.setText(name);
            holder.nvl_nj.setText(data.getAsString(Novel.NJNAME));
            // make it gone

            NetBitMap b = mNovelCover.get(data.getAsInteger(Novel.ID));
            Bitmap bmp = b.getBitmap(null);
            int pic_h = bmp.getHeight();
            int pic_w = bmp.getWidth();
            int w = (int) MainActivity.this.getResources().getDimension(R.dimen.novel_item_2_width);
            holder.imageView.setImageWidth(w);
            holder.imageView.setImageHeight((w * pic_h) / pic_w);
            holder.imageView.setImageBitmap(bmp);
            return convertView;
        }

        class Holder {
            View      nvl_item;
            ScaleImageView imageView;
            TextView  textView;
            TextView  nvl_nj;
        }
    }
}
