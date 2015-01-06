package com.example.zuweie.showfm;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class MainActivity extends Activity {

    /* data define */
    public static ClientID mClientid = new ClientID();

    private PullToRefreshGridView mPullToRefreshGridView;
    private GridView mGridView;
    private List<ContentValues> mNovel_data = null;
    private MyAdapter mAdapter;
    private MenuItem mPlaybackItem;

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
        setContentView(R.layout.activity_main);

        // Init the Data
        Novel novel = new Novel();
        mNovel_data = novel.loadData(this, null, null, null, "updated desc");

        // get the novel category
        List<String> categorys = new ArrayList<String>();
        for (int i=0; i<mNovel_data.size(); ++i){
            if (!categorys.contains(mNovel_data.get(i).getAsString(Novel.CATEGORY)))
                categorys.add(mNovel_data.get(i).getAsString(Novel.CATEGORY));
        }

        // Init the Ui data
        this.getActionBar().setHomeButtonEnabled(true);
        String coverfolder = this.getFilesDir().getAbsolutePath();
        List<ContentValues> loadCoverParams = new ArrayList<ContentValues>();


        for(int i=0; i<mNovel_data.size(); ++i){
            ContentValues data = mNovel_data.get(i);
            data.put("item_pos", i);
            String coverfile = coverfolder+"/"+data.getAsString(Novel.ID) + ".jpg";
            File file = new File(coverfile);
            if (file.exists()){
                data.put("cover_exists", true);
                //data.put("cover_uri", file.toURI().toString());
            }else{
                data.put("cover_exists", false);

                ContentValues param = new ContentValues();
                param.put("pic_url", data.getAsString(Novel.POSTER));
                param.put("pic_name", data.getAsString(Novel.ID)+".jpg");
                param.put("item_id", data.getAsInteger(Novel.ID));
                param.put("item_pos", data.getAsInteger("item_pos"));
                loadCoverParams.add(param);
            }

        }

        // if some cover need to load then create the load task to load it
        if (!loadCoverParams.isEmpty()){
            //new GetNovelCoverTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,loadCoverParams);
            new GetNovelCoverTask().execute(loadCoverParams);
        }

        mItself = new Messenger(new MainHandler());
        /* Start the PlaybackService*/
        Intent it = new Intent(MainActivity.this, PlayBackService.class);
        startService(it);
        /* Start the PlaybackService*/

        // Init the UI
        mPullToRefreshGridView = (PullToRefreshGridView) findViewById(R.id.pull_refresh_grid);
        mGridView = mPullToRefreshGridView.getRefreshableView();
        mAdapter = new MyAdapter();
        mGridView.setAdapter(mAdapter);
        
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ContentValues data = (ContentValues)mAdapter.getItem(position);
                Intent it = new Intent(MainActivity.this, PlayingListActivity.class);
                it.putExtra("nvl", data.getAsInteger(Novel.ID));
                it.putExtra("pm", MyConstant.PM_NOVEL);
                it.putExtra("nvlf", data.getAsString(Novel.URL));
                it.putExtra("nvltitle", data.getAsString(Novel.NAME));
                it.putExtra("nvlbody", data.getAsString(Novel.BODY));
                it.putExtra("nvlupdated",data.getAsLong(Novel.UPDATED));
                it.putExtra("nvlstatus", data.getAsInteger(Novel.STATUS));
                it.putExtra("njname", data.getAsString(Novel.NJNAME));
                it.putExtra("nvlauthor", data.getAsString(Novel.AUTHOR));
                it.putExtra("nvlcategory", data.getAsString(Novel.CATEGORY));
                it.putExtra("cover_exists", data.getAsBoolean("cover_exists"));
                MainActivity.this.startActivity(it);
            }
        });

        mPullToRefreshGridView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<GridView>() {
            @Override
            public void onPullDownToRefresh(PullToRefreshBase<GridView> refreshView) {
                new RefreshNovelTask().execute();
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<GridView> refreshView) {

            }
        });

    }

    @Override
    protected void onStart(){
        super.onStart();
    }

    @Override
    protected void onResume (){
        super.onResume();
        bs();
    }

    @Override
    protected void onPause () {
        ubs();
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
            this.moveTaskToBack(false);
        }else if (id == R.id.action_exit){
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
                        // do not start player progress updater
                        msg.arg1 = 0;
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
    public void onBackPressed(){
        this.moveTaskToBack(false);
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

    private int getValiPos(int itemid, int itempos){
        if (mNovel_data != null && !mNovel_data.isEmpty() && itemid >=0 && itempos >=0){
            int id = mNovel_data.get(itempos).getAsInteger(Novel.ID);
            if(id == itemid)
                return  itempos;
            else{
                for(int i=0; i<mNovel_data.size(); i++){
                    ContentValues data = mNovel_data.get(i);
                    if (data.getAsInteger(Novel.ID) == itemid){
                        return i;
                    }
                }
            }
        }
        return -1;
    }
    private void updateActionBarMenu(int what){

        switch(what){
            case PlayBackService.STA_STARTED:
                mPlaybackItem.setIcon(R.drawable.actionbar_pause);
                if (mPlaybackItem.getIntent() == null){
                    mPlaybackItem.setIntent(new Intent());
                }
                mPlaybackItem.getIntent().putExtra("player_status", PlayBackService.STA_STARTED);
                mPlaybackItem.setVisible(true);
                break;
            case PlayBackService.STA_PAUSED:
                mPlaybackItem.setIcon(R.drawable.actionbar_start);
                if (mPlaybackItem.getIntent() == null){
                    mPlaybackItem.setIntent(new Intent());
                }
                mPlaybackItem.getIntent().putExtra("player_status", PlayBackService.STA_PAUSED);
                mPlaybackItem.setVisible(true);
                break;
            default:
                mPlaybackItem.setVisible(false);
        }
        return;
    }

    /*
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_my, container, false);
            return rootView;
        }
    }
    */
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

    private class RefreshNovelTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {

            Novel novel = new Novel();
            String date = Myfunc.ltime2Sdate(novel.getMark(MainActivity.this));
            String api = "http://www.showfm.net/api/novel.asp?after="+Uri.encode(date)+"state0=1&state1=2";

            try {
                List<ContentValues> datas = novel.getData(api);
                if (datas != null && !datas.isEmpty()) {
                    novel.saveData(MainActivity.this, datas);
                    novel.setMark(MainActivity.this);
                    List<ContentValues> vs = new ArrayList<ContentValues>();

                    for(int i=0; i<datas.size(); ++i){
                        ContentValues v = new ContentValues();
                        ContentValues data = datas.get(i);
                        v.put(Novel.ID, data.getAsInteger(Novel.ID));
                        v.put(Novel.UPDATED, data.getAsInteger(Novel.UPDATED));
                        vs.add(v);
                    }
                    novel.updataNovelDate(MainActivity.this, vs);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result){
            mPullToRefreshGridView.onRefreshComplete();
            mAdapter.notifyDataSetChanged();
        }
    }

    private class GetNovelCoverTask extends AsyncTask <List<ContentValues>, Void, Integer> {

        @Override
        protected Integer doInBackground(List<ContentValues>... Params) {

            List<ContentValues> params = Params[0];
            BufferedInputStream in = null;
            BufferedOutputStream out = null;
            FileOutputStream fos = null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Integer ret = 0;

            byte[] buffer =  new byte[1024 * 100];
            for(int i=0; i<params.size(); ++i){
                try {

                    ContentValues data = params.get(i);
                    bos.reset();
                    String pic_url = data.getAsString("pic_url");
                    URL url = new URL(pic_url);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(5 * 1000);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
                    connection.setRequestProperty("Accept-Language", "zh-CN");
                    connection.setRequestProperty("Charset", "UTF-8");
                    connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
                    connection.setRequestProperty("Connection", "Keep-Alive");

                    //Bitmap bitmap = BitmapFactory.decodeByteArray(bmdata, 0, bmdata.length);
                    Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());

                    int bmsz = 1024 * 8; // 8k
                    boolean loadCoverok = false;
                    if (bitmap != null && bitmap.getByteCount() > bmsz ){
                        int q = 90;
                        // compress bitmap, make it smaller then 8k.
                        do{
                           bos.reset();

                           if (q >= 1)
                            bitmap.compress(Bitmap.CompressFormat.JPEG, q, bos);

                           if (q <=30)
                               q -= 5;
                           else if (q<=10)
                               q -= 1;
                           else
                               q -= 30;
                        }while(bos.size() > bmsz && q >1);

                        Log.v(MyConstant.TAG_NOVEL, "compress bitmap file sz : " + (bos.size() /1024)+ "k and save it in file : "+ data.getAsString("pic_name"));
                        fos = openFileOutput(data.getAsString("pic_name"), Context.MODE_PRIVATE);
                        fos.write(bos.toByteArray());
                        loadCoverok = true;
                    }else if (bitmap != null){
                        bos.reset();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                        Log.v(MyConstant.TAG_NOVEL, "save bitmap file sz : "+ (bos.size()) +"k in file: "+data.getAsString("pic_name"));
                        fos = openFileOutput(data.getAsString("pic_name"), Context.MODE_PRIVATE);
                        fos.write(bos.toByteArray());
                        loadCoverok = true;
                    }else{
                        Log.e(MyConstant.TAG_NOVEL, "bitmap is null, file loading Url is "+data.getAsString("pic_url"));
                    }

                    // TODO : update mNovel_data ui data;
                    if (loadCoverok){

                        int item_id = data.getAsInteger("item_id");
                        int item_pos = data.getAsInteger("item_pos");
                        int valipos = getValiPos(item_id, item_pos);
                        if (valipos >=0 ){
                            ContentValues itemdata = mNovel_data.get(valipos);
                            // put the update data
                            itemdata.put("cover_exists", true);
                        }
                        ret += 1;
                    }

                    if (in!= null)
                        in.close();
                    if (out!= null)
                        out.close();
                    if (fos != null)
                        fos.close();
                }  catch (IOException e) {

                        try {
                            if (in!= null)
                                in.close();
                            if (out!= null)
                                out.close();
                            if (fos != null)
                                fos.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }

                }
            }
           return ret;
        }

        @Override
        protected void onPostExecute(Integer result){
            // TODO : 分析数据， 将其丢入数据库。
            if (result > 0 && mAdapter != null)
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
                    break;
                default:
                super.handleMessage(msg);
            }

        }
    }

    class MyAdapter extends BaseAdapter {

        private Context context;
        class Holder {
            ImageView new_image;
            ImageView imageView;
            TextView  textView;
            TextView  nvl_nj;
        }


        @Override
        public int getCount() {
            if (mNovel_data == null)
                return 0;
            else
                return mNovel_data.size();
        }

        @Override
        public Object getItem(int position) {
            return mNovel_data.get(position);
        }

        @Override
        public long getItemId(int position) {
            ContentValues data = (ContentValues)this.getItem(position);
            return data.getAsInteger(Novel.ID);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;
            if (convertView == null){
                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                convertView = inflater.inflate(R.layout.novel_item, null);
                holder = new Holder();

                holder.new_image = (ImageView)convertView.findViewById(R.id.nvl_new_img);
                holder.imageView = (ImageView)convertView.findViewById(R.id.nvl_img_view);
                holder.textView  = (TextView)convertView.findViewById(R.id.nvl_text_view);
                holder.nvl_nj    = (TextView)convertView.findViewById(R.id.nvl_nj);
                convertView.setTag(holder);
            }else{
                holder = (Holder)convertView.getTag();
            }

            // render the item
            ContentValues data = (ContentValues)this.getItem(position);
            String name = data.getAsInteger(Novel.ID) + "." +data.getAsString(Novel.NAME);
            holder.textView.setText(name);
            holder.nvl_nj.setText(data.getAsString(Novel.NJNAME));

            if (position < 4){
                holder.new_image.setVisibility(View.VISIBLE);
            }else{
                holder.new_image.setVisibility(View.GONE);
            }

            //holder.imageView.setImageResource(R.drawable.nvl_default);
            if (data.getAsBoolean("cover_exists")){
                if (data.get("cover_uri") == null){
                    String coverfile = getFilesDir().getAbsolutePath()+"/"+data.getAsString(Novel.ID) + ".jpg";
                    File file = new File(coverfile);
                    if (file.exists()){
                        String uri = file.toURI().toString();
                        holder.imageView.setImageURI(Uri.parse(uri));
                        data.put("cover_uri", uri);
                    }else{
                        data.put("cover_exists", false);
                    }
                }else{
                    holder.imageView.setImageURI(Uri.parse(data.getAsString("cover_uri")));
                }
            }else{
                holder.imageView.setImageResource(R.drawable.nvl_def_bg);
            }
            return convertView;
        }
    }
}
