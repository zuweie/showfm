package com.geekfocus.zuweie.showfm;

import android.app.Application;

import com.avos.avoscloud.AVOSCloud;

/**
 * Created by zuweie on 3/2/15.
 */
public class ShowfmApplication extends Application {

    @Override
    public void onCreate(){
        super.onCreate();
        AVOSCloud.initialize(this, Myfunc.getValidText(MyConstant.AVO_APP_ID), Myfunc.getValidText(MyConstant.AVO_APP_KEY));
        NetBitMap.startThreadPool();
    }

    @Override
    public void onTerminate (){
        NetBitMap.stopThreadPool();
        super.onTerminate();
    }

}
