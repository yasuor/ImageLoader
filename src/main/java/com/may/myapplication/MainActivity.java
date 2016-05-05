package com.may.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Window;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private GridView mGridView;
    private Toolbar mToolbar;
    private final String LOCAL_HOST_IP="http://10.139.5.159";
    private String[] urls={
            LOCAL_HOST_IP+"/m1.jpg",LOCAL_HOST_IP+"/m2.jpg",
            LOCAL_HOST_IP+"/m3.jpg",LOCAL_HOST_IP+"/m4.jpg",
            LOCAL_HOST_IP+"/m5.jpg",LOCAL_HOST_IP+"/m6.jpg",
            LOCAL_HOST_IP+"/m7.jpg",LOCAL_HOST_IP+"/m8.jpg",
            LOCAL_HOST_IP+"/m9.jpg",LOCAL_HOST_IP+"/m10.jpg",
            LOCAL_HOST_IP+"/m11.jpg",LOCAL_HOST_IP+"/m12.jpg",
            LOCAL_HOST_IP+"/m13.jpg",LOCAL_HOST_IP+"/m14.jpg",
            LOCAL_HOST_IP+"/m15.jpg",LOCAL_HOST_IP+"/m16.jpg",
            LOCAL_HOST_IP+"/m17.jpg",LOCAL_HOST_IP+"/m18.jpg",
            LOCAL_HOST_IP+"/m19.jpg",LOCAL_HOST_IP+"/m20.jpg",
            LOCAL_HOST_IP+"/m21.jpg",LOCAL_HOST_IP+"/m22.jpg",
            LOCAL_HOST_IP+"/m23.jpg",LOCAL_HOST_IP+"/m24.jpg",
            LOCAL_HOST_IP+"/m25.jpg",LOCAL_HOST_IP+"/m26.jpg"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        mGridView = (GridView) findViewById(R.id.gridView);
        mGridView.setAdapter(new ImageAdapter(urls,this));
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle("照片墙");
        setSupportActionBar(mToolbar);
    }

}
