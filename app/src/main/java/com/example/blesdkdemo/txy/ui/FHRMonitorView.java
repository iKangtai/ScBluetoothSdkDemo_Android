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

import androidx.annotation.NonNull;

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
    private float downX;
    private float moveX;
    private float leftOffsetX;
    private float rightOffsetX;
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

    public long getRecordingTime(int index) {
        if (this.fhrDataList.size() > 0) {
            return (long) (((this.fhrDataList.size() - 1) - index) * DATA_TIME);
        }
        return 0;
    }

    public int getFHRIndex() {
        return this.pointList.size() - 1;
    }

    public ArrayList<FHRData> getSaveFHR(int fhrStartIndex, int fhrEndIndex) {
        ArrayList<FHRData> arrayList = new ArrayList<>();
        if (fhrStartIndex < 0) {
            fhrStartIndex = 0;
        }
        if (this.fhrDataList.size() > 1 && fhrStartIndex <= fhrEndIndex) {
            while (fhrStartIndex <= fhrEndIndex) {
                FHRData fHRData = this.fhrDataList.get(fhrStartIndex);
                if (fHRData.getFhr() == -2) {
                    break;
                }
                arrayList.add(fHRData);
                fhrStartIndex++;
            }
        }
        return arrayList;
    }

    public FhrAllData getSaveAllFHR(int fhrStartIndex, int fhrEndIndex) {
        FhrAllData fhrData = new FhrAllData();
        ArrayList<Integer> qn = new ArrayList<>();
        ArrayList<Integer> v = new ArrayList<>();
        if (fhrStartIndex < 0) {
            fhrStartIndex = 0;
        }
        if (this.fhrDataList.size() > 1 && fhrStartIndex <= fhrEndIndex) {
            while (fhrStartIndex <= fhrEndIndex) {
                FHRData fHRData = this.fhrDataList.get(fhrStartIndex);
                if (fHRData.getFhr() == -2) {
                    break;
                }
                v.add(fHRData.getFhr() < 0 ? 0 : fHRData.getFhr());
                if (fHRData.isQuickening()) {
                    qn.add(v.size() - 1);
                }
                fhrStartIndex++;
            }
        }
        fhrData.setQn(qn);
        fhrData.setV(v);
        return fhrData;
    }

    public FHRMonitorView(Context context) {
        super(context);
        initView();
    }

    public FHRMonitorView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initView();
    }

    public FHRMonitorView(Context context, AttributeSet attributeSet, int defStyle) {
        super(context, attributeSet, defStyle);
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
        this.startPaint.setTextSize(Util.dp2px(FHR_TEXT_SIZE));
        this.quickeningPaint = FHRPaint.getQuickeningPaint();
        this.fhrLine = FHRPaint.getFHRLine();
        this.safeFHRPain = FHRPaint.getSafeFHRPaint();
        this.fhrText = FHRPaint.getFHRText();
        this.fhrText.setTextSize(Util.dp2px(14));
        this.minuteText = FHRPaint.getMinuteText();
        this.minuteText.setTextSize(Util.dp2px(16));
        this.startX = Util.dp2px(42);
        this.startY = (Util.dp2px(FHR_TEXT_SIZE) / 2.0f) + Util.dp2px(9);
        this.min_scale_height = Util.dp2px(16);
        setFocusableInTouchMode(true);
        setKeepScreenOn(true);
    }

    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        this.isRunning = true;
        this.monitorThread.execute(this.monitorTH);
        new FHRThread().start();
    }

    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width,
                               int height) {
        this.scale_y_px = ((((float) height) - this.min_scale_height) - this.startY) / ((float) this.num_FHRScaleLine);
        this.per_cm_of_px = (double) ((width * 106) / 720);
        this.num_x_line = (int) (((double) getWidth()) / this.per_cm_of_px);
        this.xLength = (float) getWidth();
        this.stopY = this.startY + (this.scale_y_px * ((float) this.num_FHRScaleLine));
        long uptimeMillis = SystemClock.uptimeMillis();
        boolean onTouchEvent = onTouchEvent(MotionEvent.obtain(uptimeMillis, uptimeMillis + 100, 2, 0.0f, 0.0f, 1));
    }

    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        closeHistoryMode();
        this.isRunning = false;
    }

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
            if ((i - 3) % 5 == 0) {
                Canvas canvas3 = this.canvas;
                canvas3.drawText("" + (this.maxFHRScale - i * 10), this.startX - this.text_FHR_left_offset, (this.scale_y_px * i) + this.startY + this.text_FHR_down_offset, this.fhrText);
                this.canvas.drawLine(this.startX, this.startY + (this.scale_y_px * i), this.xLength, this.startY + (i * this.scale_y_px), this.thickLine);
            } else {
                float f2 = (float) i;
                this.canvas.drawLine(this.startX, this.startY + (this.scale_y_px * f2), this.xLength, this.startY + (f2 * this.scale_y_px), this.thinLine);
            }
        }
        this.canvas.drawLine(this.startX, this.startY, (float) getWidth(), this.startY, this.thickLine);
        this.canvas.drawLine(this.startX, this.startY, this.startX, this.stopY, this.thickLine);
        for (int i = 1; i < this.num_x_line; i++) {
            if (((((double) this.startX) + ((this.per_cm_of_px * 2.0d) * i)) - ((double) this.leftOffsetX)) + ((double) this.rightOffsetX) > ((double) this.startX)) {
                if (!this.isHistory) {
                    Canvas canvas4 = this.canvas;
                    canvas4.drawText((this.minuteScale * i) + "min", (((float) (((double) this.startX) + ((this.per_cm_of_px * 2.0d) * i))) - this.leftOffsetX) + this.rightOffsetX, (float) getHeight(), this.minuteText);
                } else {
                    Canvas canvas5 = this.canvas;
                    canvas5.drawText(((this.minuteScale * i) - 1) + "min", (((float) (((double) this.startX) + ((this.per_cm_of_px * 2.0d) * i))) - this.leftOffsetX) + this.rightOffsetX, (float) getHeight(), this.minuteText);
                }
            }
            if (((((double) this.startX) + (this.per_cm_of_px * i)) - ((double) this.leftOffsetX)) + ((double) this.rightOffsetX) > ((double) this.startX)) {
                this.canvas.drawLine((((float) (((double) this.startX) + (this.per_cm_of_px * i))) - this.leftOffsetX) + this.rightOffsetX, this.startY, (((float) (((double) this.startX) + (this.per_cm_of_px * i))) - this.leftOffsetX) + this.rightOffsetX, this.stopY, this.thinLine);
                this.canvas.drawLine((((float) (((double) this.startX) + ((this.per_cm_of_px * 2.0d) * i))) - this.leftOffsetX) + this.rightOffsetX, this.startY, (((float) (((double) this.startX) + ((this.per_cm_of_px * 2.0d) * i))) - this.leftOffsetX) + this.rightOffsetX, this.stopY, this.thickLine);
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
                int preIndex = i - 1;
                FHRPoint fHRPoint2 = this.pointList.get(preIndex);
                if (fHRPoint == null) {
                    continue;
                }
                if (fHRPoint2 == null) {
                    continue;
                }
                if ((fHRPoint2.getX() - this.leftOffsetX) + this.rightOffsetX > this.startX && (fHRPoint.getX() - this.leftOffsetX) + this.rightOffsetX > this.startX && Math.abs(this.fhrDataList.get(i).getFhr() - this.fhrDataList.get(preIndex).getFhr()) < 30) {
                    this.canvas.drawLine((fHRPoint2.getX() - this.leftOffsetX) + this.rightOffsetX, fHRPoint2.getY(), (fHRPoint.getX() - this.leftOffsetX) + this.rightOffsetX, fHRPoint.getY(), this.fhrLine);
                }
                if (fHRPoint.isQuickening() && (getFHRTo_XPx(31, preIndex) - this.leftOffsetX) + this.rightOffsetX > this.startX && (getFHRTo_XPx(31, preIndex) - this.leftOffsetX) + this.rightOffsetX + Util.dp2px(4) > this.startX) {
                    this.canvas.drawRect(this.rightOffsetX + (getFHRTo_XPx(31, preIndex) - this.leftOffsetX), this.stopY - Util.dp2px(8), Util.dp2px(4) + (getFHRTo_XPx(31, preIndex) - this.leftOffsetX) + this.rightOffsetX, this.stopY, this.quickeningPaint);
                }
                if (fHRPoint.getBreakType() == 1 && (getFHRTo_XPx(31, preIndex) - this.leftOffsetX) + this.rightOffsetX > this.startX && (getFHRTo_XPx(31, preIndex) - this.leftOffsetX) + this.rightOffsetX + Util.dp2px(4) > this.startX) {
                    this.canvas.drawLine((getFHRTo_XPx(31, preIndex) - this.leftOffsetX) + this.rightOffsetX, this.startY + Util.dp2px(13), (getFHRTo_XPx(31, preIndex) - this.leftOffsetX) + this.rightOffsetX, this.stopY, this.startPaint);
                    this.canvas.drawText(START_FLAG_TEXT, (getFHRTo_XPx(31, preIndex) - this.leftOffsetX) + this.rightOffsetX, this.startY + Util.dp2px(11), this.startPaint);
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
            case MotionEvent.ACTION_DOWN:
                this.downX = (motionEvent.getX() + this.leftOffsetX) - this.rightOffsetX;
                if (this.onMonitorTouchListener != null) {
                    this.onMonitorTouchListener.isTouchDown();
                }
                break;
            case MotionEvent.ACTION_UP:
                this.touchUpTime = System.currentTimeMillis();
                if (this.leftOffsetX - this.rightOffsetX < 0.0f) {
                    initial();
                }
                if (this.onMonitorTouchListener != null) {
                    this.onMonitorTouchListener.isTouchUp();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                this.moveX = motionEvent.getX();
                if (this.downX > this.moveX) {
                    if (this.pointList.size() <= 0 || ((double) (this.downX - this.moveX)) >= (((double) this.pointList.size()) * this.per_cm_of_px) / 60.0d || ((double) this.downX) >= (((((double) this.pointList.size()) * this.per_cm_of_px) / 60.0d) + ((double) getWidth())) - ((double) this.startX)) {

                    } else {
                        this.leftOffsetX = this.downX - this.moveX;
                    }
                }  else {
                    if (this.leftOffsetX - this.rightOffsetX <= 0.0f) {

                    }else {
                        this.rightOffsetX = this.moveX - this.downX;
                    }
                }
                break;
        }
        return true;
    }

    public void initial() {
        this.pause = false;
        this.rightOffsetX = 0.0f;
        this.leftOffsetX = 0.0f;
        this.moveX = 0.0f;
        this.downX = 0.0f;
    }

    public void setQuickening(boolean isQuickening) {
        this.isQuickening = isQuickening;
    }

    public void setBreakType(int type) {
        if (this.pointList.size() > 0) {
            this.pointList.get(this.pointList.size() - 1).setBreakType(type);
            moveToNow();
        }
    }

    public void setHistoryMode(boolean isHistoryMode) {
        this.isHistoryMode = isHistoryMode;
    }

    public void setConnect(boolean isConnect) {
        this.isConnect = isConnect;
    }

    public void addFHR(int fhr) {
        this.fhrBuff.addFirst(Integer.valueOf(fhr));
    }

    private float getFHRTo_YPx(int fhr) {
        return (((float) getHeight()) - this.min_scale_height) - ((((float) (fhr - this.minFHRScale)) * this.scale_y_px) / 10.0f);
    }

    private float getFHRTo_XPx(int fhr, int fhrIndex) {
        if (fhr <= 30) {
            return 0.0f;
        }
        if (this.isHistory) {
            return (float) (((double) this.startX) + (this.per_cm_of_px * 2.0d) + ((((double) fhrIndex) * this.per_cm_of_px) / 60.0d));
        }
        return (float) (((double) this.startX) + ((((double) fhrIndex) * this.per_cm_of_px) / 60.0d));
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
                                FHRMonitorView.this.leftOffsetX = (float) (((double) FHRMonitorView.this.leftOffsetX) + (FHRMonitorView.this.per_cm_of_px * 4.0d));
                            }
                            if (FHRMonitorView.this.getFHRIndex() > 360 && (FHRMonitorView.this.getFHRIndex() + 360) % 240 == 0) {
                                FHRMonitorView.this.leftOffsetX = (float) (((double) FHRMonitorView.this.leftOffsetX) + (FHRMonitorView.this.per_cm_of_px * 4.0d));
                            }
                        } else if (System.currentTimeMillis() - FHRMonitorView.this.touchUpTime > AUTO_MOVE_TIME) {
                            FHRMonitorView.this.moveToNow();
                            FHRMonitorView.this.touchUpTime = 0;
                        }
                    }
                    if (FHRMonitorView.this.isHistoryMode) {
                        FHRMonitorView.this.leftOffsetX += (float) (FHRMonitorView.this.per_cm_of_px / 60.0d);
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

    private void moveToNow() {
        initial();
        if (getFHRIndex() > 360) {
            int fHRIndex = ((getFHRIndex() + 360) / 240) - 2;
            this.leftOffsetX = (float) (((double) this.leftOffsetX) + (this.per_cm_of_px * 4.0d * ((double) fHRIndex)));
        }
    }

    public void setCanTouch(boolean canTouch) {
        this.canTouch = canTouch;
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
            final HistoryThread historyThread;
            final int val$fhr;
            final int val$fhr_index;

            FHRMonitorView$HistoryThread$1(HistoryThread historyThread, int fhr_index, int fhr) {
                this.historyThread = historyThread;
                this.val$fhr_index = fhr_index;
                this.val$fhr = fhr;
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
                        if (fhrMonitorView.fhrListener != null && (round = (int) Math.round(((double) (fhrMonitorView.leftOffsetX - fhrMonitorView.rightOffsetX)) / (fhrMonitorView.per_cm_of_px / 60.0d))) > 0 && round <= fhrMonitorView.fhrDataList.size()) {
                            this.fhrMonitorView.post(new FHRMonitorView$HistoryThread$1(this, round, ((FHRData) fhrMonitorView.fhrDataList.get(round - 1)).getFhr()));
                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public void setIndex(int index) {
        this.rightOffsetX = 0.0f;
        this.leftOffsetX = (float) (((double) index) * (this.per_cm_of_px / 60.0d));
        this.pause = false;
    }
}