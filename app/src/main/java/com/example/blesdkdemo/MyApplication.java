package com.example.blesdkdemo;

import android.app.Application;
import android.content.Context;
import android.preference.PreferenceManager;

/**
 * desc
 *
 * @author xiongyl 2020/6/18 22:56
 */
public class MyApplication extends Application {
    public AppPreferences appPreferences;
    private static MyApplication instance;

    public static MyApplication getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        appPreferences = new AppPreferences(this);
        instance = this;
    }

    public static class AppPreferences {
        private Context context;

        public AppPreferences(Context context) {
            this.context = context;
        }

        public String getLastDeviceAddress() {
            return PreferenceManager.getDefaultSharedPreferences(context).getString("lastDeviceAddress", "");
        }
    }
}
