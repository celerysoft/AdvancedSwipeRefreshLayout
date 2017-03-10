package com.celerysoft.demo;

import android.animation.ValueAnimator;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.celerysoft.AdvancedSwipeRefreshLayout;

/**
 * Created by Celery on 2017/1/25.
 *
 */

public class RecyclerViewActivity2 extends AppCompatActivity {
    private AdvancedSwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private SimpleRecyclerViewAdapter mAdapter;

    private TextView mHeaderTv;
    private ImageView mHeaderArrow;

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
                mHeaderTv.setText("Refreshing");

                final ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, 360f).setDuration(250);
                valueAnimator.setRepeatCount(-1);
                valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        mHeaderArrow.setRotationX((float) animation.getAnimatedValue());
                    }
                });
                valueAnimator.start();

                mSwipeRefreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        valueAnimator.end();
                        mSwipeRefreshLayout.setRefreshing(false);
                        mPage = 0;
                        mAdapter.setData(FakeBackend.getStringData(mPage));
                        mPage++;
                    }
                }, 500);
            }

            @Override
            public void onPullDistance(int distance, int distanceToTriggerSync) {
                if (distance < distanceToTriggerSync) {
                    int fixDistance = distance - (int) (0.5f * distanceToTriggerSync);
                    if (fixDistance > 0) {
                        float angle = 180f * ((float) fixDistance / (0.5f * distanceToTriggerSync));
                        mHeaderArrow.setRotationX(angle);
                        mSwipeRefreshLayout.requestLayout();
                    }
                } else {
                    mHeaderArrow.setRotationX(180f);
                    mSwipeRefreshLayout.requestLayout();
                }

            }

            @Override
            public void onPullEnable(boolean enable) {
                if (enable) {
                    mHeaderTv.setText("Release to refresh");
                } else {
                    mHeaderTv.setText("Pull down");
                }
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

        View view = LayoutInflater.from(this).inflate(R.layout.item_custom_header, mSwipeRefreshLayout, false);
        mHeaderTv = (TextView) view.findViewById(R.id.tv);
        mHeaderArrow = (ImageView) view.findViewById(R.id.iv_arrow);
        ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams(getResources().getDisplayMetrics().widthPixels, (int) (getResources().getDisplayMetrics().density * 56));
        mSwipeRefreshLayout.setHeaderView(view, layoutParams);
        mSwipeRefreshLayout.setHeaderScrollTogether(true);
        mSwipeRefreshLayout.setMaxPullAbleDistance(2 * (int) (getResources().getDisplayMetrics().density * 56));

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
