package com.reactnativevideoplayer.activity;

import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.reactnativevideoplayer.R;
import com.reactnativevideoplayer.view.VideoPlayer;

/**
 * Created by runing on 2016/11/14.
 */

public class TestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        VideoPlayer mVideoPlayer = (VideoPlayer) findViewById(R.id.vPlayer);

        mVideoPlayer.setUrl("http://clips.vorwaerts-gmbh.de/big_buck_bunny.mp4");
        mVideoPlayer.enableAuto();
        mVideoPlayer.setResizeMode(VideoPlayer.RESIZE_MODE_CONTAIN);
    }
}
