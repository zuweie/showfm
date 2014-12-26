package com.example.zuweie.showfm;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
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

        // Init the Ui data
        String coverfolder = this.getFilesDir().getAbsolutePath();
        List<ContentValues> loadCoverParams = new ArrayList<ContentValues>();


        for(int i=0; i<mNovel_data.size(); ++i){
            ContentValues data = mNovel_data.get(i);
            data.put("item_pos", i);
            String coverfile = coverfolder+"/"+data.getAsString(Novel.ID) + ".jpg";
            File file = new File(coverfile);
            if (file.exists()){
                data.put("cover_exists", true);
                //data.put("cover_uri", file.toURI().toString());
            }else{
                data.put("cover_exists", false);

                ContentValues param = new ContentValues();
                param.put("pic_url", data.getAsString(Novel.POSTER));
                param.put("pic_name", data.getAsString(Novel.ID)+".jpg");
                param.put("item_id", data.getAsInteger(Novel.ID));
                param.put("item_pos", data.getAsInteger("item_pos"));
                loadCoverParams.add(param);
            }

        }

        // if some cover need to load then create the load task to load it
        if (loadCoverParams != null && !loadCoverParams.isEmpty()){
            new GetNovelCoverTask().execute(loadCoverParams);
        }
        // fuck

        /* Start the PlaybackService*/
        Intent it = new Intent(MainActivity.this, PlayBackService.class);
        startService(it);
        /* Start the PlaybackService*/

        // Init the UI
        mPullToRefreshGridView = (PullToRefreshGridView) findViewById(R.id.pull_refresh_grid);
        mGridView = mPullToRefreshGridView.getRefreshableView();
        mAdapter = new MyAdapter();
        mGridView.setAdapter(mAdapter);
        
        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                ContentValues data = (ContentValues)mAdapter.getItem(position);
                Intent it = new Intent(MainActivity.this, PlayingListActivity.class);
                it.putExtra("nvl", data.getAsInteger(Novel.ID));
                it.putExtra("pm", MyConstant.PM_NOVEL);
                it.putExtra("nvlf", data.getAsString(Novel.URL));
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

    }

    @Override
    protected void onDestroy() {

        /* stop the PlayBackService */
        Intent it = new Intent(MainActivity.this, PlayBackService.class);
        stopService(it);
        /* stop the PlayBackService */
        super.onDestroy();
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

    private int getValiPos(int itemid, int itempos){
        if (mNovel_data != null && !mNovel_data.isEmpty() && itemid >=0 && itempos >=0){
            int id = mNovel_data.get(itempos).getAsInteger(Novel.ID);
            if(id == itemid)
                return  itempos;
            else{
                for(int i=0; i<mNovel_data.size(); i++){
                    ContentValues data = mNovel_data.get(i);
                    if (data.getAsInteger(Novel.ID) == itemid){
                        return i;
                    }
                }
            }
        }
        return -1;
    }
    private class GetNovelCoverTask extends AsyncTask <List<ContentValues>, Void, Integer> {

        @Override
        protected Integer doInBackground(List<ContentValues>... Params) {

            List<ContentValues> params = Params[0];
            BufferedInputStream in = null;
            BufferedOutputStream out = null;
            FileOutputStream fos = null;
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Integer ret = 0;

            byte[] buffer =  new byte[1024 * 100];
            for(int i=0; i<params.size(); ++i){
                try {

                    ContentValues data = params.get(i);
                    bos.reset();
                    String pic_url = data.getAsString("pic_url");
                    URL url = new URL(pic_url);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(5 * 1000);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "image/gif, image/jpeg, image/pjpeg, image/pjpeg, application/x-shockwave-flash, application/xaml+xml, application/vnd.ms-xpsdocument, application/x-ms-xbap, application/x-ms-application, application/vnd.ms-excel, application/vnd.ms-powerpoint, application/msword, */*");
                    connection.setRequestProperty("Accept-Language", "zh-CN");
                    connection.setRequestProperty("Charset", "UTF-8");
                    connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 5.2; Trident/4.0; .NET CLR 1.1.4322; .NET CLR 2.0.50727; .NET CLR 3.0.04506.30; .NET CLR 3.0.4506.2152; .NET CLR 3.5.30729)");
                    connection.setRequestProperty("Connection", "Keep-Alive");


                    /*
                    in = new BufferedInputStream(connection.getInputStream());
                    out = new BufferedOutputStream(bos);

                    // load it from net work

                    int readsz = 0;
                    while((readsz = in.read(buffer))>0){
                        out.write(buffer,0, readsz);
                    }

                    // check if need to compress the bitmap
                    byte[] bmdata = bos.toByteArray();
                     */

                    //Bitmap bitmap = BitmapFactory.decodeByteArray(bmdata, 0, bmdata.length);
                    Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());

                    int bmsz = 1024 * 8; // 8k
                    boolean loadCoverok = false;
                    if (bitmap != null && bitmap.getByteCount() > bmsz ){
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
                        }while(bos.size() > bmsz && q >1);

                        Log.v(MyConstant.TAG_NOVEL, "compress bitmap file sz : " + (bos.size() /1024)+ "k and save it in file : "+ data.getAsString("pic_name"));
                        fos = openFileOutput(data.getAsString("pic_name"), Context.MODE_PRIVATE);
                        fos.write(bos.toByteArray());
                        loadCoverok = true;
                    }else if (bitmap != null){
                        bos.reset();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos);
                        Log.v(MyConstant.TAG_NOVEL, "save bitmap file sz : "+ (bos.size()) +"k in file: "+data.getAsString("pic_name"));
                        fos = openFileOutput(data.getAsString("pic_name"), Context.MODE_PRIVATE);
                        fos.write(bos.toByteArray());
                        loadCoverok = true;
                    }else{
                        Log.e(MyConstant.TAG_NOVEL, "bitmap is null, file loading Url is "+data.getAsString("pic_url"));
                    }

                    // TODO : update mNovel_data ui data;
                    if (loadCoverok){

                        int item_id = data.getAsInteger("item_id");
                        int item_pos = data.getAsInteger("item_pos");
                        int valipos = getValiPos(item_id, item_pos);
                        if (valipos >=0 ){
                            ContentValues itemdata = mNovel_data.get(valipos);
                            // put the update data
                            itemdata.put("cover_exists", true);
                        }
                        ret += 1;
                    }

                    if (in!= null)
                        in.close();
                    if (out!= null)
                        out.close();
                    if (fos != null)
                        fos.close();
                }  catch (IOException e) {

                        try {
                            if (in!= null)
                                in.close();
                            if (out!= null)
                                out.close();
                            if (fos != null)
                                fos.close();
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }

                }
            }
           return ret;
        }

        @Override
        protected void onPostExecute(Integer result){
            // TODO : 分析数据， 将其丢入数据库。
            if (result > 0 && mAdapter != null)
                mAdapter.notifyDataSetChanged();
        }
    }

    private class MyAdapter extends BaseAdapter {

        private Context context;
        class Holder {
            ImageView new_image;
            ImageView imageView;
            TextView  textView;
            TextView  nvl_nj;
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
                holder.new_image = (ImageView)convertView.findViewById(R.id.nvl_new_img);
                holder.imageView = (ImageView)convertView.findViewById(R.id.nvl_img_view);
                holder.textView  = (TextView)convertView.findViewById(R.id.nvl_text_view);
                holder.nvl_nj    = (TextView)convertView.findViewById(R.id.nvl_nj);
                convertView.setTag(holder);
            }else{
                holder = (Holder)convertView.getTag();
            }

            // render the item
            ContentValues data = (ContentValues)this.getItem(position);
            String name = data.getAsInteger(Novel.ID) + "." +data.getAsString(Novel.NAME);
            holder.textView.setText(name);
            holder.nvl_nj.setText(data.getAsString(Novel.NJNAME));
            //holder.imageView.setImageResource(R.drawable.nvl_default);
            if (data.getAsBoolean("cover_exists")){
                if (data.get("cover_uri") == null){
                    String coverfile = getFilesDir().getAbsolutePath()+"/"+data.getAsString(Novel.ID) + ".jpg";
                    File file = new File(coverfile);
                    if (file.exists()){
                        String uri = file.toURI().toString();
                        holder.imageView.setImageURI(Uri.parse(uri));
                        data.put("cover_uri", uri);
                    }else{
                        data.put("cover_exists", false);
                    }
                }else{
                    holder.imageView.setImageURI(Uri.parse(data.getAsString("cover_uri")));
                }
            }else{
                holder.imageView.setImageResource(R.drawable.nvl_def_bg);
            }
            return convertView;
        }
    }
}
