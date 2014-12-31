package com.example.zuweie.showfm;

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

import org.json.JSONException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PlayBackService extends Service {

    private Messenger mMessenger = null;
    //private Messenger mClient = null;
    private SparseArray <Messenger> mClients = null;
    private MediaPlayer mPlayback = null;

    /* Msg define */
    public final static int MSG_LOGIN = 0x1;
    public final static int MSG_LOGOUT = 0x2;
    public final static int MSG_PLAY = 0X3;
    public final static int MSG_START = 0x4;
    public final static int MSG_PAUSE = 0X5;
    public final static int MSG_SEEKTO = 0x6;
    public final static int MSG_LOAD_RECORD_LIST = 0x7;
    public final static int MSG_START_DOWNLOAD_REC = 0x8;
    public final static int MSG_PAUSE_DOWNLOAD_REC = 0x9;
    public final static int MSG_CURRENT_STATUS =    0x10;

    /* status define */
    public final static int STA_IDLE = 0x0;
    public final static int STA_STARTED = 0x1;
    public final static int STA_PAUSED = 0x2;
    public final static int STA_PREPARING = 0x3;
    //public final static int STA_BUFFERING = 0x7;
    public final static int STA_READY = 0x4;
    public final static int STA_ERROR = 0x5;
    public final static int STA_COMPLETED = 0x6;

    /* */
    private static Status mMp3Status;
    private DoingWhat mDoingWhat;
    private List<ContentValues> mDownloadtask = null;
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
                stopUpdater();
                mMp3Status.err_extra = extra;
                mMp3Status.err_what  = what;
                mMp3Status.updateStatus(PlayBackService.STA_ERROR,  mClients);
                return false;
            }
        });

        /* set BufferingUpdate Listener */
        mPlayback.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                if (percent - mMp3Status.buffer_percent > 4){
                    mMp3Status.buffer_percent = percent;
                    mMp3Status.updateMP3BufferProgress(mClients);
                    Log.v(MyConstant.TAG_PLAYBACK, "Updated buffer percent : " + percent);
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
                //mMp3Status.updateStatus(PlayBackService.STA_COMPLETED, mClient);
                stopUpdater();
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

        mDownloadtask = new LinkedList<ContentValues>();

        mClients = new SparseArray<Messenger>();

        return START_STICKY;
    }

    @Override
    public void onDestroy (){
        mPlayback.release();
        mPlayback = null;

        // check the download task if exists stop it
        Log.v(MyConstant.TAG_DOWNLOADER, "pause all the not finished mask");
        Downloader downloader = new Downloader();
        for(int i=0; i<mDownloadtask.size(); ++i){
            ContentValues downloadtask = mDownloadtask.get(i);
            downloader.pauseDownload(downloadtask);
        }
        mDownloadtask.clear();
        super.onDestroy();
    }
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mMessenger.getBinder();
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

    private void start() {
        if (mMp3Status.canStart()) {
            mPlayback.start();
            mMp3Status.updateStatus(PlayBackService.STA_STARTED, mClients);
        }
    }

    private void paused () {
        if (mMp3Status.canPaused()){
            if (mMp3Status.canPaused()){
                mPlayback.pause();
                mMp3Status.updateStatus(PlayBackService.STA_PAUSED, mClients);
            }
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
                try {
                    clients.valueAt(i).send(msg);
                } catch (RemoteException e) {
                    Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                }
            }
        }
    }

    public static void sendMessage2Client(Message msg, SparseArray<Message>clients, int key){

    }

    private ContentValues createDownloadTaskById(int id){
        Downloader downloader = new Downloader();
        ContentValues data = downloader.loadData(PlayBackService.this, id);
        mDownloadtask.add(data);
        return data;
    }

    private ContentValues removeDownloadTaskById(int id){
        for(int i=0; i<mDownloadtask.size(); ++i){
            ContentValues downloader =  mDownloadtask.get(i);
            if(downloader.getAsInteger(Downloader.ID) == id){
                //return downloader;
                return mDownloadtask.remove(i);
            }
        }
        return null;
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
            this.novelId = -1;
        }

        public void updateMP3Progress (SparseArray<Messenger> clients){
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_MP3PROGRESS_UPDTED);
            msg.obj = mMp3Status;
            sendMessage(msg, clients);
        }

        public void updateMP3BufferProgress(SparseArray<Messenger> clients){
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_MP3BUFFERING_UPDATED);
            msg.obj = mMp3Status;
            sendMessage(msg, clients);
        }
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
        public int novelId = -1;
    }

    /* playback function define */
    class PlayBackHandler extends Handler {
        @Override
        public void handleMessage(Message msg){
            int downloadid;
            ContentValues downloadtask = null;
            switch (msg.what){
                case MSG_LOGIN:
                    //mClient = msg.replyTo;
                    addClient(msg.arg1, msg.replyTo);
                    break;
                case MSG_LOGOUT:
                    //mClient = null;
                    removeClient(msg.arg1);
                    stopUpdater();
                    break;
                case MSG_PLAY:
                    // clean the all the status;
                    mMp3Status.updateStatus();
                    stopUpdater();
                    String url = (String)msg.obj;
                    mMp3Status.itemId = msg.arg1;
                    mMp3Status.itemPos = msg.arg2;
                    play(url);
                    break;
                case MSG_START:
                    start();
                    if (msg.arg1 == 1) {
                        startUpdater();
                    }
                    break;
                case MSG_PAUSE:
                    paused();
                    stopUpdater();
                    break;
                case MSG_SEEKTO:
                    seekTo(msg.arg1);
                    break;
                case MSG_CURRENT_STATUS:
                    Message rmsg = Message.obtain(null, PlayingListActivity.MSG_ON_CURRENT_STATUS);
                    rmsg.obj = mMp3Status;
                    PlayBackService.sendMessage(rmsg, mClients);
                    break;
                case MSG_LOAD_RECORD_LIST:
                    if (mDoingWhat.getLoadingRec() == false){
                        new GetRecordListTask().execute(msg.arg1);
                    }
                    break;

                case MSG_START_DOWNLOAD_REC:
                    downloadid = (Integer)msg.obj;
                    if (downloadid > 0) {
                        // find the downloader first
                        downloadtask = createDownloadTaskById(downloadid);

                        if (downloadtask != null) {

                            new DownloadTask(downloadtask, msg.arg1, msg.arg2).execute();

                        }
                    }else{
                        Log.e(MyConstant.TAG_DOWNLOADER, "had not create the download task!");
                    }
                    break;

                case MSG_PAUSE_DOWNLOAD_REC:
                    downloadid = (Integer)msg.obj;
                    downloadtask = removeDownloadTaskById(downloadid);
                    if (downloadtask != null){
                        new Downloader().pauseDownload(downloadtask);
                    }
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
                                  ,Downloader.OnDownloadStartedListener{

        public DownloadTask(ContentValues downloadtask, int id, int pos){
            this.itemId = id;
            this.itemPos = pos;
            this.downloadtask = downloadtask;
        }

        @Override
        protected void onPreExecute(){
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_READY_DOWNLOAD);
            sendMessage(msg, mClients);
        }

        @Override
        protected Integer doInBackground(Integer... params) {

            //ContentValues downloadtask = params[0];
            try {
                new Downloader().setProgressListener(this)
                                .setDownloadDoneListener(this)
                                .setDownloadErrListener(this)
                                .setDownloadPauseListener(this)
                                .setDownloadStartedListener(this)
                                .startDownload(PlayBackService.this, downloadtask);

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
                msg.obj = (Integer)status;
                sendMessage(msg, mClients);
            }
            // at last remove the the download task from list
            mDownloadtask.remove(downloadtask);
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
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_DOWNLOAD_DONE);
            msg.obj = downloadtask.getAsInteger(Downloader.STATUS);
            msg.arg1 = itemId;
            msg.arg2 = itemPos;
            sendMessage(msg, mClients);
        }

        @Override
        public void onDownloadPauseUpdate() {
            // not a UI thread
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_DOWNLOAD_PAUSED);
            msg.obj = downloadtask.getAsInteger(Downloader.STATUS);
            msg.arg1 = itemId;
            msg.arg2 = itemPos;
            sendMessage(msg, mClients);
        }

        @Override
        public void onDownloadErrUpdate() {
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_DOWNLOAD_ERR);
            msg.obj = downloadtask.getAsInteger(Downloader.STATUS);
            msg.arg1 = itemId;
            msg.arg2 = itemPos;
            sendMessage(msg, mClients);
        }

        @Override
        public void onDownloadStartedUpdate() {
            Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_DOWNLOAD_STARTED);
            msg.obj = downloadtask.getAsInteger(Downloader.STATUS);
            msg.arg1 = itemId;
            msg.arg2 = itemPos;
            sendMessage(msg, mClients);
        }

        int itemId;
        int itemPos;
        ContentValues downloadtask;
    }

    class GetRecordListTask extends AsyncTask<Integer, Integer, List<ContentValues>>{

        @Override
        protected List<ContentValues> doInBackground(Integer[] params) {

            mDoingWhat.setLoadingRec(true);

            Integer novelid = params[0];

            //List<ContentValues> datas;
            Record record = new Record();
            // load the update form api
            String date = Myfunc.ltime2Sdate(record.getMark(PlayBackService.this, novelid));
            String api = "http://www.showfm.net/api/record.asp?after="+Uri.encode(date)+"&perpage=9999&novel_id="+novelid;
            List<ContentValues> newdatas = null;
            try {
                newdatas = record.getData(api);
                if (!newdatas.isEmpty()){
                    record.saveData(PlayBackService.this, newdatas);
                    record.setMark(PlayBackService.this, novelid);
                }
                // load the data from database;
                newdatas = record.loadDataByNovelId(PlayBackService.this, novelid);
                record.loadDownloader(PlayBackService.this, newdatas);

            } catch (IOException e) {
                Log.e(MyConstant.TAG_RECORD_API, e.getMessage());
            } catch (JSONException e) {
                Log.e(MyConstant.TAG_RECORD_JSON, e.getMessage());
            }
            mDoingWhat.setLoadingRec(false);
            return newdatas;
        }

        @Override
        protected void onPostExecute (List<ContentValues> result){
            if (result != null){
                Message msg = Message.obtain(null,PlayingListActivity.MSG_ON_LOAD_RECORD_LIST_DONE);
                ContentValues data;
                // init the ui data
                for(int i=0; i<result.size(); ++i){
                    data = result.get(i);
                    data.put("item_pos",i);
                    data.put("item_mode", 0);
                    data.put("player_status", PlayBackService.STA_IDLE);
                    data.put("player_curpos", 0);
                    data.put("player_duration", 0);
                    data.put("player_buffer", 0);
                }
                msg.obj = result;
                sendMessage(msg, mClients);
            }
        }
    }
}
