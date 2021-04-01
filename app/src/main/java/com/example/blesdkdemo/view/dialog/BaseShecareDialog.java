package com.example.blesdkdemo.view.dialog;

import android.app.Dialog;

public class BaseShecareDialog {
    protected Dialog dialog;

    public void dissmiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public boolean showing() {
        if (dialog != null && dialog.isShowing()) {
            return true;
        }

        return false;
    }
}
