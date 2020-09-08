package com.tim.fly;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        testLog();
    }

    //测试时间
    private void testLog(){
        try {
            Thread.sleep(1200);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}