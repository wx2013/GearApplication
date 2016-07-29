package gear.yc.com.gearapplication.ui.mvp.travelnotes;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import javax.inject.Inject;

import gear.yc.com.gearapplication.R;
import gear.yc.com.gearapplication.base.BaseActivity;
import gear.yc.com.gearapplication.component.DaggerComponentPresenter;
import gear.yc.com.gearapplication.component.medules.ModulePresenter;
import gear.yc.com.gearapplication.pojo.TravelNoteBook;
import gear.yc.com.gearapplication.ui.activity.SearchBooksActivity;
import gear.yc.com.gearapplication.ui.activity.TravelNotesBookDetailsActivity;
import gear.yc.com.gearlibrary.rxjava.rxbus.RxBus;
import gear.yc.com.gearlibrary.rxjava.rxbus.annotation.Subscribe;
import gear.yc.com.gearlibrary.ui.adapter.GearRecyclerViewAdapter;
import gear.yc.com.gearlibrary.utils.ToastUtil;


/**
 * GearApplication
 * Created by YichenZ on 2016/4/20 15:59.
 */
public class TravelNotesActivity extends BaseActivity implements TravelNotesContract.View, GearRecyclerViewAdapter.OnRecyclerViewItemClickListener<TravelNoteBook.Books> {
    @Inject
    TravelNotesPresenter presenter;
    @Inject
    TravelNotesAdapter mNotesAdapter;

    RecyclerView mRecyclerView;
    ImageView mBack;
    ImageView mLeft;
    TextView mTitle;
    FloatingActionButton mSearch;
    SwipeRefreshLayout refresh;


    LinearLayoutManager mLinearLayoutManager;
    GridLayoutManager mGridLayoutManager;

    //负责记录list样式
    boolean isLinear = true;
    private int page = 1;
    private String query = "";
    private String initQuery = "";
    int lastVisibleItem;
    boolean isMore=false;

    boolean isNote=true;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerComponentPresenter.builder().modulePresenter(new ModulePresenter(this,this)).build().inject(this);
        initUI();
        initData();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        query = getIntent().getStringExtra(J_FLAG);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!initQuery.equals(query) && query != null) {
            initQuery = query;
            page = presenter.refreshData(query, page,isNote);
        }
    }

    @Override
    protected void onDestroy() {
        presenter.close();
        super.onDestroy();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
    }

    @Override
    public void initUI() {
        super.initUI();
        setContentView(R.layout.activity_travel_notes);
        refresh = (SwipeRefreshLayout) findViewById(R.id.srl_refresh);
        refresh.setColorSchemeColors(R.color.colorPrimary, R.color.colorAccent);
        refresh.setOnRefreshListener(() -> page = presenter.refreshData(query, page,isNote));

        mRecyclerView = (RecyclerView) findViewById(R.id.rv_books);
        mLinearLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);

        mBack = (ImageView) findViewById(R.id.iv_back);
        mBack.setVisibility(View.INVISIBLE);

        mLeft = (ImageView) findViewById(R.id.iv_left);
        mLeft.setVisibility(View.GONE);
        mLeft.setImageResource(R.drawable.img_lv);
        mLeft.setOnClickListener(this);

        mTitle = (TextView) findViewById(R.id.tv_title);
        mTitle.setText("游记");

    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void initData() {
        super.initData();
//        mNotesAdapter = new TravelNotesAdapter(this, new ArrayList());
        mRecyclerView.setAdapter(mNotesAdapter);
        mNotesAdapter.setOnItemClickListener(this);

        mSearch = (FloatingActionButton) findViewById(R.id.fab_search);
        mSearch.setOnClickListener(this);

        mRecyclerView.setOnScrollListener(rScrollListener);
        presenter.loadData(query, page);

    }

    @Override
    public void onClick(View v) {
        super.onClick(v);
        switch (v.getId()) {
            case R.id.iv_left:
//                changeListView();
                break;
            case R.id.fab_search:
                strActivity(this, SearchBooksActivity.class, false, false, initQuery);
                break;
        }
    }

    @Subscribe(tag = RxBus.TAG_DEFAULT)
    private void dataBinding(ArrayList<TravelNoteBook.Books> bookies) {
        if (page == 1) {
            mNotesAdapter.setData(bookies);
        } else {
            mNotesAdapter.getData().addAll(bookies);
        }
        if (bookies.size() == 0) {
            isMore=false;
        }else {
            isMore=true;
        }
        mNotesAdapter.notifyDataSetChanged();
        page++;
    }

    @Subscribe(tag = RxBus.TAG_ERROR)
    private void dataError(String error) {
        ToastUtil.getInstance().makeShortToast(this, error);
    }

    @Subscribe(tag = 100)
    private void setNote(boolean isNote){
        this.isNote=isNote;
    }


    /**
     * 切换列表显示方式
     */
    @Override
    public void changeListView() {
        if (isLinear) {
            isLinear = false;
            mLeft.setImageResource(R.drawable.img_gridview);
            if (mGridLayoutManager == null) {
                mGridLayoutManager = new GridLayoutManager(this, 2);
            }
            mGridLayoutManager.scrollToPositionWithOffset(mLinearLayoutManager.findFirstVisibleItemPosition(), 1);
            mRecyclerView.setLayoutManager(mGridLayoutManager);
        } else {
            isLinear = true;
            mLeft.setImageResource(R.drawable.img_lv);
            if (mLinearLayoutManager == null) {
                mLinearLayoutManager = new LinearLayoutManager(this);
            }
            mLinearLayoutManager.scrollToPositionWithOffset(mGridLayoutManager.findFirstVisibleItemPosition(), -1);
            mRecyclerView.setLayoutManager(mLinearLayoutManager);
        }
    }

    @Override
    public void showDialog() {
        refresh.setRefreshing(true);
    }

    @Override
    public void disDialog() {
        refresh.setRefreshing(false);
    }

    RecyclerView.OnScrollListener rScrollListener = new RecyclerView.OnScrollListener() {
        @Override
        public void onScrollStateChanged(RecyclerView recyclerView,
                                         int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (isMore && newState == RecyclerView.SCROLL_STATE_IDLE
                    && lastVisibleItem + 1 == mNotesAdapter.getItemCount()) {
                if(isNote) {
                    presenter.loadData(query, page);
                }else{
                    presenter.loadData(query, page,10);
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            if (isLinear) {
                lastVisibleItem = mLinearLayoutManager.findLastVisibleItemPosition();
            } else {
                lastVisibleItem = mGridLayoutManager.findLastVisibleItemPosition();
            }

            if (dy >= 1) {
                mSearch.hide();
            } else {
                mSearch.show();
            }
        }
    };

    @Override
    public void onItemClick(View view, TravelNoteBook.Books data) {

        Intent intent = new Intent(this, TravelNotesBookDetailsActivity.class);
        intent.putExtra(J_FLAG, data.getHtml());
        intent.putExtra(J_FLAG2, "游记详情");
        intent.putExtra("imgUrl", data.getImgUrl());
        ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(this,
                        view.findViewById(R.id.cv_data), getString(R.string.tra_name_list_img));
        ActivityCompat.startActivity(this, intent, options.toBundle());
    }

}