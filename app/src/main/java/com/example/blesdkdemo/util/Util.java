package com.example.blesdkdemo.util;

import android.app.Activity;
import android.graphics.Point;
import android.os.StatFs;
import android.util.TypedValue;
import android.view.Display;

import com.example.blesdkdemo.BleApplication;


/**
 * desc
 *
 * @author xiongyl 2020/11/5 17:23
 */
public class Util {

    public static float dp2px(float i) {
        return TypedValue.applyDimension(1, i, BleApplication.getInstance().getResources().getDisplayMetrics());
    }

    public static int getFreeMB() {
        return (int) (new StatFs("/data").getAvailableBytes() / (1 << 20));
    }


    public static int getNavigationBarHeight(Activity activity) {
        Display defaultDisplay = activity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        Point point2 = new Point();
        defaultDisplay.getSize(point);
        defaultDisplay.getRealSize(point2);
        return Math.abs(point.y - point2.y);
    }

}
