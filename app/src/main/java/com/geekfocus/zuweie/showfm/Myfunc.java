package com.geekfocus.zuweie.showfm;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.text.format.DateFormat;

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
