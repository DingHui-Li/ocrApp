package com.main.ocrapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by Administrator on 2018/12/12/0012.
 */

public class listAdapter extends BaseAdapter {
    private Context context;
    private List<list_item> historyData;
    public boolean isChoice=false;
    public listAdapter(Context context,List<list_item>historyData){
        this.context=context;
        this.historyData=historyData;
    }
    @Override
    public int getCount() {
        return historyData.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder vh=null;
//        if(convertView==null){
            convertView= LayoutInflater.from(context).inflate(R.layout.list_item,parent,false);
            vh=new ViewHolder();
            vh.iv=convertView.findViewById(R.id.coverIV);
            vh.tv=convertView.findViewById(R.id.titleTV);
            vh.timeTV=convertView.findViewById(R.id.timeTV);
            convertView.setTag(vh);
//        }else{
//            vh=(ViewHolder) convertView.getTag();
//        }
        File file = new File(historyData.get(position).getCover());
        Glide.with(context).load(file).into(vh.iv);
        vh.tv.setText(historyData.get(position).getContent());
        vh.timeTV.setText(historyData.get(position).getTime());
        return convertView;
    }
    class ViewHolder{
        ImageView iv;
        TextView tv;
        TextView timeTV;
    }
}
