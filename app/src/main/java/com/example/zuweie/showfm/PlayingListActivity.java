package com.example.zuweie.showfm;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
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
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.json.JSONException;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.zip.Inflater;


public class PlayingListActivity extends Activity {

    /* api */
    //private final static String RED_API = "http://www.showfm.net/api/record.asp";

    /* content data */
    private List<ContentValues> mPlaying_data;
    private Record mRecord = new Record();
    private Integer mPlayMode;
    private int mNovelId;
    private String mNovelFolder;

    /* UI data */
    private PullToRefreshListView mPullToRefreshListView;
    private ListView mPlayinglistView;
    private MyAdapter mMyadapter;

    /* PlayBack data */
    private Messenger mPlayback;
    private boolean mBindService = false;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mPlayback = new Messenger(service);
            mBindService = true;

            try {
                // log to service
                Message msg = Message.obtain(null, PlayBackService.MSG_LOGIN);
                msg.replyTo = new Messenger(new PlayingListHandler());
                mPlayback.send(msg);

                // post request to get Record list
                if (isNovelMode()){
                    msg = Message.obtain(null, PlayBackService.MSG_LOAD_RECORD_LIST);
                    msg.arg1 = mNovelId;
                    mPlayback.send(msg);
                }
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
    public final static int MSG_ON_MP3STA_UPDATE = 0x1;
    public final static int MSG_ON_LOAD_RECORD_LIST_DONE = 0x2;
    public final static int MSG_ON_DOWNLOAD_PROGRESS = 0x3;
    public final static int MSG_ON_DOWNLOAD_DONE = 0x4;
    public final static int MSG_ON_DOWNLOAD_PAUSED = 0x6;
    public final static int MSG_ON_DOWNLOAD_ERR    = 0x7;
    public final static int MSG_ON_READY_DOWNLOAD = 0x5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing_list);

        /* init content data*/
        mPlayMode = this.getIntent().getIntExtra("pm", -1);
        if (isNovelMode()) {
            mNovelId = this.getIntent().getIntExtra("nvl", -1);
            mNovelFolder = this.getIntent().getStringExtra("nvlf");
        }

        /* init the UI */
        mPullToRefreshListView = (PullToRefreshListView)this.findViewById(R.id.playing_list);
        mPlayinglistView = mPullToRefreshListView.getRefreshableView();
        mMyadapter = new MyAdapter();
        mPlayinglistView.setAdapter(mMyadapter);
        mPlayinglistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                // get item data
                if (mPlayback != null){
                    Adapter adapter = parent.getAdapter();
                    ContentValues item = (ContentValues)adapter.getItem(position);
                    if (isNovelMode()) {
                        try {

                            String url = item.getAsString(Record.URL);
                            url = Myfunc.getValidUrl(mNovelFolder, url, 900);
                            Message msg = Message.obtain(null, PlayBackService.MSG_PLAY);
                            msg.obj = url;
                            mPlayback.send(msg);

                        } catch (RemoteException e) {
                            Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                        } catch (NoSuchAlgorithmException e){
                            Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                        }
                    }
                }
            }
        });

    }

    @Override
    protected void onStart(){
        super.onStart();
        bs();
    }

    @Override
    protected void onStop(){
        ubs();
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_playing_list, menu);
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

    private void bs(){
        if (!mBindService){
            Intent it = new Intent(PlayingListActivity.this, PlayBackService.class);
            bindService(it, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    private void ubs(){
        if (mBindService){
            Message msg = Message.obtain(null, PlayBackService.MSG_LOGOUT);
            try {
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

    private int getValidPos(int itemid , int itmepos){
        if (isNovelMode()){
            if (mPlaying_data.get(itmepos).getAsInteger(Record.ID) == itemid)
                return itmepos;
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
                holder.image = (ImageView)convertView.findViewById(R.id.playing_item_img);
                holder.text  = (TextView)convertView.findViewById(R.id.playing_item_title);
                //holder.progressBar = (ProgressBar) convertView.findViewById(R.id.playing_item_progress);
                holder.bt = (Button) convertView.findViewById(R.id.playing_item_download);
                holder.bt.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        if (isNovelMode()){
                            ContentValues data = (ContentValues)v.getTag();
                            int downloadid = data.getAsInteger(Record.DOWNLOADID);
                            if (downloadid < 0){
                                // create the downloader
                                Downloader downloader = new Downloader();

                            }
                        }
                    }
                });
                convertView.setTag(holder);
            }else{
                holder = (Holder)convertView.getTag();
            }



            if (isNovelMode()) {
                holder.text.setText(data.getAsString(Record.NAME));
                holder.bt.setTag(data);
            }

            return convertView;
        }

        class Holder {
            ImageView image;
            TextView  text;
            Button bt;
        }
    }

    class PlayingListHandler extends Handler {
        @Override
        public void handleMessage(Message msg){
            int itemid = 0;
            int itempos = 0;
            int validpos = 0;
            switch(msg.what){
                case MSG_ON_MP3STA_UPDATE:
                    PlayBackService.Status status = (PlayBackService.Status)msg.obj;
                    if (status.status == PlayBackService.STA_ERROR){

                    }else if (status.status == PlayBackService.STA_COMPLETED){
                        // go next
                    }else if(status.status == PlayBackService.STA_READY){
                        // if get ready tell the playback start playing
                        Message ms = Message.obtain(null, PlayBackService.MSG_START);
                        try {
                            mPlayback.send(ms);
                        } catch (RemoteException e) {
                            Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                        }
                    }
                    break;
                case MSG_ON_LOAD_RECORD_LIST_DONE:
                    mPlaying_data = (List<ContentValues>)msg.obj;
                    mMyadapter.notifyDataSetChanged();
                    break;
                case MSG_ON_DOWNLOAD_PROGRESS:
                    itemid = msg.arg1;
                    itempos = msg.arg2;
                    validpos = getValidPos(itemid, itempos);
                    if (isVisiblePosition(validpos)){
                        if (isNovelMode()){
                            ContentValues data = mPlaying_data.get(validpos);
                            data.put("progress", (Integer)msg.obj);
                            data.put(Downloader.STATUS, Downloader.STA_STARTED);
                            mMyadapter.notifyDataSetChanged();
                        }
                    }
                    break;
                case MSG_ON_DOWNLOAD_DONE:
                    itemid = msg.arg1;
                    itempos = msg.arg2;
                    validpos = getValidPos(itemid, itempos);
                    if (isVisiblePosition(validpos)){
                        if (isNovelMode()){
                            ContentValues data = mPlaying_data.get(validpos);
                            data.put(Downloader.STATUS, (Integer)msg.obj);
                            mMyadapter.notifyDataSetChanged();
                        }
                    }
                    break;
                case MSG_ON_DOWNLOAD_PAUSED:
                    itemid = msg.arg1;
                    itempos = msg.arg2;
                    validpos = getValidPos(itemid, itempos);
                    if (isVisiblePosition(validpos)){
                        if (isNovelMode()){
                            ContentValues data = mPlaying_data.get(validpos);
                            data.put(Downloader.STATUS, (Integer)msg.obj);
                            mMyadapter.notifyDataSetChanged();
                        }
                    }
                    break;
                case MSG_ON_DOWNLOAD_ERR:
                    itemid = msg.arg1;
                    itempos = msg.arg2;
                    validpos = getValidPos(itemid, itempos);
                    if (isVisiblePosition(validpos)){
                        if(isNovelMode()){
                            ContentValues data = mPlaying_data.get(validpos);
                            data.put(Downloader.STATUS, (Integer)msg.obj);
                            mMyadapter.notifyDataSetChanged();
                        }
                    }
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

}
