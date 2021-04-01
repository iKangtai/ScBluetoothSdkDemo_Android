package com.example.blesdkdemo.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Build;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.blesdkdemo.R;


/**
 * desc
 *
 * @author xiongyl 2021/1/25 22:58
 */
public class BleAlertDialog extends BaseShecareDialog {
    private Context context;
    private LinearLayout lLayout_bg, ll_log_main;
    private TextView txt_title;
    private TextView txt_msg, txt_msg_middle;
    private Button btn_neg;
    private Button btn_pos;
    private ImageView img_line;
    private Display display;
    private ImageView img_close;
    private boolean showTitle = false;
    private boolean showMsg = false;
    private boolean showMsgMiddle = false;
    private boolean showPosBtn = false;
    private boolean showNegBtn = false;
    private boolean showClose = false;
    private View contentView;

    public BleAlertDialog(Context context) {
        this.context = context;
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        display = windowManager.getDefaultDisplay();
    }

    public BleAlertDialog builder() {
        // 获取Dialog布局
        View view = LayoutInflater.from(context).inflate(
                R.layout.view_ble_alertdialog, null);
        // 获取自定义Dialog布局中的控件
        lLayout_bg = view.findViewById(R.id.lLayout_bg);
        ll_log_main = view.findViewById(R.id.ll_log_main);
        ll_log_main.setVisibility(View.GONE);

        txt_title = view.findViewById(R.id.txt_title);
        txt_title.setVisibility(View.GONE);
        txt_msg = view.findViewById(R.id.txt_msg);
        txt_msg.setVisibility(View.GONE);
        txt_msg_middle = view.findViewById(R.id.txt_msg_middle);
        txt_msg_middle.setVisibility(View.GONE);
        btn_neg = view.findViewById(R.id.btn_neg);
        btn_neg.setVisibility(View.GONE);
        btn_pos = view.findViewById(R.id.btn_pos);
        btn_pos.setVisibility(View.GONE);
        img_line = view.findViewById(R.id.img_line);
        img_line.setVisibility(View.GONE);

        img_close = view.findViewById(R.id.iv_dialog_close);

        contentView = view;
        // 定义Dialog布局和参数
        dialog = new Dialog(context, R.style.BleAlertDialogStyle);
        dialog.setContentView(contentView);

        // 调整dialog背景大小
        lLayout_bg.setLayoutParams(new FrameLayout.LayoutParams((int) (display
                .getWidth() * 0.85), LinearLayout.LayoutParams.WRAP_CONTENT));

        return this;
    }

    public BleAlertDialog setTitle(String title) {
        showTitle = true;
        if ("".equals(title)) {
            txt_title.setText("标题");
        } else {
            txt_title.setText(title);
        }
        return this;
    }

    public BleAlertDialog setMsg(String msg) {

        showMsg = true;
        if ("".equals(msg)) {
            txt_msg.setText("内容");
        } else {
            txt_msg.setText(msg);
        }
        return this;
    }

    public BleAlertDialog setMsg(CharSequence msg, int gravity) {

        showMsg = true;
        if ("".equals(msg)) {
            txt_msg.setText("内容");
        } else {
            txt_msg.setText(msg);
        }
        txt_msg.setGravity(gravity);
        return this;
    }

    public BleAlertDialog setMsgMiddle(String msg) {
        showMsgMiddle = true;
        if ("".equals(msg)) {
            txt_msg_middle.setText("内容");
        } else {
            txt_msg_middle.setText(msg);
        }
        return this;
    }

    public BleAlertDialog setCancelable(boolean cancel) {
        dialog.setCancelable(cancel);
        return this;
    }

    public BleAlertDialog setCanceledOnTouchOutside(boolean cancel) {
        dialog.setCanceledOnTouchOutside(cancel);
        return this;
    }

    public BleAlertDialog showCloseButton() {
        showClose = true;
        img_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        return this;
    }

    public BleAlertDialog setPositiveButton(String text,
                                            final View.OnClickListener listener) {
        showPosBtn = true;
        if ("".equals(text)) {
            btn_pos.setText(context.getString(R.string.ok));
        } else {
            btn_pos.setText(text);
        }
        btn_pos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(v);
                dialog.dismiss();
            }
        });
        return this;
    }

    public BleAlertDialog setPositiveButton(String text, int color,
                                            final View.OnClickListener listener) {
        showPosBtn = true;
        if ("".equals(text)) {
            btn_pos.setText(context.getString(R.string.ok));
        } else {
            btn_pos.setText(text);
        }
        btn_pos.setTextColor(color);
        btn_pos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(v);
                dialog.dismiss();
            }
        });
        return this;
    }

    public BleAlertDialog setNegativeButton(String text,
                                            final View.OnClickListener listener) {
        showNegBtn = true;
        if ("".equals(text)) {
            btn_neg.setText(context.getString(R.string.cancel));
        } else {
            btn_neg.setText(text);
        }
        btn_neg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(v);
                dialog.dismiss();
            }
        });
        return this;
    }

    public BleAlertDialog setNegativeButton(String text, int color,
                                            final View.OnClickListener listener) {
        showNegBtn = true;
        if ("".equals(text)) {
            btn_neg.setText(context.getString(R.string.cancel));
        } else {
            btn_neg.setText(text);
        }
        btn_neg.setTextColor(color);
        btn_neg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(v);
                dialog.dismiss();
            }
        });
        return this;
    }

    private void setLayout() {
        if (!showTitle && !showMsg) {
            txt_title.setText(context.getString(R.string.tips));
            txt_title.setVisibility(View.VISIBLE);
        }

        if (showTitle) {
            txt_title.setVisibility(View.VISIBLE);
        }

        if (showMsg) {
            txt_msg.setVisibility(View.VISIBLE);
            txt_msg_middle.setVisibility(View.GONE);
        }

        if (showMsgMiddle) {
            txt_msg_middle.setVisibility(View.VISIBLE);
            txt_msg.setVisibility(View.GONE);
        }

        if (!showPosBtn && !showNegBtn) {
            btn_pos.setText(context.getString(R.string.ok));
            btn_pos.setVisibility(View.VISIBLE);
            btn_pos.setBackgroundResource(R.drawable.alertdialog_single_selector);
            btn_pos.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        if (showPosBtn && showNegBtn) {
            btn_pos.setVisibility(View.VISIBLE);
            btn_pos.setBackgroundResource(R.drawable.alertdialog_right_selector);
            btn_neg.setVisibility(View.VISIBLE);
            btn_neg.setBackgroundResource(R.drawable.alertdialog_left_selector);
            img_line.setVisibility(View.VISIBLE);
        }

        if (showPosBtn && !showNegBtn) {
            btn_pos.setVisibility(View.VISIBLE);
            btn_pos.setBackgroundResource(R.drawable.alertdialog_single_selector);
        }

        if (!showPosBtn && showNegBtn) {
            btn_neg.setVisibility(View.VISIBLE);
            btn_neg.setBackgroundResource(R.drawable.alertdialog_single_selector);
        }

        ll_log_main.setVisibility(View.VISIBLE);
        if (showClose) {
            img_close.setVisibility(View.VISIBLE);
        } else {
            img_close.setVisibility(View.GONE);
        }
    }

    public BleAlertDialog withOverLay() {
        if (Build.VERSION.SDK_INT >= 26) {
            dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY - 1);
        }
        return this;
    }

    public BleAlertDialog show() {
        setLayout();
        try {
            dialog.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return this;
    }
}
