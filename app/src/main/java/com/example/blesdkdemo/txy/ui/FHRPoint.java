package com.example.blesdkdemo.txy.ui;

/**
 * desc
 *
 * @author xiongyl 2020/11/5 14:21
 */
public class FHRPoint {
    public static final int BREAK_TYPE_CONNECTED = 0;
    public static final int BREAK_TYPE_DISCONNECTED = 1;
    private int breakType;
    private boolean quickening;
    private float x;
    private float y;

    public FHRPoint(float f, float f2, boolean z) {
        this.x = f;
        this.y = f2;
        this.quickening = z;
        this.breakType = 0;
    }

    public FHRPoint(float f, float f2, boolean z, int i) {
        this.x = f;
        this.y = f2;
        this.quickening = z;
        this.breakType = i;
    }

    public int getBreakType() {
        return this.breakType;
    }

    public void setBreakType(int i) {
        this.breakType = i;
    }

    public boolean isQuickening() {
        return this.quickening;
    }

    public void setQuickening(boolean z) {
        this.quickening = z;
    }

    public float getX() {
        return this.x;
    }

    public void setX(float f) {
        this.x = f;
    }

    public float getY() {
        return this.y;
    }

    public void setY(float f) {
        this.y = f;
    }

    public String toString() {
        return "FHRPoint{x=" + this.x + ", y=" + this.y + ", quickening=" + this.quickening + ", breakType=" + this.breakType + '}';
    }
}