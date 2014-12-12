package com.example.zuweie.showfm;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.DateFormat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by zuweie on 12/8/14.
 */

public abstract class MyData {

    private String mTab;
    //private String[] mFields;

    private MyData(){}

    public MyData(String tab){
        this.mTab    = tab;
    }

    public abstract ContentValues j2d(JSONObject jelem) throws JSONException;
    public abstract ContentValues c2d(Cursor cursor);
    public abstract JSONArray getJa(String json) throws JSONException;

    public String getJson(String url) throws IOException{
        return GlNetfunc.getInstance().get(url);
    }

    public List<ContentValues> ja2ds (JSONArray jdatas) throws JSONException{

        List<ContentValues> datas = new ArrayList<ContentValues>();
        for(int i=0; i<jdatas.length(); ++i){

            JSONObject jelem = jdatas.getJSONObject(i);
            ContentValues data = j2d(jelem);
            datas.add(data);
        }
        return datas;
    }

    public List<ContentValues> getData (String url) throws IOException, JSONException{
        String json = getJson(url);
        JSONArray ja = getJa(json);
        List<ContentValues> datas = ja2ds(ja);
        return datas;
    }

    public long saveData(Context c, List<ContentValues> datas){
        MyOpenHelper dbh = new MyOpenHelper(c);
        SQLiteDatabase db = dbh.getWritableDatabase();
        long r = -1;
        for(int i=0; i<datas.size(); ++i){
            ContentValues data = datas.get(i);
            r = db.insert(mTab, null, data);
        }
        db.close();
        return r;
    }

    public int updateData(Context c, List<ContentValues> datas){

        MyOpenHelper dbh = new MyOpenHelper(c);
        SQLiteDatabase db = dbh.getWritableDatabase();
        int r = 0;
        for (int i=0; i<datas.size(); ++i){
            ContentValues data = datas.get(i);
            r = db.update(mTab, data, Novel.ID + " = " + data.getAsInteger(Novel.ID), null);
        }
        db.close();
        return r;
    }

    public List<ContentValues> loadData (Context c, String[] columns, String selection, String[] selectionArgs, String orderBy){
        MyOpenHelper dbh = new MyOpenHelper(c);

        List<ContentValues> datas = new ArrayList<ContentValues>();
        SQLiteDatabase db = dbh.getReadableDatabase();

        Cursor cursor = db.query(mTab, columns, selection, selectionArgs, null, null, orderBy);

        while(cursor.moveToNext()){
            ContentValues data = c2d(cursor);
            datas.add(data);
        }
        cursor.close();
        db.close();

        return datas;
    }

    public void setMark(Context c){
        String date = (String) DateFormat.format("yyyy/MM/dd", new Date());
        MyOpenHelper dbh = new MyOpenHelper(c);
        ContentValues data = new ContentValues();

        SQLiteDatabase db = dbh.getWritableDatabase();
        data.put(MyConstant.MF_TABNAME, mTab);
        data.put(MyConstant.MF_DATE, date);
        db.insert(MyConstant.MARKTAB, null, data);
        db.close();
    }

    public String getMark (Context c) {
        String date = null;
        MyOpenHelper dbh = new MyOpenHelper(c);
        SQLiteDatabase db = dbh.getWritableDatabase();

        Cursor cursor = db.query(MyConstant.MARKTAB, null, MyConstant.MF_TABNAME + " = \'"+ mTab+"\'", null, null, null, null);

        while (cursor.moveToNext()){
            date = cursor.getString(cursor.getColumnIndex(MyConstant.MF_DATE));
        }
        cursor.close();
        db.close();
        return date;
    }
}
