package com.example.blesdkdemo.txy.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.example.blesdkdemo.BleApplication;
import com.example.blesdkdemo.R;
import com.example.blesdkdemo.util.Util;
import com.ikangtai.bluetoothsdk.util.PxDxUtil;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;

public class FHRMonitorView extends SurfaceView implements SurfaceHolder.Callback {
    public static final int AUTO_MOVE_TIME = 3000;
    private static final int BIG_FHR_SCALE_NUM = 3;
    public static final int DATA_TIME = 500;
    public static final int DISCONNECT_FLAG = -1;
    public static final int FHR_TEXT_SIZE = 13;
    public static final int H = 8;
    public static final long INIT_TIME = 25;
    public static final int LEAVE_FLAG = -2;
    public static final int MAX_DIVIDE = 30;
    public static final int MINUTE_TEXT_SIZE = 16;
    public static final String START_FLAG_TEXT = BleApplication.getInstance().getResources().getString(R.string.start_flag);
    public static final int STATE_SIZE = 0;
    public static final String STATE_TEXT = "胎心率 FHR/BPM";
    private static final String TAG = "MonitorSurfaceView_TAG";
    public static final int W = 4;
    private Paint baseLine;
    private boolean canTouch = true;
    private Canvas canvas;
    private LinkedList<Integer> fhrBuff = new LinkedList<>();
    private ArrayList<FHRData> fhrDataList = new ArrayList<>();
    private Paint fhrLine;
    private OnFhrListener fhrListener;
    private Paint fhrText;
    private SurfaceHolder holder;
    private boolean isConnect = false;
    private boolean isHistory = false;
    private boolean isHistoryMode = false;
    private boolean isQuickening;
    private boolean isRunning;
    private int maxFHRScale = 240;
    private int minFHRScale = 30;
    private float min_scale_height;
    private int minuteScale = 1;
    private Paint minuteText;
    private MonitorTH monitorTH;
    private ExecutorService monitorThread;
    private int num_FHRScaleLine = ((this.maxFHRScale - this.minFHRScale) / 10);
    private int num_x_line;
    private OnMonitorTouchListener onMonitorTouchListener;
    private boolean pause = false;
    private double per_cm_of_px;
    private ArrayList<FHRPoint> pointList = new ArrayList<>();
    private Paint safeFHRPain;
    private float scale_y_px;
    private Paint startPaint;
    private float startX;
    private float startY;
    private float stopY;
    private float text_FHR_down_offset = (Util.dp2px(12) / 2.0f);
    private float text_FHR_left_offset = Util.dp2px(2);
    private Paint thickLine;
    private Paint thinLine;
    private long touchUpTime;
    private float x1;
    private float x2;
    private float x3;
    private float x4;
    private float xLength;
    private Paint quickeningPaint;

    public void setFhrListener(OnFhrListener onFhrListener) {
        this.fhrListener = onFhrListener;
    }

    public void setOnMonitorTouchListener(OnMonitorTouchListener onMonitorTouchListener2) {
        this.onMonitorTouchListener = onMonitorTouchListener2;
    }

    public void setPause(boolean z) {
        this.pause = z;
    }

    public int getQuickeningNum() {
        Iterator<FHRPoint> it = this.pointList.iterator();
        int i = 0;
        while (it.hasNext()) {
            if (it.next().isQuickening()) {
                i++;
            }
        }
        return i;
    }

    public int getNowFHR() {
        if ((this.fhrDataList.size() <= 0) || (!this.isConnect)) {
            return 0;
        }
        return this.fhrDataList.get(this.fhrDataList.size() - 1).getFhr();
    }

    public int getAvaFHR() {
        if ((this.fhrDataList.size() <= 0) || (!this.isConnect)) {
            return 0;
        }
        Iterator<FHRData> it = this.fhrDataList.iterator();
        int num = 0;
        int fhr = 0;
        while (it.hasNext()) {
            FHRData fhrData = it.next();
            if (fhrData.getFhr() > 0) {
                num++;
                fhr += fhrData.getFhr();
            }
        }
        return num > 0 ? fhr / num : num;
    }

    public void addData(@NonNull List<FHRData> list) {
        for (int i = 0; i < list.size(); i++) {
            FHRData fHRData = list.get(i);
            this.pointList.add(new FHRPoint(getFHRTo_XPx(fHRData.getFhr(), i), getFHRTo_YPx(fHRData.getFhr()), fHRData.isQuickening()));
        }
        this.fhrDataList.addAll(list);
        this.pause = false;
    }

    public void clearData() {
        this.pointList.clear();
        this.fhrDataList.clear();
        invalidate();
    }

    public long getRecordingTime(int i) {
        if (this.fhrDataList.size() > 0) {
            return (long) (((this.fhrDataList.size() - 1) - i) * DATA_TIME);
        }
        return 0;
    }

    public int getFHRIndex() {
        return this.pointList.size() - 1;
    }

    public ArrayList<FHRData> getSaveFHR(int i, int i2) {
        ArrayList<FHRData> arrayList = new ArrayList<>();
        if (i < 0) {
            i = 0;
        }
        if (this.fhrDataList.size() > 1 && i <= i2) {
            while (i <= i2) {
                FHRData fHRData = this.fhrDataList.get(i);
                if (fHRData.getFhr() == -2) {
                    break;
                }
                arrayList.add(fHRData);
                i++;
            }
        }
        return arrayList;
    }

    public FHRMonitorView(Context context) {
        super(context);
        initView();
    }

    public FHRMonitorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initView();
    }

    public FHRMonitorView(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        initView();
    }

    private void initView() {
        this.monitorThread = Executors.newSingleThreadExecutor();
        this.monitorTH = new MonitorTH();
        this.holder = getHolder();
        this.holder.addCallback(this);
        this.thinLine = FHRPaint.getThinLine();
        this.thickLine = FHRPaint.getThickLine();
        this.baseLine = FHRPaint.getBaseLine();
        this.startPaint = FHRPaint.getStartPaint();
        this.startPaint.setTextSize(Util.dp2px(13));
        this.quickeningPaint = FHRPaint.getQuickeningPaint();
        this.fhrLine = FHRPaint.getFHRLine();
        this.safeFHRPain = FHRPaint.getSafeFHRPaint();
        this.fhrText = FHRPaint.getFHRText();
        this.fhrText.setTextSize(Util.dp2px(14));
        this.minuteText = FHRPaint.getMinuteText();
        this.minuteText.setTextSize(Util.dp2px(16));
        this.startX = Util.dp2px(42);
        this.startY = (Util.dp2px(13) / 2.0f) + Util.dp2px(9);
        this.min_scale_height = Util.dp2px(16);
        setFocusableInTouchMode(true);
        setKeepScreenOn(true);
    }

    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        this.isRunning = true;
        this.monitorThread.execute(this.monitorTH);
        new FHRThread().start();
    }

    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
        this.scale_y_px = ((((float) i3) - this.min_scale_height) - this.startY) / ((float) this.num_FHRScaleLine);
        this.per_cm_of_px = (double) ((i2 * 106) / 720);
        this.num_x_line = (int) (((double) getWidth()) / this.per_cm_of_px);
        this.xLength = (float) getWidth();
        this.stopY = this.startY + (this.scale_y_px * ((float) this.num_FHRScaleLine));
        long uptimeMillis = SystemClock.uptimeMillis();
        boolean onTouchEvent = onTouchEvent(MotionEvent.obtain(uptimeMillis, uptimeMillis + 100, 2, 0.0f, 0.0f, 1));
        Log.d("MotionEvent", onTouchEvent + "");
    }

    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        closeHistoryMode();
        this.isRunning = false;
    }

    /* access modifiers changed from: private */
    public class MonitorTH extends Thread {
        private MonitorTH() {
        }

        public void run() {
            try {
                Thread.sleep(25);
                while (FHRMonitorView.this.isRunning) {
                    if (FHRMonitorView.this.pause) {
                        sleep(20);
                    } else {
                        FHRMonitorView.this.drawSelf();
                        if (!FHRMonitorView.this.isHistory) {
                            FHRMonitorView.this.pause = !FHRMonitorView.this.isConnect;
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(FHRMonitorView.TAG, "MonitorTH: ", e);
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void drawSelf() {
        try {
            this.canvas = this.holder.lockCanvas();
            if (this.canvas != null) {
                if (this.pointList.size() > 0) {
                    this.num_x_line = (int) ((((double) (getWidth() * 2)) + ((((double) this.pointList.size()) * this.per_cm_of_px) / 60.0d)) / this.per_cm_of_px);
                    this.xLength = (float) (((double) (getWidth() * 2)) + ((((double) this.pointList.size()) * this.per_cm_of_px) / 60.0d));
                }
                this.canvas.drawColor(-1);
                drawCoordinates();
            }
            if (this.canvas == null) {
                return;
            }
        } catch (Exception e) {
            Log.e(TAG, "draws: ", e);
            if (this.canvas == null) {
                return;
            }
        } catch (Throwable th) {
            if (this.canvas != null) {
                this.holder.unlockCanvasAndPost(this.canvas);
            }
            throw th;
        }
        this.holder.unlockCanvasAndPost(this.canvas);
    }

    private void drawCoordinates() {

        Canvas canvas2 = this.canvas;
        this.fhrText.setTextSize(PxDxUtil.sp2px(getContext(), 9));
        canvas2.drawText(getContext().getString(R.string.fhr), this.startX - this.text_FHR_left_offset, this.startY - this.text_FHR_down_offset, this.fhrText);
        this.fhrText.setTextSize(PxDxUtil.sp2px(getContext(), 14));
        canvas2.drawText("" + this.maxFHRScale, this.startX - this.text_FHR_left_offset, this.startY + this.text_FHR_down_offset, this.fhrText);
        for (int i = 1; i <= this.num_FHRScaleLine; i++) {
            if (i % 3 == 0) {
                Canvas canvas3 = this.canvas;
                float f = (float) i;
                canvas3.drawText("" + (this.maxFHRScale - ((i / 3) * this.minFHRScale)), this.startX - this.text_FHR_left_offset, (this.scale_y_px * f) + this.startY + this.text_FHR_down_offset, this.fhrText);
                this.canvas.drawLine(this.startX, this.startY + (this.scale_y_px * f), this.xLength, this.startY + (f * this.scale_y_px), this.thickLine);
            } else {
                float f2 = (float) i;
                this.canvas.drawLine(this.startX, this.startY + (this.scale_y_px * f2), this.xLength, this.startY + (f2 * this.scale_y_px), this.thinLine);
            }
        }
        this.canvas.drawLine(this.startX, this.startY, (float) getWidth(), this.startY, this.thickLine);
        this.canvas.drawLine(this.startX, this.startY, this.startX, this.stopY, this.thickLine);
        for (int i2 = 1; i2 < this.num_x_line; i2++) {
            double d = (double) i2;
            if (((((double) this.startX) + ((this.per_cm_of_px * 2.0d) * d)) - ((double) this.x3)) + ((double) this.x4) > ((double) this.startX)) {
                if (!this.isHistory) {
                    Canvas canvas4 = this.canvas;
                    canvas4.drawText((this.minuteScale * i2) + "min", (((float) (((double) this.startX) + ((this.per_cm_of_px * 2.0d) * d))) - this.x3) + this.x4, (float) getHeight(), this.minuteText);
                } else {
                    Canvas canvas5 = this.canvas;
                    canvas5.drawText(((this.minuteScale * i2) - 1) + "min", (((float) (((double) this.startX) + ((this.per_cm_of_px * 2.0d) * d))) - this.x3) + this.x4, (float) getHeight(), this.minuteText);
                }
            }
            if (((((double) this.startX) + (this.per_cm_of_px * d)) - ((double) this.x3)) + ((double) this.x4) > ((double) this.startX)) {
                this.canvas.drawLine((((float) (((double) this.startX) + (this.per_cm_of_px * d))) - this.x3) + this.x4, this.startY, (((float) (((double) this.startX) + (this.per_cm_of_px * d))) - this.x3) + this.x4, this.stopY, this.thinLine);
                this.canvas.drawLine((((float) (((double) this.startX) + ((this.per_cm_of_px * 2.0d) * d))) - this.x3) + this.x4, this.startY, (((float) (((double) this.startX) + ((this.per_cm_of_px * 2.0d) * d))) - this.x3) + this.x4, this.stopY, this.thickLine);
            }
        }
        this.canvas.drawRect(this.startX, this.startY + (this.scale_y_px * 8.0f), this.xLength, this.startY + (this.scale_y_px * 13.0f), this.safeFHRPain);
        drawFHRLine();
        if (this.isHistory) {
            this.canvas.drawLine((float) (((double) this.startX) + (this.per_cm_of_px * 2.0d)), this.startY, (float) (((double) this.startX) + (this.per_cm_of_px * 2.0d)), this.stopY, this.baseLine);
        }
    }

    private void drawFHRLine() {
        if (this.pointList.size() > 0) {
            for (int i = 1; i < this.pointList.size(); i++) {
                FHRPoint fHRPoint = this.pointList.get(i);
                int i2 = i - 1;
                FHRPoint fHRPoint2 = this.pointList.get(i2);
                if (fHRPoint == null) {
                    continue;
                }
                if (fHRPoint2 == null) {
                    continue;
                }
                if ((fHRPoint2.getX() - this.x3) + this.x4 > this.startX && (fHRPoint.getX() - this.x3) + this.x4 > this.startX && Math.abs(this.fhrDataList.get(i).getFhr() - this.fhrDataList.get(i2).getFhr()) < 30) {
                    this.canvas.drawLine((fHRPoint2.getX() - this.x3) + this.x4, fHRPoint2.getY(), (fHRPoint.getX() - this.x3) + this.x4, fHRPoint.getY(), this.fhrLine);
                }
                if (fHRPoint.isQuickening() && (getFHRTo_XPx(31, i2) - this.x3) + this.x4 > this.startX && (getFHRTo_XPx(31, i2) - this.x3) + this.x4 + Util.dp2px(4) > this.startX) {
                    this.canvas.drawRect(this.x4 + (getFHRTo_XPx(31, i2) - this.x3), this.stopY - Util.dp2px(8), Util.dp2px(4) + (getFHRTo_XPx(31, i2) - this.x3) + this.x4, this.stopY, this.quickeningPaint);
                }
                if (fHRPoint.getBreakType() == 1 && (getFHRTo_XPx(31, i2) - this.x3) + this.x4 > this.startX && (getFHRTo_XPx(31, i2) - this.x3) + this.x4 + Util.dp2px(4) > this.startX) {
                    this.canvas.drawLine((getFHRTo_XPx(31, i2) - this.x3) + this.x4, this.startY + Util.dp2px(13), (getFHRTo_XPx(31, i2) - this.x3) + this.x4, this.stopY, this.startPaint);
                    this.canvas.drawText(START_FLAG_TEXT, (getFHRTo_XPx(31, i2) - this.x3) + this.x4, this.startY + Util.dp2px(11), this.startPaint);
                }
            }
        }
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        this.pause = false;
        if (!this.canTouch) {
            return false;
        }
        switch (motionEvent.getAction()) {
            case 0:
                this.x1 = (motionEvent.getX() + this.x3) - this.x4;
                if (this.onMonitorTouchListener == null) {
                    return true;
                }
                this.onMonitorTouchListener.isTouchDown();
                return true;
            case 1:
                this.touchUpTime = System.currentTimeMillis();
                if (this.x3 - this.x4 < 0.0f) {
                    initial();
                }
                if (this.onMonitorTouchListener == null) {
                    return true;
                }
                this.onMonitorTouchListener.isTouchUp();
                return true;
            case 2:
                this.x2 = motionEvent.getX();
                if (this.x1 > this.x2) {
                    if (this.pointList.size() <= 0 || ((double) (this.x1 - this.x2)) >= (((double) this.pointList.size()) * this.per_cm_of_px) / 60.0d || ((double) this.x1) >= (((((double) this.pointList.size()) * this.per_cm_of_px) / 60.0d) + ((double) getWidth())) - ((double) this.startX)) {
                        return true;
                    }
                    this.x3 = this.x1 - this.x2;
                    return true;
                } else if (this.x3 - this.x4 <= 0.0f) {
                    return true;
                } else {
                    this.x4 = this.x2 - this.x1;
                    return true;
                }
            default:
                return true;
        }
    }

    public void initial() {
        this.pause = false;
        this.x4 = 0.0f;
        this.x3 = 0.0f;
        this.x2 = 0.0f;
        this.x1 = 0.0f;
    }

    public void setQuickening(boolean z) {
        this.isQuickening = z;
    }

    public void setBreakType(int i) {
        if (this.pointList.size() > 0) {
//            FHRData fHRData = new FHRData(-2, false);
//            for (int i2 = 0; i2 < 60; i2++) {
//                this.fhrDataList.add(this.fhrDataList.size() - 1, fHRData);
//                this.pointList.add(getFHRIndex(), new FHRPoint(getFHRTo_XPx(fHRData.getFhr(), getFHRIndex()), getFHRTo_YPx(fHRData.getFhr()), fHRData.isQuickening()));
//            }
            this.pointList.get(this.pointList.size() - 1).setBreakType(i);
            moveToNow();
        }
    }

    public void setHistoryMode(boolean z) {
        this.isHistoryMode = z;
    }

    public void setConnect(boolean z) {
        this.isConnect = z;
    }

    public void addFHR(int i) {
        this.fhrBuff.addFirst(Integer.valueOf(i));
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private float getFHRTo_YPx(int i) {
        return (((float) getHeight()) - this.min_scale_height) - ((((float) (i - this.minFHRScale)) * this.scale_y_px) / 10.0f);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private float getFHRTo_XPx(int i, int i2) {
        if (i <= 30) {
            return 0.0f;
        }
        if (this.isHistory) {
            return (float) (((double) this.startX) + (this.per_cm_of_px * 2.0d) + ((((double) i2) * this.per_cm_of_px) / 60.0d));
        }
        return (float) (((double) this.startX) + ((((double) i2) * this.per_cm_of_px) / 60.0d));
    }

    private class FHRThread extends Thread {
        int fhr;

        private FHRThread() {
        }

        public void run() {
            try {
                sleep(25);
                FHRMonitorView.this.fhrBuff.addFirst(0);
                while (FHRMonitorView.this.isRunning) {
                    long currentTimeMillis = System.currentTimeMillis();
                    if (FHRMonitorView.this.isConnect) {
                        this.fhr = ((Integer) FHRMonitorView.this.fhrBuff.getFirst()).intValue();
                        float fHRTo_YPx = FHRMonitorView.this.getFHRTo_YPx(this.fhr);
                        float fHRTo_XPx = FHRMonitorView.this.getFHRTo_XPx(this.fhr, FHRMonitorView.this.getFHRIndex());
                        FHRMonitorView.this.fhrDataList.add(new FHRData(this.fhr, FHRMonitorView.this.isQuickening));
                        FHRMonitorView.this.pointList.add(new FHRPoint(fHRTo_XPx, fHRTo_YPx, FHRMonitorView.this.isQuickening));
                        if (FHRMonitorView.this.fhrListener != null) {
                            FHRMonitorView.this.post(new Runnable() {
                                /* class com.laijiayiliao.myapplication.ui.monitorView.FHRMonitorView.FHRThread.AnonymousClass1 */

                                public void run() {
                                    FHRMonitorView.this.fhrListener.getFHR(FHRMonitorView.this.getFHRIndex(), FHRThread.this.fhr);
                                }
                            });
                        }
                        FHRMonitorView.this.setQuickening(false);
                        FHRMonitorView.this.fhrBuff.clear();
                        FHRMonitorView.this.fhrBuff.addFirst(-1);
                        if (FHRMonitorView.this.touchUpTime <= 0) {
                            if (FHRMonitorView.this.getFHRIndex() == 360) {
                                FHRMonitorView.this.x3 = (float) (((double) FHRMonitorView.this.x3) + (FHRMonitorView.this.per_cm_of_px * 4.0d));
                            }
                            if (FHRMonitorView.this.getFHRIndex() > 360 && (FHRMonitorView.this.getFHRIndex() + 360) % 240 == 0) {
                                FHRMonitorView.this.x3 = (float) (((double) FHRMonitorView.this.x3) + (FHRMonitorView.this.per_cm_of_px * 4.0d));
                            }
                        } else if (System.currentTimeMillis() - FHRMonitorView.this.touchUpTime > 3000) {
                            FHRMonitorView.this.moveToNow();
                            FHRMonitorView.this.touchUpTime = 0;
                        }
                    }
                    if (FHRMonitorView.this.isHistoryMode) {
                        FHRMonitorView.this.x3 += (float) (FHRMonitorView.this.per_cm_of_px / 60.0d);
                    }
                    long currentTimeMillis2 = System.currentTimeMillis() - currentTimeMillis;
                    if (currentTimeMillis2 < DATA_TIME) {
                        sleep(DATA_TIME - currentTimeMillis2);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void moveToNow() {
        initial();
        if (getFHRIndex() > 360) {
            int fHRIndex = ((getFHRIndex() + 360) / 240) - 2;
            Log.d(TAG, "run: " + fHRIndex);
            this.x3 = (float) (((double) this.x3) + (this.per_cm_of_px * 4.0d * ((double) fHRIndex)));
        }
    }

    public void setCanTouch(boolean z) {
        this.canTouch = z;
    }

    public void startHistoryMode() {
        this.isHistory = true;
        new HistoryThread(this).start();
    }

    public void closeHistoryMode() {
        this.isHistory = false;
    }


    public static class HistoryThread extends Thread {
        final FHRMonitorView fhrMonitorView;

        public static class FHRMonitorView$HistoryThread$1 implements Runnable {
            final /* synthetic */ HistoryThread historyThread;
            final /* synthetic */ int val$fhr;
            final /* synthetic */ int val$fhr_index;

            FHRMonitorView$HistoryThread$1(HistoryThread historyThread, int i, int i2) {
                this.historyThread = historyThread;
                this.val$fhr_index = i;
                this.val$fhr = i2;
            }

            public void run() {
                historyThread.fhrMonitorView.fhrListener.getFHR(this.val$fhr_index, this.val$fhr);
            }
        }

        private HistoryThread(FHRMonitorView fHRMonitorView) {
            this.fhrMonitorView = fHRMonitorView;
        }

        public void run() {
            int round;
            try {
                sleep(25);
                while (fhrMonitorView.isHistory) {
                    sleep(50);
                    if (!fhrMonitorView.pause) {
                        if (fhrMonitorView.fhrListener != null && (round = (int) Math.round(((double) (fhrMonitorView.x3 - fhrMonitorView.x4)) / (fhrMonitorView.per_cm_of_px / 60.0d))) > 0 && round <= fhrMonitorView.fhrDataList.size()) {
                            this.fhrMonitorView.post(new FHRMonitorView$HistoryThread$1(this, round, ((FHRData) fhrMonitorView.fhrDataList.get(round - 1)).getFhr()));
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Log.e("HistoryThread", new Object[0].toString());
        }

    }

    public void setIndex(int i) {
        this.x4 = 0.0f;
        this.x3 = (float) (((double) i) * (this.per_cm_of_px / 60.0d));
        this.pause = false;
    }
}