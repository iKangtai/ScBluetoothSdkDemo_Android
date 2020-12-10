package com.example.blesdkdemo;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

public class BaseFragment extends Fragment {
    /* access modifiers changed from: protected */
    public Activity mActivity;
    /* access modifiers changed from: protected */
    public Context mContext;
    protected View mView;
    protected Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    public View onCreateView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
        return this.mView;
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.mContext = context;
        this.mActivity = getActivity();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    /* access modifiers changed from: protected */
    public void showDialog(String str) {
        new AlertDialog.Builder(this.mContext).setCancelable(true).setMessage((CharSequence) str).setPositiveButton((CharSequence) getResources().getString(R.string.confirm), (DialogInterface.OnClickListener) null).show();
    }
}
