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

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class StartupActivity extends Activity {

    //private final static String NVL_API = "http://www.showfm.net/api/novel.asp";

    private TextView mStarupTips = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setTheme(R.style.StartUpActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        mStarupTips = (TextView)findViewById(R.id.start_up_text);
        Novel novel = new Novel();
        //String sdate = novel.getMark(StartupActivity.this);
        new loadDataTask().execute();
        super.onCreate(savedInstanceState);
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
                    String api = "http://www.showfm.net/api/novel.asp?after="+ Uri.encode(date)+"&state0=1&state1=2";
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
                    Thread.sleep(2000);
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

            // TODO : jump to main Activity;
            Intent intent = new Intent(StartupActivity.this, MainActivity.class);
            StartupActivity.this.startActivity(intent);
            StartupActivity.this.finish();
        }
    }
}
