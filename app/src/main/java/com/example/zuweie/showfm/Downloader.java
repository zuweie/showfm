package com.example.zuweie.showfm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.webkit.DownloadListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by zuweie on 12/16/14.
 */
public class Downloader {

    public final static String TAB = "download";
    public final static String ID  = "d_id";
    public final static String PATH = "d_path";
    public final static String FILENAME = "d_filename";
    public final static String URL = "d_url";
    public final static String STATUS = "d_status";
    public final static String ERR = "d_err";
    public final static String FILESZ = "d_filesz";
    public final static String LASTPOS = "d_lastpos";

    public final static int STA_IDLE = 0;
    public final static int STA_STARTED = 1;
    public final static int STA_PAUSED = 2;
    public final static int STA_DONE = 3;
    public final static int STA_ERR = -1;

    public float mReportFrequency = 0.05f;
    private OnProgressListener mListener;
    private OnDownloadDoneListener mDoneListener;
    private OnDownloadPauseListener mPauseListener;
    private OnDownloadErrListener mErrListener;
    private OnDownloadStartedListener mStartedListener;

    /*listener*/
    public static interface OnProgressListener {
        public void onDownloadProgressUpdate(int contentlong, int done);
    }

    public static interface OnDownloadDoneListener {
        public void onDownloadDoneUpdate();
    }

    public static interface OnDownloadPauseListener {
        public void onDownloadPauseUpdate();
    }

    public static interface OnDownloadErrListener {
        public void onDownloadErrUpdate();
    }

    public static interface OnDownloadStartedListener {
        public void onDownloadStartedUpdate();
    }
    /*listener*/

    /* set listener */
    public Downloader setProgressListener(OnProgressListener l){
        mListener = l;
        return this;
    }

    public Downloader setDownloadDoneListener(OnDownloadDoneListener l){
        this.mDoneListener = l;
        return this;
    }

    public Downloader setDownloadPauseListener(OnDownloadPauseListener l){
        this.mPauseListener = l;
        return this;
    }

    public Downloader setDownloadErrListener(OnDownloadErrListener l){
        this.mErrListener = l;
        return this;
    }

    public Downloader setDownloadStartedListener(OnDownloadStartedListener l){
        this.mStartedListener = l;
        return this;
    }
    /* set listener */

    public ContentValues loadData ( SQLiteDatabase db, int id) {

        // close the db outside
        Cursor c = db.query(TAB, null, Downloader.ID + " = \'" + id + "\'", null, null, null, null);
        ContentValues data = new ContentValues();
        while (c.moveToNext()){
            data.put(Downloader.ID, c.getInt(c.getColumnIndex(Downloader.ID)));
            data.put(Downloader.PATH, c.getString(c.getColumnIndex(Downloader.PATH)));
            data.put(Downloader.FILENAME, c.getString(c.getColumnIndex(Downloader.FILENAME)));
            data.put(Downloader.URL, c.getString(c.getColumnIndex(Downloader.URL)));
            data.put(Downloader.STATUS, c.getInt(c.getColumnIndex(Downloader.STATUS)));
            data.put(Downloader.ERR, c.getString(c.getColumnIndex(Downloader.ERR)));
            data.put(Downloader.FILESZ, c.getInt(c.getColumnIndex(Downloader.FILESZ)));
            data.put(Downloader.LASTPOS, c.getInt(c.getColumnIndex(Downloader.LASTPOS)));
        }
        c.close();
        return data;
    }

    public static ContentValues loadData(Context c, int id){
        MyOpenHelper dbh = new MyOpenHelper(c);
        SQLiteDatabase db = dbh.getWritableDatabase();
        Cursor cursor = db.query(TAB, null, Downloader.ID + " = \'"+id+"\'", null, null, null, null);
        ContentValues data = new ContentValues();
        while (cursor.moveToNext()){
            data.put(Downloader.ID, cursor.getInt(cursor.getColumnIndex(Downloader.ID)));
            data.put(Downloader.PATH, cursor.getString(cursor.getColumnIndex(Downloader.PATH)));
            data.put(Downloader.FILENAME, cursor.getString(cursor.getColumnIndex(Downloader.FILENAME)));
            data.put(Downloader.URL, cursor.getString(cursor.getColumnIndex(Downloader.URL)));
            data.put(Downloader.STATUS, cursor.getInt(cursor.getColumnIndex(Downloader.STATUS)));
            data.put(Downloader.ERR, cursor.getString(cursor.getColumnIndex(Downloader.ERR)));
            data.put(Downloader.FILESZ, cursor.getInt(cursor.getColumnIndex(Downloader.FILESZ)));
            data.put(Downloader.LASTPOS, cursor.getInt(cursor.getColumnIndex(Downloader.LASTPOS)));
        }
        cursor.close();
        db.close();
        return data;
    }

    public static long saveData (Context c, ContentValues data){
        MyOpenHelper dbh = new MyOpenHelper(c);
        SQLiteDatabase db = dbh.getWritableDatabase();
        long rid = db.insert(TAB, null, data);
        db.close();
        return rid;
    }


    public static int updateData (Context c, ContentValues data){

        //ContentValues v = new ContentValues();
        //v.put(Downloader.STATUS, getState());
        if (data.get(Downloader.ID) != null){

            MyOpenHelper dbh = new MyOpenHelper(c);
            SQLiteDatabase db = dbh.getWritableDatabase();
            String wherecase = Downloader.ID + " = \'" + data.getAsInteger(Downloader.ID) + "\' ";
            int r = db.update(Downloader.TAB, data, wherecase, null);
            db.close();
            return r;
        }
        return 0;
    }

    public static long createData(Context c, String filename, String path, String url) {
        ContentValues data = new ContentValues();
        data.put(Downloader.FILENAME, filename);
        data.put(Downloader.PATH, path);
        data.put(Downloader.URL, url);
        long rid = saveData(c, data);
        return rid;
    }

    public static int deleteData(Context c, ContentValues data, boolean deletefile){

        if (deletefile){
            // delete the file first
            String f = data.getAsString(Downloader.PATH) + data.getAsString(Downloader.FILENAME);
            File file = new File(f);
            if (file.exists()){
                file.delete();
            }
        }

        MyOpenHelper dbh = new MyOpenHelper(c);
        SQLiteDatabase db = dbh.getWritableDatabase();
        String wherecase = Downloader.ID + " =\'"+data.getAsInteger(Downloader.ID) + "\'";
        int r = db.delete(Downloader.TAB, wherecase, null);
        db.close();
        return r;
    }


    public HttpURLConnection getConnection(long downloadStart, ContentValues data) throws IOException, NoSuchAlgorithmException {

        String itemurl = data.getAsString(Downloader.URL);

        URL url = new URL(Myfunc.getValidUrl("",itemurl, 900));

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(5 * 1000);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
        conn.setRequestProperty("Accept-Language", "zh-CN");
        //conn.setRequestProperty("Referer", mUrl);
        conn.setRequestProperty("Charset", "UTF-8");
        conn.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
        conn.setRequestProperty("Connection", "Keep-Alive");
        if (downloadStart > 0)
            conn.setRequestProperty("Range", "bytes="+downloadStart+"-");
        return  conn;
    }

    public int startDownload(Context c, ContentValues downloadtask) throws IOException {

        // get a ouput file
        int sz = 0;
        final int BUFFERSZ = 1024 * 10; // 10 K
        byte[] buffer = new byte[BUFFERSZ];
        long lastpos=0;
        long contentlong = 0;
        FileOutputStream fos = null;
        HttpURLConnection connection = null;
        BufferedInputStream in = null;
        BufferedOutputStream bos = null;
        File file = null;

        synchronized (downloadtask){

            if (downloadtask.getAsInteger(Downloader.STATUS) == Downloader.STA_IDLE
            || downloadtask.getAsInteger(Downloader.STATUS) == Downloader.STA_PAUSED){
                try{
                    Log.v(MyConstant.TAG_DOWNLOADER, "get ready to download");
                    String f = downloadtask.getAsString(Downloader.PATH) + downloadtask.getAsString(Downloader.FILENAME);
                    file = new File(f);
                    if (downloadtask.getAsInteger(Downloader.STATUS) == Downloader.STA_IDLE){
                        // create the new download file;
                        fos = new FileOutputStream(f);
                    }else if (downloadtask.getAsInteger(Downloader.STATUS) == Downloader.STA_PAUSED){
                        // open the old download file;
                        fos = new FileOutputStream(f, true);
                        //file = new File(f);
                        if (file.exists()) {
                            lastpos = file.length();
                            Log.d(MyConstant.TAG_DOWNLOADER, "download start "+lastpos);
                        }
                    }
                    connection = getConnection(lastpos, downloadtask);
                    connection.connect();
                    // for debug
                    printHeader(connection.getHeaderFields());

                    if (downloadtask.getAsInteger(Downloader.STATUS) == Downloader.STA_IDLE){
                        contentlong = connection.getContentLength();
                        downloadtask.put(Downloader.FILESZ, contentlong);
                    }else{
                        contentlong = downloadtask.getAsInteger(Downloader.FILESZ);
                    }

                    // input
                    in = new BufferedInputStream(connection.getInputStream());
                    // ouput
                    bos = new BufferedOutputStream(fos, BUFFERSZ);
                    downloadtask.put(Downloader.STATUS, Downloader.STA_STARTED);
                    onDownloadStarted();
                    Log.v(MyConstant.TAG_DOWNLOADER, "starting downloading...");
                }catch (IOException e){

                    downloadtask.put(Downloader.STATUS, Downloader.STA_ERR);
                    downloadtask.put(Downloader.ERR, e.getMessage());
                    downloadtask.put(Downloader.LASTPOS, lastpos);
                    onDownloadErr();
                    updateData(c, downloadtask);

                    // close the input stream and out put output stream.
                    if (in != null) in.close();
                    if (bos != null) bos.close();
                    throw new IOException(e);
                }catch (NoSuchAlgorithmException e){
                    Log.e(MyConstant.TAG_DOWNLOADER, e.getMessage());
                }
            }
        }

        try{
            float lastpercent = 0.0f;

            while (downloadtask.getAsInteger(Downloader.STATUS) == Downloader.STA_STARTED && (sz = in.read(buffer))>=0 ){

                Log.v(MyConstant.TAG_DOWNLOADER, "read size " + sz);
                bos.write(buffer, 0, sz);

                // progress updated
                lastpos += sz;
                float curpercent = (float)lastpos / (float)contentlong;
                if ((curpercent - lastpercent) >= mReportFrequency) {
                    onUpDateProgress((int) contentlong, (int) lastpos);
                    lastpercent = curpercent;
                }
            }

            synchronized (downloadtask){
                if (sz < 0){
                    // that is finished
                    //setState(Downloader.STA_FINISHED);
                    downloadtask.put(Downloader.STATUS, Downloader.STA_DONE);
                    onDownloadDone();
                    Log.d(MyConstant.TAG_DOWNLOADER, "download file length: "+file.length()+" content length: "+ contentlong);
                    Log.v(MyConstant.TAG_DOWNLOADER, " donwload done!");
                }
                // this downloader status is pause !
                if (downloadtask.getAsInteger(Downloader.STATUS) == Downloader.STA_PAUSED) {
                    onDownloadPause();
                    Log.d(MyConstant.TAG_DOWNLOADER, "download paused! last read pos is "+lastpos+" file saved size is "+file.length());
                }

                // update it data to db
                updateData(c, downloadtask);
                return 0;
            }
        }catch (IOException e){
            synchronized (downloadtask){
                downloadtask.put(Downloader.STATUS, Downloader.STA_ERR);
                downloadtask.put(Downloader.ERR, e.getMessage());
                downloadtask.put(Downloader.LASTPOS, lastpos);
                updateData(c, downloadtask);
                onDownloadErr();
                // close the input stream and out put output stream.
                if (in != null) in.close();
                if (bos != null) bos.close();
                throw new IOException(e);
            }
        }
    }

    public int pauseDownload (ContentValues downloadtask) {
        if (downloadtask.getAsInteger(Downloader.STATUS) == Downloader.STA_STARTED){
            downloadtask.put(Downloader.STATUS, Downloader.STA_PAUSED);
            Log.v(MyConstant.TAG_DOWNLOADER, "Pause Download task!");
        }
        return 0;
    }

    private void printHeader(Map<String, List<String>> header){
        Log.d(MyConstant.TAG_DOWNLOADER, "Print Header: ");
        Iterator<String> it = header.keySet().iterator();
        while(it.hasNext()){
            String key = it.next();
            List<String> vl= header.get(key);
            StringBuffer bf = new StringBuffer("");
            for(int i=0; i<vl.size(); ++i){
                bf.append(vl.get(i) + " ");
            }
            Log.d(MyConstant.TAG_DOWNLOADER, key+" : "+bf.toString());
        }
    }

    private void onUpDateProgress (int contentlong, int done){
        mListener.onDownloadProgressUpdate(contentlong, done);
    }

    private void onDownloadDone(){
        mDoneListener.onDownloadDoneUpdate();
    }

    private void onDownloadPause () {
        mPauseListener.onDownloadPauseUpdate();
    }

    private void onDownloadErr () {
        mErrListener.onDownloadErrUpdate();
    }

    private void onDownloadStarted () {
        mStartedListener.onDownloadStartedUpdate();
    }


}
