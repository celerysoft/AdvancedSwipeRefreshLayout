package com.celerysoft.demo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Celery on 2017/2/28.
 *
 */

public class SimpleListViewAdapter extends BaseAdapter {
    private Context mContext;
    private List<String> mData;

    public SimpleListViewAdapter(Context context) {
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
    public int getCount() {
        return mData == null ? 0 : mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData == null ? null : mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;

        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_simple_recycler_view, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.tv = (TextView) convertView.findViewById(R.id.tv);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        try {
            viewHolder.tv.setText(mData.get(position));
        } catch (Exception e) {
            e.printStackTrace();
            viewHolder.tv.setText("error");
        }

        return convertView;
    }

    private class ViewHolder {
        TextView tv;
    }
}
