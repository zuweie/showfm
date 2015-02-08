package com.geekfocus.zuweie.showfm;

/**
 * Created by zuweie on 12/8/14.
 */
public class MyConstant {
    /* TAG */
    public final static String TAG_NOVEL_API = "novel_api";
    public final static String TAG_NOVEL_JSON = "novel_json";
    public final static String TAG_RECORD_JSON = "record_json";
    public final static String TAG_RECORD_API  = "record_api";
    public final static String TAG_PLAYBACK   = "playback";
    public final static String TAG_DOWNLOADER = "downloader";
    public final static String TAG_NOVEL = "novel";
    /*ERROR CODE*/

    /*ERROR MSG*/

    /* LOG MESSAGE */

    /*mark tab*/
    public final static String MARKTAB = "mark";
    public final static String MF_ID      = "id";
    public final static String MF_TABNAME = "tabname";
    public final static String MF_DATE    = "date";
    public final static String MF_CONTENTID = "content_id";

    /* play history tab */
    public final static String HISTORYTAB = "history";
    public final static String HISTORY_ID = "his_id";
    public final static String CONTENT_TYPE = "content_type";
    public final static String CONTENT_NAME = "content_name";
    public final static String CONTENT_ID = "content_id";
    public final static String RECORD_ID = "record_id";
    public final static String RECORD_NAME = "record_name";
    public final static String POSITION    = "position";

    /* play content type */
    public final static Integer PM_NOVEL = 1;
    public final static Integer PM_PLAY   = 2;

    /* async task status */
    public final static int TASK_STATUS_OK = 0;
    public final static int TASK_STATUS_API_ERR = -1;
    public final static int TASK_STATUS_JSON_ERR = -2;
}