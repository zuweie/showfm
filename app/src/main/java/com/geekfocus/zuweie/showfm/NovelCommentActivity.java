package com.geekfocus.zuweie.showfm;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.avos.avoscloud.AVException;
import com.avos.avoscloud.AVObject;
import com.avos.avoscloud.AVQuery;
import com.avos.avoscloud.FindCallback;
import com.avos.avoscloud.SaveCallback;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


public class NovelCommentActivity extends Activity{

    /* ui */
    private PullToRefreshListView mPullToRefreshListView;
    //private ListView mListView;
    private MyAdapter mMyadapter;
    CommentHeaderHolder mHolder;
    /* ui */

    /* data */
    Integer mNovelId;
    List<NovelComment> mComments = new LinkedList<NovelComment>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_novel_comment);

        //MyLogin.getInstance().getSSOUser().setLoadAvatarListener(this);
        mNovelId = this.getIntent().getIntExtra("nvl", -1);
        /* init ui */
        this.getActionBar().setHomeButtonEnabled(true);
        this.getActionBar().setIcon(R.drawable.activity_back);

        mPullToRefreshListView = (PullToRefreshListView) findViewById(R.id.comment_list);
        mPullToRefreshListView.setOnRefreshListener(new PullToRefreshBase.OnRefreshListener2<ListView>() {
            @Override
            public void onPullDownToRefresh(PullToRefreshBase<ListView> refreshView) {
                loadComment();
            }

            @Override
            public void onPullUpToRefresh(PullToRefreshBase<ListView> refreshView) {

            }
        });

        ListView listView = mPullToRefreshListView.getRefreshableView();

        View header = this.getLayoutInflater().inflate(R.layout.commit_box, null);
        mHolder = new CommentHeaderHolder();
        mHolder.avatar = (ImageView) header.findViewById(R.id.avatar_im);
        mHolder.contentbox = (EditText)header.findViewById(R.id.content_box_tx);
        mHolder.commit = (Button)header.findViewById(R.id.commit_bt);

        mHolder.commit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 检查是否是登录的.
                if (MyLogin.getInstance().isAVOSLogin() && MyLogin.getInstance().isSSoLogin()){
                    AVObject comment = new AVObject("Comment");
                    comment.put("body", mHolder.contentbox.getText().toString());
                    comment.put("novelId", mNovelId);
                    comment.put("user", MyLogin.getInstance().getAVOSUser());
                    comment.put("userAvatar", MyLogin.getInstance().getSSOUser().getAvatarUrl());
                    comment.put("userName", MyLogin.getInstance().getSSOUser().getNickName());
                    comment.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(AVException e) {
                            loadComment();
                            Toast.makeText(NovelCommentActivity.this, R.string.comment_post_success, Toast.LENGTH_LONG).show();
                        }
                    });
                    mHolder.contentbox.setText("");
                }else{
                    // show the dialog to login
                    AlertDialog.Builder builder = new AlertDialog.Builder(NovelCommentActivity.this);
                    builder.setTitle(R.string.invalid_login).setMessage(R.string.do_login);
                    builder.setNegativeButton(R.string.negative, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
                    builder.setPositiveButton(R.string.positive, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // TODO : login
                            Intent it = new Intent(NovelCommentActivity.this, LoginActivity2.class);
                            startActivityForResult(it, MyConstant.LOGIN_REQ_CODE);
                            dialog.dismiss();
                        }
                    });
                    builder.create().show();
                }
            }
        });

        listView.addHeaderView(header);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Adapter adapter = parent.getAdapter();
                NovelComment comment = (NovelComment) adapter.getItem(position);
                if (comment != null){
                    String format = getResources().getString(R.string.replay_comment_format);
                    String content = String.format(format, comment.userName);
                    mHolder.contentbox.setText(content);
                    parent.setSelection(0);
                }
            }
        });
        mMyadapter = new MyAdapter();

        listView.setAdapter(mMyadapter);

        loadComment();

        /* init ui */

        if (MyLogin.getInstance().isSSoLogin()){

            Bitmap bm = MyLogin.getInstance().getSSOUser().getAvatar().getBitmap(new NetBitMap.LoadBitmapCallback() {
                @Override
                public void done(Bitmap bm, Object error) {
                    if (error == null && bm != null)
                        mHolder.avatar.setImageBitmap(bm);
                }
            });

            mHolder.avatar.setImageBitmap(bm);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MyConstant.LOGIN_REQ_CODE){
            if (resultCode == MyConstant.LOGIN_OK){
                Bitmap bmp = MyLogin.getInstance().getSSOUser().getAvatar().getBitmap(new NetBitMap.LoadBitmapCallback() {
                    @Override
                    public void done(Bitmap bm, Object error) {
                        if (bm != null) {
                            mHolder.avatar.setImageBitmap(bm);
                        }
                    }
                });
                mHolder.avatar.setImageBitmap(bmp);
            }
        }
    }

    public void loadComment (){
        // load the comment
        AVQuery<AVObject> query = new AVQuery<AVObject>("Comment");
        query.whereEqualTo(MyConstant.AVO_COMMENT_NOVELID, mNovelId);
        query.orderByDescending(MyConstant.AVO_COMMENT_UPDATEDAT);
        query.findInBackground(new FindCallback<AVObject>() {
            @Override
            public void done(List<AVObject> avObjects, AVException e) {
                if (e == null){
                    mComments.clear();
                    SimpleDateFormat df = new SimpleDateFormat("yy/MM/dd HH:mm");
                    for (int i=0; i<avObjects.size(); ++i) {
                        AVObject avo = avObjects.get(i);
                        NovelComment comment = new NovelComment();
                        comment.avoId = avo.getObjectId();
                        comment.novelId = avo.getInt(MyConstant.AVO_COMMENT_NOVELID);

                        if (avo.has(MyConstant.AVO_COMMENT_USERNAME)){
                            comment.userName = avo.getString(MyConstant.AVO_COMMENT_USERNAME);
                        }

                        Date commentDate = avo.getUpdatedAt();
                        comment.updatedAt = df.format(commentDate);
                        if (avo.has(MyConstant.AVO_COMMENT_BODY)) {
                            comment.body = avo.getString(MyConstant.AVO_COMMENT_BODY);
                        }

                        if (avo.has(MyConstant.AVO_COMMENT_USERAVATAR)){
                            comment.avatarbm = new NetBitMap(NovelCommentActivity.this, avo.getString(MyConstant.AVO_COMMENT_USERAVATAR), R.drawable.no_avatar);
                        }

                        mComments.add(comment);
                    }
                    mMyadapter.notifyDataSetChanged();
                }
                if (mPullToRefreshListView.isRefreshing())
                    mPullToRefreshListView.onRefreshComplete();
            }
        });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_novel_comment, menu);
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
        }else if (id == android.R.id.home){
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }

    class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mComments.size() ;
        }

        @Override
        public Object getItem(int position) {
            return mComments.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Holder holder = null;
            if (convertView == null){
                convertView = getLayoutInflater().inflate(R.layout.comment_item, null);
                holder = new Holder();
                holder.avatar = (ImageView) convertView.findViewById(R.id.avatar_img);
                holder.nickname = (TextView) convertView.findViewById(R.id.nick_name_tx);
                holder.posttime = (TextView) convertView.findViewById(R.id.comment_time_tx);
                holder.content = (TextView) convertView.findViewById(R.id.content_tx);
                convertView.setTag(holder);
            }else {
                holder = (Holder) convertView.getTag();
            }

            // render
            NovelComment comment = (NovelComment) this.getItem(position);
            holder.nickname.setText(comment.userName);
            holder.posttime.setText(comment.updatedAt);
            holder.content.setText(comment.body);

            if (comment.avatarbm != null){
                holder.avatar.setImageBitmap(comment.avatarbm.getBitmap(null));
            }else
                holder.avatar.setImageResource(R.drawable.no_avatar);

            return convertView;
        }

        class Holder {
            ImageView avatar;
            TextView nickname;
            TextView posttime;
            TextView content;
        }
    }

    class CommentHeaderHolder {
        ImageView avatar;
        EditText contentbox;
        Button commit;
    }

    public static class NovelComment {
        public int novelId;
        public String avoId;
        public String user;
        public String userName;
        public String body;
        public String updatedAt;
        public NetBitMap avatarbm;
    }

    public static class AdFragment extends Fragment {
        private AdView mAdView;

        public AdFragment() {
        }

        @Override
        public void onActivityCreated(Bundle bundle) {
            super.onActivityCreated(bundle);

            // Gets the ad view defined in layout/ad_fragment.xml with ad unit ID set in
            // values/strings.xml.
            mAdView = (AdView) getView().findViewById(R.id.adView);

            // Create an ad request. Check logcat output for the hashed device ID to
            // get test ads on a physical device. e.g.
            // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
            AdRequest adRequest = new AdRequest.Builder()
                    .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                    .build();

            // Start loading the ad in the background.
            mAdView.loadAd(adRequest);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(R.layout.fragment_ad, container, false);
        }

        /** Called when leaving the activity */
        @Override
        public void onPause() {
            if (mAdView != null) {
                mAdView.pause();
            }
            super.onPause();
        }

        /** Called when returning to the activity */
        @Override
        public void onResume() {
            super.onResume();
            if (mAdView != null) {
                mAdView.resume();
            }
        }

        /** Called before the activity is destroyed */
        @Override
        public void onDestroy() {
            if (mAdView != null) {
                mAdView.destroy();
            }
            super.onDestroy();
        }

    }
}
