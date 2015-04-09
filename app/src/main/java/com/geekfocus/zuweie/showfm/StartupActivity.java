package com.geekfocus.zuweie.showfm;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.umeng.analytics.MobclickAgent;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class StartupActivity extends Activity {

    private TextView mStarupTips = null;
    //private Messenger mItSelf = null;

    boolean mUpdateSSOUserOk = false;

    public static final int MSG_ON_UPDATE_LOGIN_USER = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setTheme(R.style.StartUpActionBar);
        super.onCreate(savedInstanceState);
        /*comment the facebook code, can not be use in china,Fuck!!
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        */
        setContentView(R.layout.activity_startup);
        mStarupTips = (TextView)findViewById(R.id.start_up_text);
        new loadDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_startup, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume(){
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause(){
        MobclickAgent.onPause(this);
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == MyConstant.LOGIN_REQ_CODE){
            if (resultCode == MyConstant.LOGIN_OK){
                Toast.makeText(StartupActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
            }else if(resultCode == MyConstant.LOGIN_ERROR){
                Toast.makeText(StartupActivity.this, "登录异常", Toast.LENGTH_SHORT).show();
            }else if (resultCode == MyConstant.LOGIN_CANCEL){
                Toast.makeText(StartupActivity.this, "登录取消", Toast.LENGTH_SHORT).show();
            }
            Intent it = new Intent(StartupActivity.this, MainActivity.class);
            startActivity(it);
            finish();
        }
    }

    private class loadDataTask extends AsyncTask<String, CharSequence, Integer> {
        @Override
        protected Integer doInBackground(String[] params) {

            try {
                Novel novel = new Novel();
                long time = novel.getMark(StartupActivity.this);
                long diffday = Myfunc.diffDay(time);

                if (diffday >=1){
                    Thread.sleep(1000);
                    publishProgress(getResources().getText(R.string.startup_progress_1));
                    String date = Myfunc.ltime2Sdate(time);
                    String api = Myfunc.getNovelApi()+"?after="+ Uri.encode(date)+"&state0=1&state1=2";
                    Log.v(MyConstant.TAG_NOVEL, "update api: " + api);
                    //publishProgress("api:"+api);
                    List<ContentValues> datas = novel.getData(api);
                    publishProgress(getResources().getText(R.string.startup_progress_2));
                    if (datas != null && !datas.isEmpty()) {
                        novel.saveData(StartupActivity.this, datas);
                        List<ContentValues> vs = new ArrayList<ContentValues>();
                        publishProgress(getResources().getText(R.string.startup_progress_3));
                        for(int i=0; i<datas.size(); ++i){
                            ContentValues v = new ContentValues();
                            ContentValues data = datas.get(i);
                            v.put(Novel.ID, data.getAsInteger(Novel.ID));
                            v.put(Novel.UPDATED, data.getAsLong(Novel.UPDATED));
                            vs.add(v);
                        }
                        novel.updataNovelDate(StartupActivity.this, vs);
                    }
                    novel.setMark(StartupActivity.this);
                    publishProgress(getResources().getText(R.string.startup_progress_4));
                    //Thread.sleep(2000);
                }else{
                    publishProgress(getResources().getText(R.string.startup_progress_4));
                    Thread.sleep(5000);
                }

                return 0;
            } catch (IOException e) {
                Log.e(MyConstant.TAG_NOVEL_API, e.getMessage());
            } catch (JSONException e) {
                Log.e(MyConstant.TAG_NOVEL_JSON, e.getMessage());
            } catch (InterruptedException e) {
                Log.e(MyConstant.TAG_NOVEL, e.getMessage());
            }
            return -1;
        }

        @Override
        protected void onProgressUpdate(CharSequence... params){
            mStarupTips.setText(params[0]);
        }

        @Override
        protected void onPostExecute (Integer result){

            if (result == 0){
                Toast.makeText(StartupActivity.this, R.string.load_data_ok, Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(StartupActivity.this, R.string.load_data_unusual, Toast.LENGTH_SHORT).show();
            }

            mStarupTips.setText(R.string.load_user_info);

            // 1 read last login info
            MyLogin.getInstance().setContext(StartupActivity.this);
            MyLogin.getInstance().readLoginStatus();
            if (MyLogin.getInstance().getLoginStatus() != MyLogin.STA_LOGOUT
               && MyLogin.getInstance().isSSOTokenvalid()){
                MyLogin.getInstance().fetchAVOSUser();
                MyLogin.getInstance().fetchSSoUser();
                Intent it = new Intent(StartupActivity.this, MainActivity.class);
                startActivity(it);
                finish();
            }else {
                Intent intent = new Intent(StartupActivity.this, LoginActivity2.class);
                startActivityForResult(intent, MyConstant.LOGIN_REQ_CODE);
            }
        }
    }

}
