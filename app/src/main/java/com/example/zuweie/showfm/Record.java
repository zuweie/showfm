package com.example.zuweie.showfm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by zuweie on 12/10/14.
 */
public class Record extends MyData{

    public final static String TAB = "record";
    public final static String ID  = "id";
    public final static String NJID = "nj_id";
    public final static String NJNAME = "nj_name";
    public final static String NAME = "name";
    public final static String URL  = "url";
    public final static String UPDATED = "updated";
    public final static String NOVELID = "novel_id";
    public final static String DOWNLOADID = "download_id";

    private static SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

    public Record() {
        super(TAB);
    }

    @Override
    public ContentValues j2d(JSONObject jelem) throws JSONException {
        ContentValues data = new ContentValues();
        if (!jelem.isNull(Record.ID))
            data.put(Record.ID, jelem.getInt(Record.ID));
        if (!jelem.isNull(Record.NJID))
            data.put(Record.NJID, jelem.getString(Record.NJID));
        if (!jelem.isNull(Record.NJNAME))
            data.put(Record.NJNAME, jelem.getString(Record.NJNAME));
        if (!jelem.isNull(Record.NAME))
            data.put(Record.NAME, jelem.getString(Record.NAME));
        if (!jelem.isNull(Record.URL))
            data.put(Record.URL, jelem.getString(Record.URL));
        if (!jelem.isNull(Record.UPDATED)) {
            try {
                String date = jelem.getString(Record.UPDATED);
                long time = this.df.parse(date).getTime();
                data.put(Record.UPDATED, time);
            } catch (ParseException e) {
                Log.e(MyConstant.TAG_RECORD_JSON, e.getMessage());
            }
        }
        if (!jelem.isNull(Record.NOVELID))
            data.put(Record.NOVELID, jelem.getInt(Record.NOVELID));
        return data;
    }

    @Override
    public ContentValues c2d(Cursor cursor) {
        ContentValues data = new ContentValues();

        data.put(Record.ID, cursor.getInt(cursor.getColumnIndex(Record.ID)));
        data.put(Record.NJID, cursor.getString(cursor.getColumnIndex(Record.NJID)));
        data.put(Record.NJNAME, cursor.getString(cursor.getColumnIndex(Record.NJNAME)));
        data.put(Record.NAME, cursor.getString(cursor.getColumnIndex(Record.NAME)));
        data.put(Record.URL, cursor.getString(cursor.getColumnIndex(Record.URL)));
        data.put(Record.UPDATED, cursor.getLong(cursor.getColumnIndex(Record.UPDATED)));
        data.put(Record.NOVELID, cursor.getInt(cursor.getColumnIndex(Record.NOVELID)));
        data.put(Record.DOWNLOADID, cursor.getInt(cursor.getColumnIndex(Record.DOWNLOADID)));

        return data;
    }

    @Override
    public JSONArray getJa(String json) throws JSONException {
        JSONObject jdata = new JSONObject(json);
        JSONArray  ja    = jdata.getJSONArray("records");
        return ja;
    }


    public void loadDownloader (Context c, List<ContentValues> datas) {
        MyOpenHelper dbh = new MyOpenHelper(c);
        SQLiteDatabase db = dbh.getWritableDatabase();
        Downloader downloader = new Downloader();
        for(int i=0; i<datas.size(); ++i){
            ContentValues data = datas.get(i);

            int downloadid = data.getAsInteger(Record.DOWNLOADID);

            if (downloadid > 0){
                ContentValues dd = downloader.loadData(db, downloadid);
                data.putAll(dd);
            }
        }
        db.close();
    }

    public List<ContentValues> loadDataByNovelId(Context c, int novelId){
        String selection = Record.NOVELID +" = \'"+novelId+"\'";
        return loadData(c,null,selection, null,Record.UPDATED + " desc ");
    }


    public int updateDataById(Context c, ContentValues data){
        String selection = Record.ID + " =\'" + data.getAsInteger(Record.ID) + "\'";
        return updateData(c, data, selection, null);
    }


    public int createDownloader(Context c, ContentValues rdata){

        // 1 check the External storage is valid?
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {

            Downloader downloader = new Downloader();
            Record     record     = new Record();
            Novel      novel      = new Novel();

            ContentValues ndata = novel.loadDataById(c, rdata.getAsInteger(Record.NOVELID));

            String path = c.getExternalFilesDir(Environment.DIRECTORY_MUSIC).getAbsolutePath()+"/";
            String filename = String.valueOf(System.currentTimeMillis()/1000)+".mp3";
            String url = ndata.getAsString(Novel.URL) + "/" +rdata.getAsString(Record.URL);

            long id = downloader.createData(c,filename, path, url);
            if (id > 0){
                rdata.put(Record.DOWNLOADID, id);
                ContentValues v = new ContentValues();
                v.put(Record.ID, rdata.getAsString(Record.ID));
                v.put(Record.DOWNLOADID, id);
                record.updateDataById(c,v);
            }
            return (int)id;
        }
        return -1;
    }

    public int deleteDownloader(Context c, ContentValues rdata){
        int downloadid = rdata.getAsInteger(Record.DOWNLOADID);
        if (downloadid > 0){
            Downloader downloader = new Downloader();
            ContentValues dd = downloader.loadData(c, downloadid);
            int r = downloader.deleteData(c,dd, true);
            rdata.put(Record.DOWNLOADID, -1);
            ContentValues v = new ContentValues();
            v.put(Record.ID, rdata.getAsInteger(Record.ID));
            v.put(Record.DOWNLOADID, -1);
            r = new Record().updateDataById(c, v);
            return r;
        }
        return 0;
    }
}
