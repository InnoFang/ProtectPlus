package io.innofang.medically;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.alibaba.android.arouter.facade.annotation.Route;

/**
 * Author: Inno Fang
 * Time: 2017/9/30 15:13
 * Description:
 */


@Route(path = "/heart_beat/1")
public class HeartBeatActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_heart_beat);
        if (null == savedInstanceState) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.container, HeartBeatOldFragment.newInstance())
                    .commit();
        }
    }
}