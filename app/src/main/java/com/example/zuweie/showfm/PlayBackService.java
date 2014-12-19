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

import org.json.JSONException;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public class PlayBackService extends Service {

    private Messenger mMessenger = null;
    private Messenger mClient = null;
    private MediaPlayer mPlayback = null;

    /* Msg define */
    public final static int MSG_LOGIN = 0x1;
    public final static int MSG_LOGOUT = 0x2;
    public final static int MSG_PLAY = 0X3;
    public final static int MSG_PLAY_LOCAL = 0x7;
    public final static int MSG_START = 0x4;
    public final static int MSG_PAUSED = 0X5;
    public final static int MSG_SEEKTO = 0x6;
    public final static int MSG_LOAD_RECORD_LIST = 0x7;
    public final static int MSG_START_DOWNLOAD_REC = 0x8;
    public final static int MSG_PAUSE_DOWNLOAD_REC = 0x9;

    /* status define */
    public final static int STA_IDLE = 0x0;
    public final static int STA_STARTED = 0x1;
    public final static int STA_PAUSED = 0x2;
    public final static int STA_PREPARING = 0x3;
    public final static int STA_READY = 0x4;
    public final static int STA_ERROR = 0x5;
    public final static int STA_COMPLETED = 0x6;

    /* */
    private static Status mMp3Status;
    private DoingWhat mDoingWhat;
    private List<ContentValues> mDownloadtask = null;

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
                mMp3Status.updateStatus(PlayBackService.STA_ERROR,  mClient);
                return false;
            }
        });

        /* set BufferingUpdate Listener */
        mPlayback.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                mMp3Status.buffer_percent = percent;
                //mStatus.updateStatus(STA_PREPARING, mClient);
            }
        });

        /* set OnPrepared Listener */
        mPlayback.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mMp3Status.updateStatus(PlayBackService.STA_READY, mClient);

            }
        });

        /* set OnCompletion Listener */
        mPlayback.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mMp3Status.updateStatus(PlayBackService.STA_COMPLETED, mClient);
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
        //throw new UnsupportedOperationException("Not yet implemented");
        return mMessenger.getBinder();
    }

    /* playback function define */
    private void play(Context c, String uri){

        try {
            mPlayback.reset();
            mPlayback.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMp3Status.updateStatus();

            mPlayback.setDataSource(PlayBackService.this, Uri.parse(uri));
            mPlayback.prepareAsync();
            mMp3Status.updateStatus(PlayBackService.STA_PREPARING, mClient);

        } catch (IOException e) {
            Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
        }
    }

    private void play(String url){

        try {
            mPlayback.reset();
            mPlayback.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMp3Status.updateStatus();

            mPlayback.setDataSource(url);
            mPlayback.prepareAsync();
            mMp3Status.updateStatus(PlayBackService.STA_PREPARING,mClient);
        } catch (IOException e) {
            Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
        }

    }

    private void start() {
        if (mMp3Status.canStart()) {
            mPlayback.start();
            mMp3Status.updateStatus(PlayBackService.STA_STARTED, mClient);
        }
    }

    private void paused () {
        if (mMp3Status.canPaused()){
            if (mMp3Status.canPaused()){
                mPlayback.pause();
                mMp3Status.updateStatus(PlayBackService.STA_PAUSED, mClient);
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
            position    = 0x0;
            duration    = 0x0;
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

        public void updateStatus (Integer status, Messenger client){
            if (status != null)
                this.status = status;
            if (client != null){
                try {
                    Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_MP3STA_UPDATE);
                    msg.obj = mMp3Status;
                    client.send(msg);
                } catch (RemoteException e) {
                    Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                }
            }
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
        }
        public int status;
        //public String title;
        public int position;
        public int duration;
        public int err_what;
        public int err_extra;
        public int buffer_percent;
        public int itemPos;
        public int itemId;
    }

    /* playback function define */
    class PlayBackHandler extends Handler {
        @Override
        public void handleMessage(Message msg){
            int downloadid;
            ContentValues downloadtask = null;
            switch (msg.what){
                case MSG_LOGIN:
                    mClient = msg.replyTo;
                    break;
                case MSG_LOGOUT:
                    mClient = null;
                    break;
                case MSG_PLAY:
                    String url = (String)msg.obj;
                    play(url);
                    break;
                case MSG_START:
                    start();
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

                            //new Downloader().setListener(PlayBackService.this).startDownload(PlayBackService.this, downloadtask);
                        }
                    }else{
                        Log.e(MyConstant.TAG_DOWNLOADER, "had not create the download task!");
                    }

                    break;
                case MSG_PAUSE_DOWNLOAD_REC:
                    downloadid = msg.arg1;
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

    class DownloadTask extends AsyncTask<Integer, Integer, Integer> implements Downloader.OnProgressListener
                                                                          ,Downloader.OnDownloadDoneListener
                                                                          ,Downloader.OnDownloadPauseListener
                                                                          ,Downloader.OnDownloaderErrListener{

        public DownloadTask(ContentValues downloadtask, int id, int pos){
            this.itemId = id;
            this.itemPos = pos;
            this.downloadtask = downloadtask;
        }

        @Override
        protected void onPreExecute(){
            if (mClient != null){
                Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_READY_DOWNLOAD);
                try {
                    mClient.send(msg);
                } catch (RemoteException e) {
                    Log.e(MyConstant.TAG_DOWNLOADER, e.getMessage());
                }
            }
        }

        @Override
        protected Integer doInBackground(Integer... params) {

            //ContentValues downloadtask = params[0];
            try {
                new Downloader().setProgressListener(this).startDownload(PlayBackService.this, downloadtask);
            } catch (IOException e) {
                Log.e(MyConstant.TAG_DOWNLOADER, e.getMessage());
            }
            return 0;
        }

        @Override
        protected void onPostExecute (Integer result){
            int status = downloadtask.getAsInteger(Downloader.STATUS);
            if (mClient != null){
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
                    try {
                        msg.arg1 = itemId;
                        msg.arg2 = itemPos;
                        msg.obj = (Integer)status;
                        mClient.send(msg);
                    } catch (RemoteException e) {
                        Log.e(MyConstant.TAG_DOWNLOADER, e.getMessage());
                    }
                }
            }
            // at last remove the the download task from list
            mDownloadtask.remove(downloadtask);
        }

        @Override
        protected void onProgressUpdate(Integer... values){
            if (mClient != null){
                int percent = values[0];
                Message msg = Message.obtain(null, PlayingListActivity.MSG_ON_DOWNLOAD_PROGRESS);
                msg.obj = percent;
                msg.arg1 = itemId;
                msg.arg2 = itemPos;
                try {
                    mClient.send(msg);
                } catch (RemoteException e) {
                    Log.e(MyConstant.TAG_DOWNLOADER, e.getMessage());
                }
            }
        }

        @Override
        public void onDownloadProgressUpdate(int contentlong, int done) {
            int percent = (int)(((float) done / (float) contentlong) * 100);
            publishProgress(percent);
        }

        @Override
        public void onDownloadDoneUpdate() {
            // not a UI thread drop it
        }

        @Override
        public void onDownloadPauseUpdate() {
            // not a UI thread
        }

        @Override
        public void onDownloadErrUpdate() {
            // not a UI thread
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
            String date = record.getMark(PlayBackService.this);
            String api = "http://www.showfm.net/api/record.asp?after="+date+"&perpage=9999&novel_id="+novelid;
            List<ContentValues> newdatas = null;
            try {
                newdatas = record.getData(api);
                if (!newdatas.isEmpty()){
                    record.saveData(PlayBackService.this, newdatas);
                    record.setMark(PlayBackService.this);
                }
                // load the data from database;
                newdatas = record.loadData(PlayBackService.this, null, Record.NOVELID +" = \'"+novelid+"\'",null, Record.UPDATED + " desc ");
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
            if (mClient != null){
                try {
                    Message msg = Message.obtain(null,PlayingListActivity.MSG_ON_LOAD_RECORD_LIST_DONE);
                    msg.obj = result;
                    mClient.send(msg);
                } catch (RemoteException e) {
                    Log.e(MyConstant.TAG_PLAYBACK, e.getMessage());
                }
            }
        }
    }
}
