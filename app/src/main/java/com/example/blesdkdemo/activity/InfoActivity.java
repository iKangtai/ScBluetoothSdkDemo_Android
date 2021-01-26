package com.example.blesdkdemo.activity;

import android.os.Bundle;

import com.example.blesdkdemo.ui.InfoFragment;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

/**
 * 体温计详情
 *
 * @author xiongyl 2020/9/24 0:55
 */
public class InfoActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        InfoFragment infoFragment = new InfoFragment();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().add(android.R.id.content, infoFragment).commit();
    }
}
