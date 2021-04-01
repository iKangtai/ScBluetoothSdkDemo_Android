package com.example.blesdkdemo.txy;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blesdkdemo.Constant;
import com.example.blesdkdemo.R;
import com.example.blesdkdemo.activity.ChatActivity;
import com.example.blesdkdemo.util.ChatUrlUtil;
import com.hjq.permissions.Permission;
import com.ikangtai.bluetoothsdk.model.ScPeripheral;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/**
 * Fetal Preference
 *
 * @author xiongyl 2020/9/24 0:55
 */
public class BleActivity extends AppCompatActivity {
    public static final int PERMISSION_REQUEST_CODE = 1000;
    private ImageView iv_connect;
    private LinearLayout layout_error;
    private MonitorBLEFragment mainBLEFragment;
    private TextView tv_error;
    private ImageView iv_back;
    private ImageView iv_service;
    private ScPeripheral scPeripheral;

    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        scPeripheral = (ScPeripheral) getIntent().getSerializableExtra("connectDevice");
        setContentView(R.layout.activity_txy_ble);
        initView();
        requestPermission();
        initViews();
        initFragments();
    }

    private void initView() {
        iv_connect = findViewById(R.id.iv_connect);
        layout_error = findViewById(R.id.layout_error);
        iv_back = findViewById(R.id.iv_back);
        tv_error = findViewById(R.id.tv_error);
        iv_service = findViewById(R.id.iv_service);
        iv_service.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(BleActivity.this, ChatActivity.class);
                intent.putExtra("url", ChatUrlUtil.getChatUrl(Constant.appId, Constant.appSecret, Constant.unionId, 18, 20, 5, scPeripheral.getMacAddress()));
                startActivity(intent);
            }
        });
        iv_back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exitApp();
            }
        });
    }

    public void onBackPressed() {
        exitApp();
    }

    private void exitApp() {
        if (BaseMonitorFragment.isRecording) {
            Toast.makeText(this, getResources().getString(R.string.exit_if_recording), Toast.LENGTH_SHORT).show();
        } else {
            new AlertDialog.Builder(this).setCancelable(false).setMessage((CharSequence) getResources().getString(R.string.exit_app)).setPositiveButton((CharSequence) getResources().getString(R.string.confirm), (DialogInterface.OnClickListener) new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    finish();
                }
            }).setNegativeButton((CharSequence) getResources().getString(R.string.cancel), (DialogInterface.OnClickListener) null).show();
        }
    }

    private void requestPermission() {
        String[] strArr = {Permission.ACCESS_FINE_LOCATION, Permission.RECORD_AUDIO};
        boolean z = false;
        for (int j = 0; j < strArr.length; j++) {
            if (ContextCompat.checkSelfPermission(this, strArr[j]) != 0) {
                z = true;
                break;
            }
        }
        if (z) {
            ActivityCompat.requestPermissions(this, strArr, PERMISSION_REQUEST_CODE);
        }
    }

    private void initViews() {

    }

    public void setWarning(boolean z, @NonNull String str) {
        if (!z) {
            this.layout_error.setVisibility(View.GONE);
            return;
        }
        this.layout_error.setVisibility(View.VISIBLE);
        this.tv_error.setText(str);
    }

    public void setWarningOnClickListener(View.OnClickListener onClickListener) {
        this.layout_error.setOnClickListener(onClickListener);
    }

    public void setConnectIcon(@DrawableRes int i) {
        iv_connect.setImageResource(i);
    }

    private void initFragments() {
        if (this.mainBLEFragment == null || !this.mainBLEFragment.isAdded()) {
            this.mainBLEFragment = new MonitorBLEFragment();
            getSupportFragmentManager().beginTransaction().add((int) R.id.rl_fragment, (Fragment) this.mainBLEFragment).commit();
        }
    }
}