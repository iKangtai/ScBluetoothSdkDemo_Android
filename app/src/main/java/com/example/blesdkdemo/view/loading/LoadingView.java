package com.example.blesdkdemo.view.loading;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.example.blesdkdemo.R;


public class LoadingView extends androidx.appcompat.widget.AppCompatImageView {
    private LoadingDrawable mLoadingDrawable;

    public LoadingView(Context context) {
        super(context);
    }

    public LoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        //setLoadingRenderer(new LevelLoadingRenderer.Builder(context).build());
    }

    public void setLoadingRenderer(LoadingRenderer loadingRenderer) {
        mLoadingDrawable = new LoadingDrawable(loadingRenderer);
        setImageDrawable(mLoadingDrawable);
    }

    public void startLoading() {
        stopAnimation();
        if (mLoadingDrawable != null) {
            setImageDrawable(mLoadingDrawable);
        } else {
            setLoadingRenderer(new LevelLoadingRenderer.Builder(getContext()).build());
        }
        startAnimation();
    }

    public void finishLoading() {
        stopAnimation();
        setImageResource(R.drawable.device_binding_page_ic_selected);
    }

    public void initLoading() {
        stopAnimation();
        setImageResource(R.drawable.device_binding_page_ic_unselected);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnimation();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }

    @Override
    protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);

        final boolean visible = visibility == VISIBLE && getVisibility() == VISIBLE;
        if (visible) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    private void startAnimation() {
        if (mLoadingDrawable != null) {
            mLoadingDrawable.start();
        }
    }

    private void stopAnimation() {
        if (mLoadingDrawable != null) {
            mLoadingDrawable.stop();
        }
    }
}
