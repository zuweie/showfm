package com.example.zuweie.showfm;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;

public class MainActivity extends Activity {

    /* data define */
    private PullToRefreshGridView mPullToRefreshGridView;
    private GridView mGridView;
    private List<ContentValues> mNovel_data = null;
    private final static String strNovelapi = "http://www.showfm.net/api/novel.asp";
    private MyAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Init the Data
        Novel novel = new Novel();
        mNovel_data = novel.loadData(this, null, null, null, "updated desc");

        // Init the UI
        mPullToRefreshGridView = (PullToRefreshGridView) findViewById(R.id.pull_refresh_grid);
        mGridView = mPullToRefreshGridView.getRefreshableView();
        mAdapter = new MyAdapter(this);
        mGridView.setAdapter(mAdapter);
        
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                long novelid = mAdapter.getItemId(position);
                Intent it = new Intent(MainActivity.this, PlayingListActivity.class);
                it.putExtra("nvl", novelid);
                it.putExtra("pm", MyConstant.PM_NOVEL);
                MainActivity.this.startActivity(it);
            }
        });

        mPullToRefreshGridView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<GridView>() {
            @Override
            public void onPullDownToRefresh(PullToRefreshBase<GridView> refreshView) {

            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<GridView> refreshView) {

            }
        });

        // load the novel data from db

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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

    private class GetNovelDataTask extends AsyncTask <String, Void, String> {

        @Override
        protected String doInBackground(String... urls) {
           String url = urls[0];
           return null;
        }

        protected void onPostExecute(String result){
            // TODO : 分析数据， 将其丢入数据库。
        }
    }

    private class MyAdapter extends BaseAdapter {

        private Context context;
        class Holder {
            ImageView imageView;
            TextView  textView;
        }
        public MyAdapter (Context c){
            this.context = c;
        }

        @Override
        public int getCount() {
            if (mNovel_data == null)
                return 0;
            else
                return mNovel_data.size();
        }

        @Override
        public Object getItem(int position) {
            return mNovel_data.get(position);
        }

        @Override
        public long getItemId(int position) {
            ContentValues data = (ContentValues)this.getItem(position);
            return data.getAsInteger(Novel.ID);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder;
            if (convertView == null){
                LayoutInflater inflater = MainActivity.this.getLayoutInflater();
                convertView = inflater.inflate(R.layout.novel_item, null);
                holder = new Holder();
                holder.imageView = (ImageView)convertView.findViewById(R.id.nvl_img_view);
                holder.textView  = (TextView)convertView.findViewById(R.id.nvl_text_view);
                convertView.setTag(holder);
            }else{
                holder = (Holder)convertView.getTag();
            }

            // render the item
            ContentValues data = (ContentValues)this.getItem(position);
            String name = data.getAsString(Novel.NAME);
            holder.textView.setText(name);
            holder.imageView.setImageResource(R.drawable.nvl_default);
            return convertView;
        }
    }
}
