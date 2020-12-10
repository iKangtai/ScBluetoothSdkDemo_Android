package com.example.blesdkdemo.util;

import android.content.Context;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.LayoutInflaterCompat;

public class FontUtil {
    public static Typeface typeface;

    private FontUtil() {
    }

    public static void init(Context context) {
        try {
            if (typeface == null) {
                typeface = Typeface.createFromAsset(context.getAssets(), "arial.ttf");
            }
        } catch (Exception unused) {
            Toast.makeText(context, "字体文件加载失败！", Toast.LENGTH_LONG).show();
        }
    }

    public static void changeAllFont(final AppCompatActivity appCompatActivity) {
        LayoutInflaterCompat.setFactory2(LayoutInflater.from(appCompatActivity), new LayoutInflater.Factory2() {
            public View onCreateView(String str, Context context, AttributeSet attributeSet) {
                return null;
            }

            public View onCreateView(View view, String str, Context context, AttributeSet attributeSet) {
                View createView = appCompatActivity.getDelegate().createView(view, str, context, attributeSet);
                if (!(FontUtil.typeface == null || createView == null || !(createView instanceof TextView))) {
                    ((TextView) createView).setTypeface(FontUtil.typeface);
                }
                return createView;
            }
        });
    }
}
