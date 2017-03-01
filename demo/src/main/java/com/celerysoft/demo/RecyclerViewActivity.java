package com.celerysoft.demo;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.celerysoft.AdvancedSwipeRefreshLayout;

/**
 * Created by Celery on 2017/1/25.
 *
 */

public class RecyclerViewActivity extends AppCompatActivity {
    private AdvancedSwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private SimpleRecyclerViewAdapter mAdapter;

    private int mPage = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recycler_view);

        initActivity();
    }

    private void initActivity() {
        bindView();
        bindListener();
        initData();
        initView();
    }

    private void bindView() {
        mSwipeRefreshLayout = (AdvancedSwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
    }

    private void bindListener() {
        mSwipeRefreshLayout.setOnPullToRefreshListener(new AdvancedSwipeRefreshLayout.OnPullToRefreshListener() {
            @Override
            public void onRefresh() {
                mSwipeRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setRefreshing(false);
                        mPage = 0;
                        mAdapter.setData(FakeBackend.getStringData(mPage));
                        mPage++;
                    }
                }, 500);
            }

            @Override
            public void onPullDistance(int distance, int distanceToTriggerSync) {

            }

            @Override
            public void onPullEnable(boolean enable) {

            }
        });

        mSwipeRefreshLayout.setOnPushToLoadMoreListener(new AdvancedSwipeRefreshLayout.OnPushToLoadMoreListener() {
            @Override
            public void onLoadMore() {
                mSwipeRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mSwipeRefreshLayout.setLoadingMore(false);
                        mAdapter.addData(FakeBackend.getStringData(mPage));
                        mPage++;
                    }
                }, 500);
            }

            @Override
            public void onPushDistance(int distance) {

            }

            @Override
            public void onPushEnable(boolean enable) {

            }
        });
    }

    private void initData() {
        mPage = 0;
        mAdapter = new SimpleRecyclerViewAdapter(this);
        mAdapter.setData(FakeBackend.getStringData(mPage));
        mPage++;
        mRecyclerView.setAdapter(mAdapter);
    }

    private void initView() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("RecyclerView");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        mSwipeRefreshLayout.setColorSchemeColors(
                getResources().getColor(R.color.colorAccent),
                getResources().getColor(R.color.colorPrimary),
                getResources().getColor(R.color.colorPrimaryDark)
        );

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            this.finish();
        }

        return super.onOptionsItemSelected(item);
    }
}
