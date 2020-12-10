package com.example.blesdkdemo.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.StatFs;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;

import com.example.blesdkdemo.MyApplication;

/**
 * desc
 *
 * @author xiongyl 2020/11/5 17:23
 */
public class Util {

    public static float dp2px(int i) {
        return TypedValue.applyDimension(1, (float) i, MyApplication.getInstance().getResources().getDisplayMetrics());
    }

    public static int getFreeMB() {
        return (int) (new StatFs("/data").getAvailableBytes() / PlaybackStateCompat.ACTION_SET_CAPTIONING_ENABLED);
    }

    @TargetApi(19)
    public static void setNavigationBar(Activity activity) {
        activity.getWindow().addFlags(134217728);
    }

    public static int getNavigationBarHeight(Activity activity) {
        Display defaultDisplay = activity.getWindowManager().getDefaultDisplay();
        Point point = new Point();
        Point point2 = new Point();
        defaultDisplay.getSize(point);
        defaultDisplay.getRealSize(point2);
        return Math.abs(point.y - point2.y);
    }

    public static void solveNavigationBar(Activity activity) {
        if (getNavigationBarHeight(activity) > 0) {
            activity.getWindow().getDecorView().setSystemUiVisibility(Build.VERSION.SDK_INT >= 19 ? 5890 : 1795);
        }
    }

    public static String getVersion() {
        String str;
        PackageManager.NameNotFoundException e;
        try {
            str = MyApplication.getInstance().getPackageManager().getPackageInfo(MyApplication.getInstance().getPackageName(), 0).versionName;
            Log.e("TAG", "getVersion: " + str);
        } catch (PackageManager.NameNotFoundException e3) {
            PackageManager.NameNotFoundException nameNotFoundException = e3;
            str = "1.0.0-beta";
            e = nameNotFoundException;
            e.printStackTrace();
            return str;
        }
        return str;
    }

    public static byte[] toByteArray(short[] sArr) {
        int length = sArr.length;
        byte[] bArr = new byte[(length << 1)];
        for (int i = 0; i < length; i++) {
            int i2 = i * 2;
            bArr[i2] = (byte) sArr[i];
            bArr[i2 + 1] = (byte) (sArr[i] >> 8);
        }
        return bArr;
    }

    public short[] toShortArray(byte[] bArr) {
        int length = bArr.length >> 1;
        short[] sArr = new short[length];
        for (int i = 0; i < length; i++) {
            int i2 = i * 2;
            sArr[i] = (short) (((bArr[i2 + 1] & 255) << 8) | (bArr[i2] & 255));
        }
        return sArr;
    }


}
