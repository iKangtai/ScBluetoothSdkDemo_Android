package com.example.blesdkdemo;


import android.annotation.SuppressLint;
import android.os.Bundle;

import com.example.blesdkdemo.util.FontUtil;

import androidx.appcompat.app.AppCompatActivity;

@SuppressLint({"Registered"})
public class BaseActivity extends AppCompatActivity {
    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        FontUtil.changeAllFont(this);
        super.onCreate(bundle);
    }
}
