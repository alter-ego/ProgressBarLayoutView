package com.alterego.progressbarlayout.example;

import android.os.Bundle;

import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.alterego.progressbarlayout.ProgressBarLayoutView;


public class MyActivity extends ActionBarActivity {

    private ProgressBarLayoutView mCircleProgressView;
    private TextView mBeginningTextView;
    private TextView mEndingTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mBeginningTextView = (TextView) findViewById(R.id.beginning_tv);
        mEndingTextView = (TextView) findViewById(R.id.ending_tv);
        mCircleProgressView = (ProgressBarLayoutView) findViewById(R.id.expanded_circle_progress);
        mCircleProgressView.setBeginningCrossAnimationView(mBeginningTextView);
        mCircleProgressView.setEndingCrossAnimationView(mEndingTextView);

        mCircleProgressView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCircleProgressView.setProgress(100);
            }
        });

        mEndingTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mEndingTextView.setVisibility(View.GONE);
                mBeginningTextView.setVisibility(View.VISIBLE);
                //mCircleProgressView.setProgress(30);
            }
        });

        mBeginningTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCircleProgressView.reset();
                mCircleProgressView.setProgress(30);
            }
        });

        try {
            mCircleProgressView.setProgress(30);
        } catch (Exception e) {
            Log.e("ProgressBarLayoutView", "MyActivity.growProgress exception = " + e.toString());
        }
    }

}
