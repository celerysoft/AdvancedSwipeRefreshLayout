package com.celerysoft.demo;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Celery on 2017/1/25.
 *
 */

public class SimpleAdapter extends RecyclerView.Adapter<SimpleAdapter.ViewHolder> {
    private Context mContext;
    private List<String> mData;

    public SimpleAdapter(Context context) {
        mContext = context;
    }

    public void addData(List<String> data) {
        if (mData == null) {
            mData = new ArrayList<>();
        }
        mData.addAll(data);
        notifyDataSetChanged();
    }

    public void setData(List<String> data) {
        mData = data;
        notifyDataSetChanged();
    }

    @Override
    public SimpleAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_simple_recycler_view, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(SimpleAdapter.ViewHolder holder, int position) {
        try {
            holder.tv.setText(mData.get(position));
        } catch (Exception e) {
            e.printStackTrace();
            holder.tv.setText("error");
        }

    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tv;

        public ViewHolder(View itemView) {
            super(itemView);

            tv = (TextView) itemView.findViewById(R.id.tv);
        }
    }
}
