package com.ktds.queuing_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;

public class SplashActivity extends ActionBarActivity {
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final String excuteType = getIntent().getStringExtra("executeType");

        // 몇초 뒤에 무엇을 하라는 기능
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = null;
                if(excuteType != null && excuteType.equals("beacon")) {
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    // 비콘과 연동이 되어있다면 MainActivity로 설정이 되어있어야한다.
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                }
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        }, 3000);
    }
}