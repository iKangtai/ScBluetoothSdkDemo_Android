package com.example.blesdkdemo.txy.ui;

/**
 * desc
 *
 * @author xiongyl 2020/11/5 14:21
 */

import android.graphics.Color;
import android.graphics.Paint;

import com.example.blesdkdemo.BleApplication;
import com.example.blesdkdemo.R;
import com.example.blesdkdemo.util.Util;

public class FHRPaint {
    private static int color_safe_FHR = Color.rgb(193, 255, 192);

    public static Paint getThinLine() {
        Paint paint = new Paint();
        paint.setStrokeWidth(Util.dp2px(1f));
        paint.setColor(BleApplication.getInstance().getResources().getColor(R.color.color_e8e8e8));
        return paint;
    }

    public static Paint getThickLine() {
        Paint paint = new Paint();
        paint.setStrokeWidth(Util.dp2px(1f));
        paint.setColor(BleApplication.getInstance().getResources().getColor(R.color.color_e8e8e8));
        return paint;
    }

    public static Paint getBaseLine() {
        Paint paint = new Paint();
        paint.setStrokeWidth(Util.dp2px(1.5f));
        paint.setColor(BleApplication.getInstance().getResources().getColor(R.color.color_67A3FF));
        return paint;
    }

    public static Paint getStartPaint() {
        Paint paint = new Paint();
        paint.setStrokeWidth(Util.dp2px(1.5f));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(BleApplication.getInstance().getResources().getColor(R.color.color_67A3FF));
        return paint;
    }

    public static Paint getFHRLine() {
        Paint paint = new Paint();
        paint.setStrokeWidth(Util.dp2px(1.5f));
        paint.setAntiAlias(true);
        paint.setColor(BleApplication.getInstance().getResources().getColor(R.color.app_primary_dark_color));
        return paint;
    }

    public static Paint getFHRText() {
        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.RIGHT);
        //paint.setStrokeWidth(2.0f);
        paint.setColor(BleApplication.getInstance().getResources().getColor(R.color.color_444444));
        return paint;
    }

    public static Paint getMinuteText() {
        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(BleApplication.getInstance().getResources().getColor(R.color.color_444444));
        //paint.setStrokeWidth(2.0f);
        return paint;
    }

    public static Paint getSafeFHRPaint() {
        Paint paint = new Paint();
        paint.setColor(BleApplication.getInstance().getResources().getColor(R.color.color_4d81b2fe));
        return paint;
    }

    public static Paint getQuickeningPaint() {
        Paint paint = new Paint();
        paint.setColor(BleApplication.getInstance().getResources().getColor(R.color.app_primary_dark_color));
        return paint;
    }
}
