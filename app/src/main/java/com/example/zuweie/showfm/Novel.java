package com.example.zuweie.showfm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by zuweie on 12/8/14.
 */
public class Novel extends MyData{

    public final static String TAB = "novel";
    public final static String ID = "id";
    public final static String NJNAME = "nj_name";
    public final static String NJID = "nj_id";
    public final static String NAME = "novel_name";
    public final static String POSTER = "poster";
    public final static String BODY = "body";
    public final static String UPDATED = "updated";
    public final static String STATUS = "status";
    public final static String KEYWORD = "keyword";
    public final static String CATEGORY = "category";

    private SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");


    public Novel() {
        super(TAB);
    }

    @Override
    public ContentValues j2d(JSONObject jelem) throws JSONException {

        ContentValues data = new ContentValues();

        if (!jelem.isNull(Novel.ID))
            data.put(Novel.ID, jelem.getInt(Novel.ID));
        if (!jelem.isNull(Novel.NJID))
            data.put(Novel.NJID, jelem.getString(Novel.NJID));
        if (!jelem.isNull(Novel.NAME))
            data.put(Novel.NJNAME, jelem.getString(Novel.NJNAME));
        if (!jelem.isNull(Novel.NAME))
            data.put(Novel.NAME, jelem.getString(Novel.NAME));
        if (!jelem.isNull(Novel.BODY))
            data.put(Novel.BODY, jelem.getString(Novel.BODY));
        if (!jelem.isNull(Novel.POSTER))
            data.put(Novel.POSTER, jelem.getString(Novel.POSTER));
        if (!jelem.isNull(Novel.KEYWORD))
            data.put(Novel.KEYWORD,jelem.getString(Novel.KEYWORD));
        if(!jelem.isNull(Novel.CATEGORY))
            data.put(Novel.CATEGORY, jelem.getString(Novel.CATEGORY));
        if(!jelem.isNull(Novel.UPDATED)) {
            try {
                String date = jelem.getString(Novel.UPDATED);
                long time = this.df.parse(date).getTime();
                data.put(Novel.UPDATED, time);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (!jelem.isNull(Novel.STATUS))
            data.put(Novel.STATUS,jelem.getInt(Novel.STATUS));

        return data;
    }

    @Override
    public ContentValues c2d(Cursor cursor) {

        ContentValues data = new ContentValues();
        data.put(Novel.ID, cursor.getInt(cursor.getColumnIndex(Novel.ID)));
        data.put(Novel.NJID, cursor.getString(cursor.getColumnIndex(Novel.NJID)));
        data.put(Novel.NJNAME, cursor.getString(cursor.getColumnIndex(Novel.NJNAME)));
        data.put(Novel.NAME, cursor.getString(cursor.getColumnIndex(Novel.NAME)));
        data.put(Novel.BODY, cursor.getString(cursor.getColumnIndex(Novel.BODY)));
        data.put(Novel.POSTER, cursor.getString(cursor.getColumnIndex(Novel.POSTER)));
        data.put(Novel.KEYWORD, cursor.getString(cursor.getColumnIndex(Novel.KEYWORD)));
        data.put(Novel.CATEGORY, cursor.getString(cursor.getColumnIndex(Novel.CATEGORY)));
        data.put(Novel.UPDATED, cursor.getLong(cursor.getColumnIndex(Novel.UPDATED)));
        data.put(Novel.STATUS, cursor.getInt(cursor.getColumnIndex(Novel.STATUS)));
        return data;
    }

    @Override
    public JSONArray getJa(String json) throws JSONException {
        JSONObject jdata = new JSONObject(json);
        JSONArray  jNovel = jdata.getJSONArray("novels");
        return jNovel;
    }

}
