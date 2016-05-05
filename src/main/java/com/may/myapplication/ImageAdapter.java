package com.may.myapplication;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.util.List;

/**
 * Created by Administrator on 2016/5/4.
 */
public class ImageAdapter extends BaseAdapter {
    private String[] urls;
    private Context mContext;
    private LayoutInflater inflater;
    private ImageLoader imageLoader;
    private int imageWidth =0;
    private Bitmap defaultBitmap;

    public ImageAdapter(String[] urls, Context mContext) {
        this.urls = urls;
        this.mContext = mContext;
        inflater = LayoutInflater.from(mContext);
        imageLoader =new ImageLoader(mContext);
        imageWidth =ImageLoader.getScreenSize(mContext).widthPixels/3;
        defaultBitmap=ImageLoader.decodeBitmapFromResource(mContext.getResources(),R.drawable.default1,imageWidth,imageWidth);
    }

    @Override
    public int getCount() {
        return urls.length;
    }

    @Override
    public Object getItem(int position) {
        return urls[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.grid_view_item, parent, false);
            holder = new ViewHolder();
            holder.imageView = (ImageView) convertView.findViewById(R.id.image);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ImageView imageView =holder.imageView;
        //先设置默认图片
        imageView.setImageBitmap(defaultBitmap);
        String url = (String) getItem(position);
        //加载指定图片
        imageView.setTag(url);
        imageLoader.bindBitmap(url,imageView,imageWidth,imageWidth);
        return convertView;
    }

    class ViewHolder {
        ImageView imageView;
    }
}
