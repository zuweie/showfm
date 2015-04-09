package com.geekfocus.zuweie.showfm;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by zuweie on 3/13/15.
 */
public class NetBitMap implements GeneralNotifyer.GeneralListener{
    String mUrl;
    String mBmpfileName;
    Context mContext;
    Integer status = 0;
    GeneralNotifyer notifyer = null;


    //boolean mCompress = false;
    int mCompressSize = 0;
    int mStoreType = 0;
    int mDefaultType = 0;

    boolean mLocalBmp = false;

    private int mDf_Resid = -1;
    private String mDf_file;

    LoadBitmapCallback mcallback;
    public static final int STA_IDLE = 0;
    public static final int STA_LOADING = 1;

    public static final int STY_INTERNAL_CACHE = 0;
    public static final int STY_INTERNAL = 1;
    public static final int STY_EXTERNAL_CACHE = 2;
    public static final int STY_EXTERNAL = 3;



    private static final int DFT_RES = 1;
    private static final int DFT_FILE = 2;

    public static ExecutorService mPool = null;

    public static void startThreadPool () {
        mPool = Executors.newFixedThreadPool(5);
    }

    public static void stopThreadPool() {
        if (!mPool.isShutdown())
            mPool.shutdown();
    }

    public NetBitMap(Context context,String url, int defaultbm){
        this.mUrl = url;
        this.mContext = context;
        this.mDf_Resid = defaultbm;
        this.mDefaultType = DFT_RES;
        try {
            this.mBmpfileName = mContext.getCacheDir()+"/"+Myfunc.md5(url) + ".jpg";
        } catch (NoSuchAlgorithmException e) {}
    }

    public NetBitMap(Context context, String url, int defaultbm, int compresssz ){
        this.mUrl = url;
        this.mContext = context;
        this.mDf_Resid = defaultbm;
        this.mDefaultType = DFT_RES;
        this.mCompressSize = compresssz;
        try {
            this.mBmpfileName = mContext.getCacheDir()+"/"+Myfunc.md5(url) + ".jpg";
        } catch (NoSuchAlgorithmException e) {}
    }

    @Override
    public void onSomethingDone(Object result) {
        if (mcallback != null) {
            mcallback.done((Bitmap) result, null);
            mcallback = null;
        }
    }

    @Override
    public void onSomethingError(Object error) {
        if (mcallback != null){
            mcallback.done(null, error);
            mcallback = null;
        }
    }

    public Bitmap getDefaultBitmap() {
        if (mDefaultType == DFT_RES){
            return BitmapFactory.decodeResource(mContext.getResources(), mDf_Resid);
        }else if (mDefaultType == DFT_FILE){
            return BitmapFactory.decodeFile(mDf_file);
        }
        return null;
    }

    synchronized public Bitmap getBitmap(LoadBitmapCallback callback){
        Bitmap bmp;
        if ((bmp = loadCacheBitmap()) != null){
            return bmp;
        }else{
                // Start load bm
            if (mUrl != null && !mUrl.isEmpty() && getStatus() ==  NetBitMap.STA_IDLE) {
                if (notifyer == null) {
                    notifyer = new GeneralNotifyer();
                    notifyer.setGeneralListener(this);
                }
                mcallback = callback;
                mPool.execute(new Loadbm());
            }
            return getDefaultBitmap();
        }
    }

    public Bitmap loadCacheBitmap(){
        if (getStatus() == STA_IDLE){
            Bitmap bmp  = BitmapFactory.decodeFile(mBmpfileName);
            return  bmp;
        }
        return null;
    }

    private synchronized int getStatus () {
        return status;
    }

    private synchronized void setStatus (int status) {
        this.status = status;
    }

    class Loadbm implements Runnable {

        @Override
        public void run() {

            setStatus(NetBitMap.STA_LOADING);

            try {
                Log.v(MyConstant.TAG_NOVEL, "start a thread to load bitmap!");
                URL url  = new URL(mUrl);
                HttpURLConnection connection = null;
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5 * 1000);
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
                connection.setRequestProperty("Accept-Language", "zh-CN");
                connection.setRequestProperty("Charset", "UTF-8");
                connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
                connection.setRequestProperty("Connection", "Keep-Alive");
                Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
                connection.disconnect();

                if (bitmap != null){
                    FileOutputStream fos = null;

                    switch (mStoreType){

                        case STY_EXTERNAL_CACHE:
                        default:
                            File bmCache = new File(mContext.getCacheDir() , Myfunc.md5(mUrl)+".jpg");
                            fos = new FileOutputStream(bmCache);
                            break;
                    }

                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    if (mCompressSize > 0){
                        int q = 90;
                        // compress bitmap, make it smaller then 8k.
                        do{
                            bos.reset();

                            if (q >= 1)
                                bitmap.compress(Bitmap.CompressFormat.JPEG, q, bos);

                            if (q <=30)
                                q -= 5;
                            else if (q<=10)
                                q -= 1;
                            else
                                q -= 30;
                        }while(bos.size() > mCompressSize && q >1);
                    }else{
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                    }
                    Log.v(MyConstant.TAG_NOVEL, "compress bitmap file sz : " + (bos.size() /1024)+ "k and save it in "+Myfunc.md5(mUrl)+" file");
                    fos.write(bos.toByteArray());
                    fos.close();
                    //bitmap.recycle();
                }
                Log.v(MyConstant.TAG_NOVEL, "finished load bitmap !");
                notifyer.NotifySomethingDone(bitmap);
            } catch (IOException e) {
                Log.e(MyConstant.TAG_NOTIFY, e.getMessage());
                notifyer.NotifySomethingError(e.getMessage());
            } catch (NoSuchAlgorithmException e) {
                Log.e(MyConstant.TAG_NOTIFY, e.getMessage());
                notifyer.NotifySomethingError(e.getMessage());
            }
            setStatus(NetBitMap.STA_IDLE);
        }
    }

    public static interface LoadBitmapCallback  {
        public void done(Bitmap bm, Object error);
    }
}
