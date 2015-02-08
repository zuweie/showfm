package com.geekfocus.zuweie.showfm;

import android.net.Uri;
import android.text.format.DateFormat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

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

        String token = "qR9sXisWuGzk3";
        String sign  = token+"&"+etime+"&"+down;

        sign = Myfunc.md5(sign);

        sign = sign.substring(12, 20) + etime;
        return "http://dl.showfm.net/downloads/"+foler+ Uri.encode(itemurl)+"?_upt="+sign;
    }

    public static long diffHour(long time){
        long now = System.currentTimeMillis();
        long difftime =  now - time;
        return  difftime / 1000 / 60 / 60;
    }

    public static long diffDay(long time){
        return diffHour(time) / 24;
    }

    public static String ltime2Sdate (long time){
        String date = DateFormat.format("yyyy/MM/dd hh:mm:ss", time).toString();
        return date;
    }
}
