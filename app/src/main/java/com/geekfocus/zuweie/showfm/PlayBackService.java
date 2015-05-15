package com.geekfocus.zuweie.showfm;

import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PlayBackService extends Service {

    private Messenger mMessenger = null;
    //private Messenger mClient = null;
    private SparseArray <Messenger> mClients = null;
    private MediaPlayer mPlayback = null;
    AudioManager mAudioManager = null;
    AudioManager.OnAudioFocusChangeListener mAudioFocusChangerListener = null;
    /* Msg define */
    public final static int MSG_LOGIN = 1;
    public final static int MSG_LOGOUT = 2;
    public final static int MSG_PLAY = 3;
    public final static int MSG_PLAY_LOCAL = 14;
    public final static int MSG_START = 4;
    public final static int MSG_PAUSE = 5;
    public final static int MSG_SEEKTO = 6;
    public final static int MSG_LOAD_RECORD_LIST = 7;
    public final static int MSG_START_DOWNLOAD_REC = 8;
    public final static int MSG_PAUSE_DOWNLOAD_REC = 9;
    public final static int MSG_DELETE_DOWNLOAD_REC=13;
    public final static int MSG_CURRENT_STATUS =    10;
    public final static int MSG_START_PLAYER_PGROGRESS_UPDATER = 11;
    public final static int MSG_STOP_PLAYER_PROGRESS_UPDATER = 12;

    /* status define */
    public final static int STA_IDLE = 0;
    public final static int STA_STARTED = 1;
    public final static int STA_PAUSED = 2;
    public final static int STA_PREPARING = 3;
    public final static int STA_READY = 4;
    public final static int STA_ERROR = 5;
    public final static int STA_COMPLETED = 6;

    /* */
    private static Status mMp3Status;
    private DoingWhat mDoingWhat;
    private SparseArray<ContentValues> mRunningDownloadtask = null;
    private Timer mPlayerProgressUpdater;
    //private boolean isUpdaterCancel = true;

    public PlayBackService() {super();}

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        /* set Messenger */
        mMessenger = new Messenger(new PlayBackHandler());

        /* set MediaPlayer */
        mPlayback = new MediaPlayer();

        /* set error listener */
        mPlayback.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mMp3Status.err_extra = extra;
                mMp3Status.err_what  = what;
                mMp3Status.updateStatus(PlayBackService.STA_ERROR,  mClients);
                abandonAudioFocus();
                return false;
            }
        });

        /* set BufferingUpdate Listener */
        mPlayback.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if (percent - mMp3Status.buffer_percent > 4){
                    mMp3Status.buffer_percent = percent;
                }
            }
        });

        /* set OnPrepared Listener */
        mPlayback.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mMp3Status.updateStatus(PlayBackService.STA_READY, mClients);
            }
        });

        /* set OnCompletion Listener */
        mPlayback.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                abandonAudioFocus();
            }
        });

        /* set OnSeekComplete Listener */
        mPlayback.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {}
        });

        /*set Playback Status */
        mMp3Status = new Status();

        mDoingWhat = new DoingWhat();

        mRunningDownloadtask = new SparseArray<ContentValues>();

        mClients = new SparseArray<Messenger>();

        mAudioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);

        mAudioFocusChangerListener = new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int focusChange) {
                if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){
                    // Pause playback
                    pause();
                } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
                    start();
                }
            }
        };

        return START_STICKY;
    }

    @Override
    public void onDestroy (){
        mPlayback.release();
        mPlayback = null;

        // check the download task if exists stop it
        Log.v(MyConstant.TAG_DOWNLOADER, "pause all the not finished mask");
        Downloader downloader = new Downloader();
        for(int i=0; i<mRunningDownloadtask.size(); ++i){
            ContentValues downloadtask = mRunningDownloadtask.get(i);
            downloader.pauseDownload(downloadtask);
        }
        mRunningDownloadtask.clear();
        super.onDestroy();
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mMessenger.getBinder();
    }

    private int requestAudioFocus(){
        return mAudioManager.requestAudioFocus(mAudioFocusChangerListener, AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
    }

    private void abandonAudioFocus(){
        mAudioManager.abandonAudioFocus(mAudioFocusChangerListener);
    }

    /* playback function define */
    private void play(Context c, String uri){

        try {
            mPlayback.reset();
            mPlayback.setAudioStreamType(AudioManager.STREAM_MUSIC);
            //mMp3Status.updateStatus();

            mPlayback.setDataSource(PlayBackService.this, Uri.parse(uri));
            mPlayback.prepareAsync();
            mMp3Status.updateStatus(PlayBackService.STA_PREPARING, mClients);

        } catch (IOException e) {
            Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
        }
    }

    private void play(String url){

        try {
            mPlayback.reset();
            mPlayback.setAudioStreamType(AudioManager.STREAM_MUSIC);
            //mMp3Status.updateStatus();

            mPlayback.setDataSource(url);
            mPlayback.prepareAsync();
            mMp3Status.updateStatus(PlayBackService.STA_PREPARING,mClients);
        } catch (IOException e) {
            Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
        }

    }

    private void play(Uri uri){
        try{
            mPlayback.reset();
            mPlayback.setAudioStreamType(AudioManager.STREAM_MUSIC);

            mPlayback.setDataSource(PlayBackService.this, uri);
            mPlayback.prepareAsync();
            mMp3Status.updateStatus(PlayBackService.STA_PREPARING, mClients);
        }catch (IOException e){
            Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
        }
    }

    private void start() {
        if (mMp3Status.canStart()) {
            mPlayback.start();
            mMp3Status.updateStatus(PlayBackService.STA_STARTED, mClients);
        }
    }

    private void pause () {
        if (mMp3Status.canPaused()){
            mPlayback.pause();
            mMp3Status.updateStatus(PlayBackService.STA_PAUSED, mClients);
        }
    }

    private void seekTo (int msec) {
        if (mMp3Status.canSeekto()){
            mPlayback.seekTo(msec);
        }
    }

    public class DoingWhat {
        public Boolean isLoadingRecordList = new Boolean(false);

        synchronized Boolean getLoadingRec (){
            return  isLoadingRecordList;
        }

        synchronized void setLoadingRec (Boolean v) {
            isLoadingRecordList = v;
        }
    }

    public void startUpdater() {
        mPlayerProgressUpdater = new Timer();
        mPlayerProgressUpdater.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mMp3Status.status == PlayBackService.STA_STARTED && mMp3Status.canGetPostion()){
                    mMp3Status.position = mPlayback.getCurrentPosition();
                    mMp3Status.duration = mPlayback.getDuration();
                    mMp3Status.updateMP3Progress(mClients);
                }

            }
        }, 500, 1000);
    }
    public void stopUpdater() {
        if (mPlayerProgressUpdater != null)
            mPlayerProgressUpdater.cancel();
        mPlayerProgressUpdater = null;
    }


    public void addClient(int key, Messenger client){
        synchronized (mClients) {
            mClients.put(key, client);
        }
    }

    public void removeClient(int key){
        synchronized (mClients){
            mClients.remove(key);
        }

    }


    public static void sendMessage(Message msg, SparseArray<Messenger> clients){

        synchronized (clients){
            for(int i=0; i<clients.size(); ++i){
                Messenger client = clients.valueAt(i);
                sendMessage2Client(msg, client);
            }
        }

    }


    public static void sendMessage2Client(Message msg, Messenger client){

        if (client != null){
            try {
                client.send(msg);
            } catch (RemoteException e) {
                Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
            }
        }
    }

    private ContentValues createRunningDownloadTaskById(int id){
        Downloader downloader = new Downloader();
        ContentValues data = downloader.loadData(PlayBackService.this, id);
        mRunningDownloadtask.put(id, data);
        return data;
    }

    private ContentValues removeRunningDownloadTaskById(int id){
        ContentValues data = mRunningDownloadtask.get(id);
        mRunningDownloadtask.remove(id);
        return data;
    }

    private ContentValues getRunningDownloadTaskByDownloadId(int id){
        return mRunningDownloadtask.get(id);
    }

    private List<ContentValues> loadReclist(int novelid, boolean remote){
        Record record = new Record();
        List<ContentValues> newdatas = null;

        if (remote){

            String date = Myfunc.ltime2Sdate(record.getMark(PlayBackService.this, novelid));
            String api = Myfunc.getRecordApi()+"?after="+Uri.encode(date)+"&perpage=9999&novel_id="+novelid;

            try {
                newdatas = record.getData(api);
                if (!newdatas.isEmpty()) {
                    record.saveData(PlayBackService.this, newdatas);
                }
                record.setMark(PlayBackService.this, novelid);
            } catch (IOException e) {
                if (e!=null && e.getMessage()!= null)
                    Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
            } catch (JSONException e) {
                if (e!= null && e.getMessage()!= null)
                    Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
            }

        }
        // load it from database;
        newdatas = record.loadDataByNovelId(PlayBackService.this, novelid);

        // load playing status
        for(int i=0; i<newdatas.size(); ++i){
            ContentValues data = newdatas.get(i);
            data.put("item_pos",i);
            data.put("item_mode", 0);
            // item status
            if (data.getAsInteger(Record.ID) == mMp3Status.itemId){
                data.put("player_status", mMp3Status.status);
                data.put("player_curpos", mMp3Status.position);
                data.put("player_duration", mMp3Status.duration);
                data.put("player_buffer", 0);
                mMp3Status.itemPos = data.getAsInteger("item_pos");
            }else{
                data.put("player_status", PlayBackService.STA_IDLE);
                data.put("player_curpos", 0);
                data.put("player_duration", 0);
                data.put("player_buffer", 0);
            }

            // load download status
            int downloadid = data.getAsInteger(Record.DOWNLOADID);
            if (downloadid > 0){
                // load the download Task info
                // 1 check if the download task is running.
                // 2 else load it from db;
                ContentValues downloaddata = getRunningDownloadTaskByDownloadId(downloadid);
                if (downloaddata == null){
                    downloaddata = Downloader.loadData(PlayBackService.this, downloadid);
                }
                data.putAll(downloaddata);
            }
        }
        return newdatas;
    }

    /* playback info define */
    public static class Status{
        public Status(){
            status      = PlayBackService.STA_IDLE;
            //position    = 0x0;
            //duration    = 0x0;
        }
        public boolean canStart (){
            return status == PlayBackService.STA_READY || status == PlayBackService.STA_PAUSED;
        }
        public boolean canPaused () {
            return status == PlayBackService.STA_STARTED || status == PlayBackService.STA_PAUSED;
        }
        public boolean canSeekto () {
            return status == PlayBackService.STA_READY || status == PlayBackService.STA_STARTED ||status == PlayBackService.STA_PAUSED;
        }
        public boolean canGetPostion () {
            return (status != PlayBackService.STA_IDLE && status != PlayBackService.STA_PREPARING && status != PlayBackService.STA_ERROR);
        }


        public void updateStatus (Integer status, SparseArray<Messenger> clients){
            if (status != null)
                this.status = status;
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_MP3STA_UPDATE);
            msg.obj = mMp3Status;
            sendMessage(msg, clients);
            //clients.valueAt(i).send(msg);
        }

        public void updateStatus () {
            this.status = PlayBackService.STA_IDLE;
            this.position = 0;
            this.duration = 0;
            this.err_extra =0;
            this.err_what  = 0;
            this.buffer_percent = 0;
            this.itemId = -1;
            this.itemPos = -1;
            this.playMode = -1;
            this.contentId = -1;
        }

        public void updateMP3Progress (SparseArray<Messenger> clients) {
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_MP3PROGRESS_UPDTED);
            msg.obj = mMp3Status;
            sendMessage2Client(msg, clients.get(PlayingListActivity.mClientId.getClientID()));
        }
        /*
        public void updateMP3BufferProgress (SparseArray<Messenger> clients) {
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_MP3BUFFERING_UPDATED);
            msg.obj = mMp3Status;
            sendMessage2Client(msg, clients.get(PlayingListActivity.mClientId.getClientID()));
        }
        */

        public int status;
        //public String title;
        public int position;
        public int duration;
        public int err_what;
        public int err_extra;
        public int buffer_percent;
        public int itemPos = -1;
        public int itemId = -1;
        public int playMode = MyConstant.PM_NOVEL;
        //public int novelId = -1;
        public int contentId = -1;
    }

    /* playback function define */
    class PlayBackHandler extends Handler {
        @Override
        public void handleMessage(Message msg){
            int downloadid;
            ContentValues downloadtask = null;
            Message rmsg;
            Integer[] iparams;
            int recid;
            int novelid;
            int strategy;

            switch (msg.what){
                case MSG_LOGIN:
                    addClient(msg.arg1, msg.replyTo);
                    break;
                case MSG_LOGOUT:
                    removeClient(msg.arg1);
                    break;
                case MSG_PLAY:
                    /* init mMp3Status here! very important, take care */
                    /* play means stop the current one, and play next*/
                    // call the client to clean up the last playing item
                    if (msg.arg1 != mMp3Status.itemId) {
                        rmsg = Message.obtain(null, PlayingListActivity.MSG_ON_CLEAN_UP_ITEM_UI);
                        rmsg.arg1 = mMp3Status.itemId;
                        rmsg.arg2 = mMp3Status.itemPos;
                        sendMessage2Client(rmsg, mClients.get(PlayingListActivity.mClientId.getClientID()));
                    }

                    // clean the all the status;
                    mMp3Status.updateStatus();
                    String params = (String)msg.obj;
                    String[] param = params.split(" ");
                    String url = param[0];
                    mMp3Status.contentId = Integer.valueOf(param[1]);
                    mMp3Status.itemId = msg.arg1;
                    mMp3Status.itemPos = msg.arg2;

                    play(url);
                    break;
                case MSG_PLAY_LOCAL:
                    if (msg.arg1 != mMp3Status.itemId){
                        rmsg = Message.obtain(null, PlayingListActivity.MSG_ON_CLEAN_UP_ITEM_UI);
                        rmsg.arg1 = mMp3Status.itemId;
                        rmsg.arg2 = mMp3Status.itemPos;
                        sendMessage2Client(rmsg, mClients.get(PlayingListActivity.mClientId.getClientID()));
                    }
                    mMp3Status.updateStatus();
                    params = (String)msg.obj;
                    param = params.split(" ");
                    String file = param[0];
                    mMp3Status.contentId = Integer.valueOf(param[1]);
                    mMp3Status.itemId    = msg.arg1;
                    mMp3Status.itemPos   = msg.arg2;

                    Uri uri = Uri.parse(file);
                    play(uri);

                    break;
                case MSG_START:
                    int result = requestAudioFocus();
                    if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                        start();
                    }
                    break;
                case MSG_PAUSE:
                    pause();
                    abandonAudioFocus();
                    break;
                case MSG_SEEKTO:
                    seekTo(msg.arg1);
                    break;
                case MSG_CURRENT_STATUS:
                    rmsg = Message.obtain(null, PlayingListActivity.MSG_ON_CURRENT_STATUS);
                    rmsg.obj = mMp3Status;
                    PlayBackService.sendMessage(rmsg, mClients);
                    break;
                case MSG_START_PLAYER_PGROGRESS_UPDATER:
                    startUpdater();
                    break;
                case MSG_STOP_PLAYER_PROGRESS_UPDATER:
                    stopUpdater();
                    break;
                case MSG_LOAD_RECORD_LIST:
                    Record record = new Record();
                    int force = msg.arg2;
                    long time = record.getMark(PlayBackService.this, msg.arg1);
                    if ((force == 1 || Myfunc.diffHour(time)>=6) && mDoingWhat.getLoadingRec() == false){
                        new GetRecordListTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, msg.arg1);
                        Toast.makeText(PlayBackService.this, R.string.load_record_list, Toast.LENGTH_LONG).show();
                    } else if (mDoingWhat.getLoadingRec() == false){
                        List<ContentValues> datas = loadReclist(msg.arg1, false);
                        msg = Message.obtain(null,PlayingListActivity.MSG_ON_LOAD_RECORD_LIST_DONE);
                        msg.obj = datas;
                        sendMessage2Client(msg, mClients.get(PlayingListActivity.mClientId.getClientID()));
                    }
                    break;

                case MSG_START_DOWNLOAD_REC:
                    iparams = (Integer[])msg.obj;
                    recid = msg.arg1;
                    novelid = iparams[0];
                    strategy = iparams[2];
                    downloadid = iparams[1];
                    if (downloadid > 0) {
                        // find the downloader first
                        downloadtask = createRunningDownloadTaskById(downloadid);
                        if (downloadtask != null) {
                            new DownloadTask(downloadtask,msg.arg1, msg.arg2).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, strategy, recid, novelid);
                        }
                    }else{
                        Log.e(MyConstant.TAG_DOWNLOADER, "had not create the download task!");
                    }
                    break;

                case MSG_PAUSE_DOWNLOAD_REC:
                    downloadid = (Integer)msg.obj;
                    downloadtask = removeRunningDownloadTaskById(downloadid);
                    if (downloadtask != null){
                        new Downloader().pauseDownload(downloadtask);
                    }
                    break;

                case MSG_DELETE_DOWNLOAD_REC:
                    // TODO : delete the downtask
                    // 1 check the download status, if downloading. stop it
                    // 2 delete the downtask from db;

                    downloadid = msg.arg1;
                    downloadtask = removeRunningDownloadTaskById(downloadid);
                    if (downloadtask != null) {
                        new Downloader().pauseDownload(downloadtask);
                    }
                    rmsg = Message.obtain();
                    rmsg.what = PlayingListActivity.MSG_ON_DOWNLOAD_DELETE;
                    rmsg.arg1 = msg.arg1;
                    rmsg.arg2 = msg.arg2;
                    PlayBackService.this.sendMessage(rmsg, mClients);
                    break;

                default:
                    super.handleMessage(msg);
            }
        }
    }


    class DownloadTask extends AsyncTask<Integer, Integer, Integer>
                        implements Downloader.OnProgressListener
                                  ,Downloader.OnDownloadDoneListener
                                  ,Downloader.OnDownloadPauseListener
                                  ,Downloader.OnDownloadErrListener
                                  ,Downloader.OnDownloadStartedListener
                                  ,Downloader.OnDownloadPreparingListener{

        public DownloadTask(ContentValues downloadtask, int id, int pos){
            this.itemId = id;
            this.itemPos = pos;
            this.downloadtask = downloadtask;
        }

        @Override
        protected Integer doInBackground(Integer... params) {

            //ContentValues downloadtask = params[0];
            try {
                int strategy = params[0];
                int recid    = params[1];
                int novelid  = params[2];
                new Downloader().setProgressListener(this)
                                .setDownloadDoneListener(this)
                                .setDownloadErrListener(this)
                                .setDownloadPauseListener(this)
                                .setDownloadStartedListener(this)
                                .setDownloadPreparingListener(this)
                                .startDownload(PlayBackService.this, downloadtask, strategy, Myfunc.getDownloadConnectionHeader(recid,novelid));

            } catch (IOException e) {
                Log.e(MyConstant.TAG_DOWNLOADER, e.getMessage());
            }
            return 0;
        }

        @Override
        protected void onPostExecute (Integer result){

            int status = downloadtask.getAsInteger(Downloader.STATUS);
            Message msg = null;
            switch(status){
                case Downloader.STA_DONE:
                    msg = Message.obtain(null, PlayingListActivity.MSG_ON_DOWNLOAD_DONE);
                    break;
                case Downloader.STA_PAUSED:
                    msg = Message.obtain(null, PlayingListActivity.MSG_ON_DOWNLOAD_PAUSED);
                    break;
                case Downloader.STA_ERR:
                    msg = Message.obtain(null, PlayingListActivity.MSG_ON_DOWNLOAD_ERR);
                    break;
                default:
                    super.onPostExecute(result);
            }
            if (msg != null){
                msg.arg1 = itemId;
                msg.arg2 = itemPos;
                msg.obj = downloadtask;
                sendMessage(msg, mClients);
            }
            // if we get here the runtime download task is finished. so remove it from download task list;
            removeRunningDownloadTaskById(downloadtask.getAsInteger(Downloader.ID));
        }

        @Override
        protected void onProgressUpdate(Integer... values){
            int percent = values[0];
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_DOWNLOAD_PROGRESS);
            msg.obj = percent;
            msg.arg1 = itemId;
            msg.arg2 = itemPos;
            sendMessage(msg, mClients);
        }

        @Override
        public void onDownloadProgressUpdate(int contentlong, int done) {
            int percent = (int)(((float) done / (float) contentlong) * 100);
            publishProgress(percent);
        }

        @Override
        public void onDownloadDoneUpdate() {
            // not a UI thread try it
            /*
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_DOWNLOAD_DONE);
            msg.obj = downloadtask.getAsInteger(Downloader.STATUS);
            msg.arg1 = itemId;
            msg.arg2 = itemPos;
            sendMessage(msg, mClients);
            */
        }

        @Override
        public void onDownloadPauseUpdate() {
            // not a UI thread
            /*
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_DOWNLOAD_PAUSED);
            msg.obj = downloadtask.getAsInteger(Downloader.STATUS);
            msg.arg1 = itemId;
            msg.arg2 = itemPos;
            sendMessage(msg, mClients);
            */
        }

        @Override
        public void onDownloadErrUpdate() {
            /*
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_DOWNLOAD_ERR);
            msg.obj = downloadtask.getAsInteger(Downloader.STATUS);
            msg.arg1 = itemId;
            msg.arg2 = itemPos;
            sendMessage(msg, mClients);
            */
        }

        @Override
        public void onDownloadStartedUpdate() {
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_DOWNLOAD_STARTED);
            msg.obj = downloadtask;
            msg.arg1 = itemId;
            msg.arg2 = itemPos;
            sendMessage(msg, mClients);
        }

        @Override
        public void onDownloadPreparing(){
            Message msg  = Message.obtain();
            msg.arg1 = itemId;
            msg.arg2 = itemPos;
            msg.what = PlayingListActivity.MSG_ON_PREPARING_DOWNLOAD;
            msg.obj  = new Integer(Downloader.STA_PREPARING);
            sendMessage(msg, mClients);
        }

        int itemId;
        int itemPos;
        ContentValues downloadtask;
    }

    class GetRecordListTask extends AsyncTask<Integer, Integer, List<ContentValues>>{

        @Override
        protected void onPreExecute(){
            //Toast.makeText(PlayBackService.this, R.string.load_record_list, Toast.LENGTH_SHORT).show();
        }

        @Override
        protected List<ContentValues> doInBackground(Integer[] params) {
            Log.v(MyConstant.TAG_NOVEL, "loading Record list...");
            mDoingWhat.setLoadingRec(true);
            Integer novelid = params[0];
            List<ContentValues> newdatas = loadReclist(novelid, true);
            mDoingWhat.setLoadingRec(false);
            return newdatas;
        }

        @Override
        protected void onPostExecute (List<ContentValues> result){
            if (result != null){
                Log.v(MyConstant.TAG_NOVEL, "load done!");
                Message msg = Message.obtain(null,PlayingListActivity.MSG_ON_LOAD_RECORD_LIST_DONE);
                msg.obj = result;
                sendMessage2Client(msg, mClients.get(PlayingListActivity.mClientId.getClientID()));
            }
        }
    }
}
