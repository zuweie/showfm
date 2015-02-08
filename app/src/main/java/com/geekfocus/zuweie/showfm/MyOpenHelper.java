package com.geekfocus.zuweie.showfm;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by zuweie on 12/8/14.
 */
public class MyOpenHelper extends SQLiteOpenHelper{

    private final static int DATABASE_VERSION = 2;
    private final static String DATABASE= "sf";

    /* SQL TO CREATE THE NOVEL TABLE */
    private final static String SQL_CREATE_NOVEL = " CREATE TABLE " + Novel.TAB + " ( "
                                                 + Novel.ID + " INTEGER PRIMARY KEY, "
                                                 + Novel.NJNAME + " VARCHAR(255) DEFAULT NULL, "
                                                 + Novel.NJID + " VARCHAR(255) DEFAULT NULL, "
                                                 + Novel.NAME + " VARCHAR(255) DEFAULT NULL, "
                                                 + Novel.NJAVATAR + " VARCHAR(1024) DEFAULT NULL,"
                                                 + Novel.URL  + " VARCHAR(1024) DEFAULT NULL, "
                                                 + Novel.AUTHOR + " VARCHAR(255) DEFAULT NULL, "
                                                 + Novel.BODY + " TEXT DEFAULT NULL, "
                                                 + Novel.POSTER + " VARCHAR(1024) DEFAULT NULL, "
                                                 + Novel.KEYWORD + " VARCHAR(255) DEFAULT NULL, "
                                                 + Novel.CATEGORY + " VARCHAR(255) DEFAULT NULL, "
                                                 + Novel.UPDATED + " UNSIGNED BIG INT DEFAULT 0, "
                                                 + Novel.STATUS + " TINYINT DEFAULT 0, "
                                                 + Novel.FAVOUR + " TINYINT DEFAULT 0 "
                                                 + " ); ";

    /* SQL to Create the novel update time */
    private final static String SQL_CREATE_MARK = " CREATE TABLE " +MyConstant.MARKTAB+ " ( "
                                                + MyConstant.MF_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                                                + MyConstant.MF_TABNAME + " VARCHAR(255), "
                                                + MyConstant.MF_CONTENTID + " INTEGER DEFAULT 0, "
                                                + MyConstant.MF_DATE + " UNSIGNED BIG INT DEFAULT 0 );";

    private final static String SQL_CREATE_RECORD = " CREATE TABLE " + Record.TAB + " ( "
                                                  + Record.ID + " INTEGER PRIMARY KEY, "
                                                  + Record.NJID + " VARCHAR(255) DEFAULT NULL, "
                                                  + Record.NJNAME + " VARCHAR(255) DEFAULT NULL, "
                                                  + Record.NAME + " VARCHAR(255) DEFAULT NULL, "
                                                  + Record.URL + " TEXT DEFAULT NULL, "
                                                  + Record.NOVELID + " INTEGER, "
                                                  + Record.DOWNLOADID + " INTEGER DEFAULT -1, "
                                                  + Record.UPDATED + " UNSIGNED BIG INT DEFAULT 0, "
                                                  + Record.READ + " INTEGER DEFAULT 0 "
                                                  + " ); ";

    private final static String SQL_CREATE_DOWNLOAD = " CREATE TABLE " + Downloader.TAB + " ( "
                                                    + Downloader.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                                                    + Downloader.FILENAME + " VARCHAR(255) DEFAULT NULL, "
                                                    + Downloader.PATH + " VARCHAR(255) DEFAULT NULL, "
                                                    + Downloader.URL + " TEXT DEFAULT NULL, "
                                                    + Downloader.STATUS + " INTEGER DEFAULT 0, "
                                                    + Downloader.ERR + " TEXT DEFAULT NULL, "
                                                    + Downloader.FILESZ + " INTEGER DEFAULT 0, "
                                                    + Downloader.LASTPOS + " INTEGER DEFAULT 0 "
                                                    + " ); ";

    private final static String SQL_CREATE_PLAY_HISTORY = " CREATE TABLE " + MyConstant.HISTORYTAB + " ( "
                                                        + MyConstant.HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                                                        + MyConstant.CONTENT_TYPE + " TINYINT DEFAULT 1, "
                                                        + MyConstant.CONTENT_ID + " INTEGER DEFAULT 0, "
                                                        + MyConstant.CONTENT_NAME + " VARCHAR(255) DEFAULT NULL, "
                                                        + MyConstant.RECORD_ID + " INTEGER DEFAULT 0, "
                                                        + MyConstant.RECORD_NAME + " INTEGER DEFAULT 0, "
                                                        + MyConstant.POSITION + " UNSIGNED BIG INT DEFAULT 0 "
                                                        + " ) ";

    MyOpenHelper(Context c){
        super(c, DATABASE, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        db.execSQL(SQL_CREATE_NOVEL);
        db.execSQL(SQL_CREATE_RECORD);
        db.execSQL(SQL_CREATE_MARK);
        db.execSQL(SQL_CREATE_DOWNLOAD);
        db.execSQL(SQL_CREATE_PLAY_HISTORY);

        /* INSERT mark tab time */
        ContentValues data = new ContentValues();
        data.put(MyConstant.MF_TABNAME, Novel.TAB);
        long rowid = db.insert(MyConstant.MARKTAB, null, data);

        data.clear();
        data.put(MyConstant.MF_TABNAME, Record.TAB);
        rowid = db.insert(MyConstant.MARKTAB, null, data);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {}
}
