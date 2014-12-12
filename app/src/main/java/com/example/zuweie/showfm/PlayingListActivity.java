package com.example.zuweie.showfm;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.handmark.pulltorefresh.library.PullToRefreshListView;

import org.json.JSONException;

import java.io.IOException;
import java.util.List;
import java.util.zip.Inflater;


public class PlayingListActivity extends Activity {

    /* api */
    private final static String RED_API = "http://www.showfm.net/api/record.asp";

    /* data define */
    private List<ContentValues> mPlaying_data;
    private Record mRecord = new Record();
    private Integer mPlayMode;
    private long mNovelId;
    private PullToRefreshListView mPullToRefreshListView;
    private ListView mPlayinglistView;
    private MyAdapter mMyadapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playing_list);

        mPlayMode = this.getIntent().getIntExtra("pm", -1);

        if (mPlayMode == MyConstant.PM_NOVEL)
            mNovelId = this.getIntent().getLongExtra("nvl", -1);


        /* init the UI */
        mPullToRefreshListView = (PullToRefreshListView)this.findViewById(R.id.playing_list);
        mPlayinglistView = mPullToRefreshListView.getRefreshableView();
        mMyadapter = new MyAdapter();
        mPlayinglistView.setAdapter(mMyadapter);


        /* load the data from net */
        if (mPlayMode == MyConstant.PM_NOVEL){
            String date = mRecord.getMark(PlayingListActivity.this);
            String api = RED_API + "?after="+date+"&perpage=10000&novel_id="+mNovelId;
            new GetDataTask().execute(api);
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_playing_list, menu);
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

    class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            if (mPlaying_data == null)
                return 0;
            else
                return mPlaying_data.size();
        }

        @Override
        public Object getItem(int position) {
            return mPlaying_data.get(position);
        }

        @Override
        public long getItemId(int position) {
            if (mPlayMode == MyConstant.PM_NOVEL)
                return mPlaying_data.get(position).getAsInteger(Record.ID);
            else
                return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;
            if (convertView == null){
                LayoutInflater inflater = PlayingListActivity.this.getLayoutInflater();
                convertView = inflater.inflate(R.layout.playlist_item, null);
                holder = new Holder();
                holder.image = (ImageView)convertView.findViewById(R.id.playing_item_img);
                holder.text  = (TextView)convertView.findViewById(R.id.playing_item_title);
                holder.progressBar = (ProgressBar) convertView.findViewById(R.id.playing_item_progress);
                convertView.setTag(holder);
            }else{
                holder = (Holder)convertView.getTag();
            }

            ContentValues data = (ContentValues)this.getItem(position);

            if (mPlayMode == MyConstant.PM_NOVEL)
                holder.text.setText(data.getAsString(Record.NAME));

            return convertView;
        }

        class Holder {
            ImageView image;
            TextView  text;
            ProgressBar progressBar;
        }
    }

    class GetDataTask extends AsyncTask<String, Void, Integer>{

        @Override
        protected void onPreExecute(){
            Toast.makeText(PlayingListActivity.this, R.string.load_data_start, Toast.LENGTH_LONG);
        }

        @Override
        protected Integer doInBackground(String... params) {

            String api = params[0];

            try {
                mPlaying_data = mRecord.getData(api);
            } catch (IOException e) {
                Log.e(MyConstant.TAG_RECORD_API, e.getMessage());
                return MyConstant.TASK_STATUS_API_ERR;
            } catch (JSONException e) {
                Log.e(MyConstant.TAG_RECORD_JSON, e.getMessage());
                return MyConstant.TASK_STATUS_JSON_ERR;
            }
            return MyConstant.TASK_STATUS_OK;
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result){
                case MyConstant.TASK_STATUS_OK:
                    Toast.makeText(PlayingListActivity.this, R.string.load_data_ok, Toast.LENGTH_SHORT);
                    break;
                case MyConstant.TASK_STATUS_API_ERR:
                    Toast.makeText(PlayingListActivity.this, R.string.load_data_status_api_err, Toast.LENGTH_SHORT);
                    break;
                case MyConstant.TASK_STATUS_JSON_ERR:
                    Toast.makeText(PlayingListActivity.this, R.string.load_data_status_json_err, Toast.LENGTH_SHORT);
                    break;
            }
            mMyadapter.notifyDataSetChanged();
        }
    }
}
