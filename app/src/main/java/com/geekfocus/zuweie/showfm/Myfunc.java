package com.geekfocus.zuweie.showfm;

import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.format.DateFormat;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Created by zuweie on 12/17/14.
 */
public class Myfunc {
    public static String md5(String text) throws NoSuchAlgorithmException {

        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(text.getBytes());
        byte[] sign = md5.digest();
        int i;
        StringBuffer signbuffer = new StringBuffer("");
        for(int ii=0; ii<sign.length; ++ii){
            i = sign[ii];
            if (i < 0)
                i += 256;
            if (i < 16)
                signbuffer.append("0");
            signbuffer.append(Integer.toHexString(i));
        }
        return signbuffer.toString();
    }

    public static ContentValues getDownloadConnectionHeader(int recid, int novelid){
        ContentValues header = new ContentValues();
        header.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        header.put("Accept-Language", "en-US,en;q=0.5");
        header.put("Referer", "http://www.showfm.net/novel/download.asp?id="+recid+"&xilie="+novelid);
        header.put("Charset", "UTF-8");
        header.put("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:33.0) Gecko/20100101 Firefox/33.0");
        header.put("Connection", "keep-Alive");
        header.put("Cache-Control", "max-age=0");
        return header;
    }

    public static String getValidUrl (String foler, String itemurl, int expire) throws NoSuchAlgorithmException {

        String down = "/downloads/"+foler+itemurl;
        // sign
        long etime = System.currentTimeMillis()/1000L + expire;

        String token = getValidText(MyConstant.RESOURCE_TOKEN);
        String sign  = token+"&"+etime+"&"+down;

        sign = Myfunc.md5(sign);

        sign = sign.substring(12, 20) + etime;
        return getResourceHost()+"/downloads/"+foler+ Uri.encode(itemurl)+"?_upt="+sign;
    }

    public static long diffHour(long time){
        long now = System.currentTimeMillis();
        long difftime =  now - time;
        return  difftime / 1000 / 60 / 60;
    }

    public static String getVersionName(Context context){
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pi.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            return "";
        }
    }

    public static int getVersionCode(Context context){
        try {
            PackageInfo pi=context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pi.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            return 0;
        }
    }

    public static byte[] bmpToByteArray(Bitmap bitmap){
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output);
        bitmap.recycle();
        byte[] result = output.toByteArray();
        try {
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static long diffDay(long time){
        return diffHour(time) / 24;
    }

    public static String ltime2Sdate (long time){
        String date = DateFormat.format("yyyy/MM/dd hh:mm:ss", time).toString();
        return date;
    }

    public static boolean showupYYT(){

        if(showup == 0)
            return false;
        else if (showup > total)
            return true;

        int chance = (int) (((float)showup / (float)total) * 100);

        int rc = crandom.nextInt(100);

        if (rc <= chance)
            return  true;
        else
            return false;
    }

    public static ContentValues getYytRandom() {
        if (showupYYT() && yyts.size() > 0){
            int pos = yytrandom.nextInt(yyts.size());
            return yyts.get(pos);
        }else
            return null;
    }

    public static void setShowUpChance(int s, int t){
        showup = s;
        total  = t;
    }

    public static int showup = 0;
    public static int total  = 100;
    public static List<ContentValues> yyts = new LinkedList<ContentValues>();
    public static Random crandom = new Random();
    public static Random yytrandom = new Random();
    //public static native String getValidToken ();
    public static native String getResourceHost ();
    public static native String getNovelApi ();
    public static native String getRecordApi ();
    //public static native String getAVOSAPPID ();
    //public static native String getAVOSAPPKEY ();
    //public static native String getWeiboAPPKEY ();
    public static native String getValidText (String text);

    static {
        System.loadLibrary("api");
    }

}
