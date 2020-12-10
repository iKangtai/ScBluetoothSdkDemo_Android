package com.example.blesdkdemo.ui;

/**
 * desc
 *
 * @author xiongyl 2020/11/5 14:21
 */
import android.graphics.Color;
import android.graphics.Paint;

import com.example.blesdkdemo.MyApplication;
import com.example.blesdkdemo.R;
import com.example.blesdkdemo.util.FontUtil;

import androidx.core.internal.view.SupportMenu;
import androidx.core.view.ViewCompat;

public class MyPaint {
    private static int color_safe_FHR = Color.rgb(193, 255, 192);

    public static Paint getThinLine() {
        Paint paint = new Paint();
        paint.setStrokeWidth(2.0f);
        paint.setColor(MyApplication.getInstance().getResources().getColor(R.color.title_background));
        return paint;
    }

    public static Paint getThickLine() {
        Paint paint = new Paint();
        paint.setStrokeWidth(3.0f);
        paint.setColor(MyApplication.getInstance().getResources().getColor(R.color.title_background));
        return paint;
    }

    public static Paint getBaseLine() {
        Paint paint = new Paint();
        paint.setStrokeWidth(4.0f);
        paint.setColor(SupportMenu.CATEGORY_MASK);
        return paint;
    }

    public static Paint getStartPaint() {
        Paint paint = new Paint();
        paint.setStrokeWidth(4.0f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(ViewCompat.MEASURED_STATE_MASK);
        return paint;
    }

    public static Paint getFHRLine() {
        Paint paint = new Paint();
        paint.setStrokeWidth(2.0f);
        paint.setAntiAlias(true);
        return paint;
    }

    public static Paint getFHRText() {
        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.RIGHT);
        if (FontUtil.typeface != null) {
            paint.setTypeface(FontUtil.typeface);
        }
        paint.setStrokeWidth(2.0f);
        return paint;
    }

    public static Paint getMinuteText() {
        Paint paint = new Paint();
        paint.setTextAlign(Paint.Align.CENTER);
        if (FontUtil.typeface != null) {
            paint.setTypeface(FontUtil.typeface);
        }
        paint.setColor(MyApplication.getInstance().getResources().getColor(R.color.title_background));
        paint.setStrokeWidth(2.0f);
        return paint;
    }

    public static Paint getSafeFHRPaint() {
        Paint paint = new Paint();
        paint.setColor(color_safe_FHR);
        return paint;
    }
}
