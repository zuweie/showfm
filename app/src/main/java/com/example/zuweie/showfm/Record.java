package com.example.zuweie.showfm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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

    private SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

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
                ContentValues dd = downloader.loadData(c, downloadid);
                data.putAll(dd);
            }
        }
        db.close();
    }

    public void saveDownloader(Context c, int downloadid){
        ContentValues data = new ContentValues();
        data.put(Record.DOWNLOADID, downloadid);
        
    }
}
