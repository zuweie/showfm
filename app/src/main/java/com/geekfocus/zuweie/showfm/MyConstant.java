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
    public final static String TAG_WEIBO_AUTH = "weibo_auth";
    public final static String TAG_LOGIN = "login";
    public final static String TAG_NET = "net";
    public final static String TAG_NOTIFY = "notify";
    public final static String TAG_NETBITMAP = "net_bitmap";
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

    public final static String RESOURCE_TOKEN = "sqWRu9GszXki3";
    public final static String AVO_APP_ID = "58q2s83rqrpx61cvl291sqmqi16azeud95caqnoqfg2qyswd";
    public final static String AVO_APP_KEY = "vmh2p786zgf4jbc8e67ae694y2m14ojnj04yf0mh0qc4fi3u";
    public final static String WEIBO_APP_KEY = "8441059732";


    public final static String WX_APP_ID = "wxb9da6876f33082ae";

    /* play content type */
    public final static Integer PM_NOVEL = 1;
    public final static Integer PM_PLAY   = 2;

    /* async task status */
    public final static int TASK_STATUS_OK = 0;
    public final static int TASK_STATUS_API_ERR = -1;
    public final static int TASK_STATUS_JSON_ERR = -2;

    /* av object */
    public final static String AV_SSO_USER = "sso_user";
    public final static String SSO_NAME = "sso_name";
    public final static String SSO_UID  = "sso_uid";
    public final static String SSO_NICKNAME = "sso_nickname";
    public final static String SSO_IMG_URL = "sso_img_url";
    public final static String SSO_URl = "sso_url";
    public final static String SSO_TYPE = "sso_type";

    public final static String AVO_COMMENT_OID = "objectId";
    public final static String AVO_COMMENT_BODY = "body";
    public final static String AVO_COMMENT_NOVELID = "novelId";
    public final static String AVO_COMMENT_USER = "user";
    public final static String AVO_COMMENT_USERAVATAR = "userAvatar";
    public final static String AVO_COMMENT_USERNAME = "userName";
    public final static String AVO_COMMENT_UPDATEDAT = "updatedAt";

    public final static String AVO_USER_NAME = "name";
    public final static String AVO_USER_AVATAR = "avatar";

    /* av object */


    public final static String WEIBO_REDIRECT_URL = "https://api.weibo.com/oauth2/default.html";
    public final static String WEIBO_SCOPE = "email,direct_messages_read,direct_messages_write,"
                                           +"friendships_groups_read,friendships_groups_write,statuses_to_me_read,"
                                           +"follow_app_official_microblog,"
                                           +"invitation_write";

    public final static int LOGIN_REQ_CODE = 1;
    public final static int LOGIN_OK = 0;
    public final static int LOGIN_ERROR = -1;
    public final static int LOGIN_CANCEL = 2;

    public final static int ME_REQ_CODE = 4;
    public final static int ME_RESULT_EXIT = 3;

}
