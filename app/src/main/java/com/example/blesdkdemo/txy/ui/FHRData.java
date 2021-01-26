package com.example.blesdkdemo.txy.ui;

/**
 * desc
 *
 * @author xiongyl 2020/11/5 14:20
 */
public class FHRData {
    private int fhr;
    private boolean quickening;

    public FHRData(int i, boolean z) {
        this.fhr = i;
        this.quickening = z;
    }

    public int getFhr() {
        return this.fhr;
    }

    public void setFhr(int i) {
        this.fhr = i;
    }

    public boolean isQuickening() {
        return this.quickening;
    }

    public void setQuickening(boolean z) {
        this.quickening = z;
    }
}