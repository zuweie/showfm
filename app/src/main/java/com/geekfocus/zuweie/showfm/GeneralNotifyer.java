package com.geekfocus.zuweie.showfm;

import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 * Created by zuweie on 3/14/15.
 */
public class GeneralNotifyer {

    public GeneralNotifyer(){
        mMessenger = new Messenger(new GeneralHandler());
    }

    public void setGeneralListener(GeneralListener gl){
        this.ml = gl;
    }

    public void NotifySomethingDone (Object result) {
        try {
            Message msg =Message.obtain(null, GeneralNotifyer.MSG_SOMETHING_DONE);
            msg.obj = result;
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.e(MyConstant.TAG_NOTIFY, e.getMessage());
        }
    }

    public void NotifySomethingError(Object error) {
        try{
            Message msg = Message.obtain(null, GeneralNotifyer.MSG_SOMETHING_ERROR);
            msg.obj = error;
            mMessenger.send(msg);
        }catch (RemoteException e){
            Log.e(MyConstant.TAG_NOTIFY, e.getMessage());
        }
    }

    private Messenger mMessenger = null;
    private GeneralListener ml;
    public final static int MSG_SOMETHING_DONE = 1;
    public final static int MSG_SOMETHING_ERROR = 2;

    static interface GeneralListener {
        public void onSomethingDone(Object result);
        public void onSomethingError(Object error);
    }

    class GeneralHandler extends Handler {
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case GeneralNotifyer.MSG_SOMETHING_DONE:
                    if (ml != null)
                        ml.onSomethingDone(msg.obj);
                    break;
                case GeneralNotifyer.MSG_SOMETHING_ERROR:
                    if (ml != null)
                        ml.onSomethingError(msg.obj);
                    break;
                default:
                    handleMessage(msg);
            }
        }
    }
}
