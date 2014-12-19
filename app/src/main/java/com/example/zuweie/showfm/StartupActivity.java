package com.example.zuweie.showfm;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.List;


public class StartupActivity extends Activity {

    private final static String NVL_API = "http://www.showfm.net/api/novel.asp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        this.setTheme(R.style.StartUpActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_startup);
        new loadDataTask().execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_startup, menu);
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

    private class loadDataTask extends AsyncTask<String, Void, Integer> {
        @Override
        protected Integer doInBackground(String[] params) {
            Novel novel = new Novel();
            String date = novel.getMark(StartupActivity.this);
            String api = NVL_API + "?after="+date+"&state0=1&state1=2";
            try {
                List<ContentValues> datas = novel.getData(api);
                novel.saveData(StartupActivity.this, datas);
                novel.setMark(StartupActivity.this);
                return 0;
            } catch (IOException e) {
                Log.e(MyConstant.TAG_NOVEL_API, e.getMessage());
            } catch (JSONException e) {
                Log.e(MyConstant.TAG_NOVEL_JSON, e.getMessage());
            }

            return -1;
        }

        @Override
        protected void onPostExecute (Integer result){

            if (result == 0){
                Toast.makeText(StartupActivity.this, R.string.load_data_ok, Toast.LENGTH_SHORT);
            }else{
                Toast.makeText(StartupActivity.this, R.string.load_data_unusual, Toast.LENGTH_SHORT);
            }

            // TODO : jump to main Activity;
            Intent intent = new Intent(StartupActivity.this, MainActivity.class);
            StartupActivity.this.startActivity(intent);
            StartupActivity.this.finish();
        }
    }
}
