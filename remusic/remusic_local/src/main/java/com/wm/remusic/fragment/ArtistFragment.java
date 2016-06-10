package com.wm.remusic.fragment;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.wm.remusic.R;
import com.wm.remusic.info.ArtistInfo;
import com.wm.remusic.lastfmapi.LastFmClient;
import com.wm.remusic.lastfmapi.callbacks.ArtistInfoListener;
import com.wm.remusic.lastfmapi.models.ArtistQuery;
import com.wm.remusic.lastfmapi.models.LastfmArtist;
import com.wm.remusic.service.MusicPlayer;
import com.wm.remusic.uitl.DividerItemDecoration;
import com.wm.remusic.uitl.IConstants;
import com.wm.remusic.uitl.MusicUtils;
import com.wm.remusic.uitl.PreferencesUtility;
import com.wm.remusic.uitl.SortOrder;

import java.util.List;

/**
 * Created by wm on 2016/1/17.
 */
public class ArtistFragment extends BaseFragment {

    private List<ArtistInfo> artistInfos;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private ArtistAdapter mAdapter;
    private PreferencesUtility mPreferences;
    private RecyclerView.ItemDecoration itemDecoration;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.recylerview, container, false);

        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(layoutManager);
        //fastScroller = (FastScroller) view.findViewById(R.id.fastscroller);
        //new loadArtists().execute("");
        mAdapter = new ArtistAdapter(null);
        recyclerView.setAdapter(mAdapter);
        setItemDecoration();
        reloadAdapter();

        return view;
    }

    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPreferences = PreferencesUtility.getInstance(getActivity());
    }


    @Override
    public void onActivityCreated(final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.artist_sort_by, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sort_by_az:
                mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_A_Z);
                reloadAdapter();
                return true;
            case R.id.menu_sort_by_number_of_songs:
                mPreferences.setArtistSortOrder(SortOrder.ArtistSortOrder.ARTIST_NUMBER_OF_SONGS);
                reloadAdapter();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //设置分割线
    private void setItemDecoration() {

        itemDecoration = new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST);
        recyclerView.addItemDecoration(itemDecoration);
    }

    //更新adapter界面
    public void reloadAdapter() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(final Void... unused) {
                List<ArtistInfo> artList = MusicUtils.queryArtist(getActivity());
                if (artList != null)
                    mAdapter.updateDataSet(artList);
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                mAdapter.notifyDataSetChanged();
            }
        }.execute();
    }

    //异步加载recyclerview界面
    private class loadArtists extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            if (getActivity() != null) {
                artistInfos = MusicUtils.queryArtist(getActivity());
                if (artistInfos != null)
                    mAdapter = new ArtistAdapter(artistInfos);
            }

            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            recyclerView.setAdapter(mAdapter);
            if (getActivity() != null) {
                setItemDecoration();
            }
        }

        @Override
        protected void onPreExecute() {
        }
    }

    public class ArtistAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private List<ArtistInfo> mList;

        public ArtistAdapter(List<ArtistInfo> list) {
//            if (list == null) {
//                throw new IllegalArgumentException("model Data must not be null");
//            }
            mList = list;
        }

        //更新adpter的数据
        public void updateDataSet(List<ArtistInfo> list) {
            this.mList = list;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
            View itemView = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.recyclerview_common_item, viewGroup, false);
            return new ListItemViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int i) {
            ArtistInfo model = mList.get(i);
            //设置条目状态
            ((ListItemViewHolder) holder).mainTitle.setText(model.artist_name);
            ((ListItemViewHolder) holder).title.setText(model.number_of_tracks + "首");

            //根据播放中歌曲的歌手名判断当前歌手专辑条目是否有播放的歌曲
            if (MusicPlayer.getCurrentArtistId() == (model.artist_id)) {
                ((ListItemViewHolder) holder).moreOverflow.setImageResource(R.drawable.song_play_icon);
            } else {
                ((ListItemViewHolder) holder).moreOverflow.setImageResource(R.drawable.abc_ic_menu_moreoverflow_mtrl_alpha);
            }

            //lastFm api加载歌手图片
            LastFmClient.getInstance(getContext()).getArtistInfo(new ArtistQuery(model.artist_name.toString()), new ArtistInfoListener() {
                @Override
                public void artistInfoSucess(LastfmArtist artist) {
                    if (artist != null && artist.mArtwork != null) {
                        ((ListItemViewHolder) holder).draweeView.setImageURI(Uri.parse(artist.mArtwork.get(2).mUrl));

                    } else {

                    }
                }

                @Override
                public void artistInfoFailed() {

                }
            });

        }

        @Override
        public int getItemCount() {
            return mList == null ? 0 : mList.size();
        }

        public class ListItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
            //ViewHolder
            SimpleDraweeView draweeView;
            TextView mainTitle, title;
            ImageView moreOverflow;

            ListItemViewHolder(View view) {
                super(view);
                this.mainTitle = (TextView) view.findViewById(R.id.viewpager_list_toptext);
                this.title = (TextView) view.findViewById(R.id.viewpager_list_bottom_text);
                this.draweeView = (SimpleDraweeView) view.findViewById(R.id.viewpager_list_img);
                this.moreOverflow = (ImageView) view.findViewById(R.id.viewpager_list_button);

                //弹出frament菜单
                this.moreOverflow.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        MoreFragment morefragment = new MoreFragment().newInstance(mList.get(getAdapterPosition()).artist_id + "", IConstants.ARTISTOVERFLOW);
                        morefragment.show(getFragmentManager(), "");
                    }
                });

                //为每个条目设置监听
                view.setOnClickListener(this);

            }

            //加载歌手专辑界面fragment
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = ((AppCompatActivity) getContext()).getSupportFragmentManager().beginTransaction();
                ArtistDetailFragment fragment = ArtistDetailFragment.newInstance(mList.get(getAdapterPosition()).artist_id);
                transaction.hide(((AppCompatActivity) getContext()).getSupportFragmentManager().findFragmentById(R.id.fragment_container));
                transaction.add(R.id.fragment_container, fragment);
                transaction.addToBackStack(null).commit();
            }

        }
    }


}
