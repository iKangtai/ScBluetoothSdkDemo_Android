package com.example.blesdkdemo.txy;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blesdkdemo.R;
import com.example.blesdkdemo.txy.ui.OnFhrListener;
import com.example.blesdkdemo.util.Util;
import com.ikangtai.bluetoothsdk.BleCommand;
import com.ikangtai.bluetoothsdk.ScPeripheralManager;
import com.ikangtai.bluetoothsdk.audio.SoundCard;
import com.ikangtai.bluetoothsdk.listener.ReceiveDataListenerAdapter;
import com.ikangtai.bluetoothsdk.model.ScPeripheral;
import com.ikangtai.bluetoothsdk.model.ScPeripheralData;
import com.ikangtai.bluetoothsdk.util.BleCode;
import com.ikangtai.bluetoothsdk.util.LogUtils;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Fetal Heart Monitoring
 *
 * @author xiongyl 2020/9/24 0:55
 */
public class MonitorBLEFragment extends BaseMonitorFragment implements View.OnClickListener, OnFhrListener {
    private static final String TAG = "MonitorBLEFragment_TAG";
    private SoundCard mSoundCard;
    private ScPeripheralManager scPeripheralManager;
    private ScPeripheral scPeripheral;
    private String macAddress;
    private ProgressDialog dialog;

    private ReceiveDataListenerAdapter receiveDataListenerAdapter = new ReceiveDataListenerAdapter() {

        @Override
        public void onReceiveData(String macAddress, List<ScPeripheralData> scPeripheralDataList) {

        }

        @Override
        public void onReceiveError(String macAddress, int code, String msg) {
            /**
             * The code see {@link BleCode}
             */
            LogUtils.d("onReceiveError:" + code + "  " + msg);
            checkConnectDialog();
            showErrorMessage(BleCode.getMessage(code));
        }

        @Override
        public void onConnectionStateChange(String macAddress, int state) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                Toast.makeText(MonitorBLEFragment.this.mContext, MonitorBLEFragment.this.getResources().getString(R.string.connected), Toast.LENGTH_SHORT).show();
                mainActivity.setConnectIcon(R.drawable.ble_connect);
                MonitorBLEFragment.this.isConnected = true;
                Log.e(MonitorBLEFragment.TAG, "onConnectionStateChange:  connected");
                MonitorBLEFragment.this.mainActivity.setWarning(false, "");
                MonitorBLEFragment.this.monitorView.setConnect(true);
                MonitorBLEFragment.this.mSoundCard.setPause(false);
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                Toast.makeText(MonitorBLEFragment.this.mContext, MonitorBLEFragment.this.getResources().getString(R.string.disconnected), Toast.LENGTH_SHORT).show();
                if (BaseMonitorFragment.isRecording) {
                    MonitorBLEFragment.this.mSoundCard.stopRecord();
                    MonitorBLEFragment.this.recordEnd(MonitorBLEFragment.this.getResources().getString(R.string.recordEnd_if_disconnected));
                }
                mainActivity.setConnectIcon(R.drawable.ble_disconnect);
                MonitorBLEFragment.this.isConnected = false;
                MonitorBLEFragment.this.monitorView.setBreakType(1);
                MonitorBLEFragment.this.mFHR = 0;
                MonitorBLEFragment.this.heartIconFlag = 0;
                MonitorBLEFragment.this.tv_FHR.setText("---");
                Log.e(MonitorBLEFragment.TAG, "onConnectionStateChange:  disconnect");
                checkConnectDialog();
            }
            MonitorBLEFragment.this.monitorView.setConnect(MonitorBLEFragment.this.isConnected);
            MonitorBLEFragment.this.monitorView.setPause(!MonitorBLEFragment.this.isConnected);
            MonitorBLEFragment.this.mSoundCard.setPause(!MonitorBLEFragment.this.isConnected);
        }

        @Override
        public void onReceiveCommandData(String macAddress, int type, int resultCode, byte[] value) {
            super.onReceiveCommandData(macAddress, type, resultCode, value);
            /**
             * The type see {@link BleCommand}
             */
            if (dialog != null) {
                dialog.dismiss();
                dialog = null;
            }
            if (type == BleCommand.GET_DEVICE_DATA) {
                byte[] data = value;
                if (MonitorBLEFragment.this.isRunning) {
                    Message obtain = Message.obtain();
                    obtain.what = 1;
                    obtain.arg1 = data.length;
                    obtain.obj = data;
                    int[] result = mSoundCard.handleData(data);
                    int frameValue = result[0];
                    int heartIconFlag = result[1];
                    if (frameValue != -1) {
                        mFHR = frameValue;
                        monitorView.addFHR(frameValue);
                    }
                    if (heartIconFlag == 1) {
                        MonitorBLEFragment.this.heartIconFlag = 1;
                    }
                }
            }
        }
    };

    private void showErrorMessage(String errorMessage) {
        new AlertDialog.Builder(getContext())
                .setTitle(getString(R.string.tips))
                .setIcon(R.mipmap.ic_launcher_round)
                .setMessage(errorMessage)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    private void checkConnectDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
            getActivity().finish();
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle bundle) {
        super.onActivityCreated(bundle);
        scPeripheral = (ScPeripheral) getActivity().getIntent().getSerializableExtra("connectDevice");
        macAddress = scPeripheral.getMacAddress();
        scPeripheralManager = ScPeripheralManager.getInstance();
        mainActivity.setConnectIcon(R.drawable.ble_disconnect);
        initAudio();
        new IconFlashTH().start();
        setListeners();
        showConnectDevice();
    }

    private void showConnectDevice() {
        if (TextUtils.isEmpty(macAddress)) {
            return;
        }
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Connecting, please wait...");
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                getActivity().finish();
            }
        });
        dialog.show();
        //Connect a Bluetooth device
        scPeripheralManager.connectPeripheral(macAddress, receiveDataListenerAdapter);
    }


    @Override
    public void onPause() {
        this.mSoundCard.setPause(true);
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isRecording) {
            this.mSoundCard.stopRecord();
            recordEnd(getResources().getString(R.string.recordEnd_if_leave));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPermission();
        this.mSoundCard.setPause(!this.isConnected);
        this.monitorView.setFhrListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.monitorView.setConnect(false);
        this.mSoundCard.close();
        isRecording = false;
        Log.e(TAG, "close fragment！！");
        if (scPeripheralManager != null) {
            scPeripheralManager.stopScan();
            scPeripheralManager.disconnectPeripheral();
        }
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override
    public void getFHR(int fhrIndex, int fhrValue) {
        if (isRecording && fhrValue == -1) {
            disConnect();
            Log.e(TAG, "ble_disConnect: ");
        }
        if (fhrValue >= 60) {
            TextView textView = this.tv_FHR;
            textView.setText("" + fhrValue);
            this.tv_FHR.setBackground(null);
        } else {
            this.tv_FHR.setText("---");
            this.mFHR = 0;
            this.heartIconFlag = 0;
        }

    }

    private void checkPermission() {
        this.needPermission = false;
        if (ContextCompat.checkSelfPermission(this.mContext, "android.permission.ACCESS_COARSE_LOCATION") != 0) {
            this.needPermission = true;
            Toast.makeText(this.mContext, getResources().getString(R.string.phone_no_ble), Toast.LENGTH_LONG).show();
        }
    }

    private void initAudio() {
        this.mSoundCard = new SoundCard(SoundCard.SAMPLING, SoundCard.ORIGINAL_SAMPLING);
    }


    private void disConnect() {
        Log.e(TAG, "diConnect.....");
    }

    private void setListeners() {
        this.btn_quickening.setOnClickListener(this);
        this.btn_switch.setOnClickListener(this);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_quickening:
                if (this.needPermission) {
                    Toast.makeText(this.mContext, getResources().getString(R.string.no_ble_permission), Toast.LENGTH_LONG).show();
                } else if (!this.isConnected) {
                    Toast.makeText(this.mContext, getResources().getString(R.string.ununited), Toast.LENGTH_SHORT).show();
                } else {
                    setQuickening();
                }
                break;
            case R.id.btn_switch:
                if (this.needPermission) {
                    Toast.makeText(this.mContext, getResources().getString(R.string.no_ble_permission), Toast.LENGTH_LONG).show();
                } else if (!this.isConnected) {
                    Toast.makeText(this.mContext, getResources().getString(R.string.ununited), Toast.LENGTH_SHORT).show();
                } else if (Util.getFreeMB() < 60) {
                    Toast.makeText(this.mContext, getResources().getString(R.string.free_space_insufficient), Toast.LENGTH_SHORT).show();
                } else if (!isRecording) {
                    recordStart();
                    this.mSoundCard.startRecord(this.audio_path_name);
                } else {
                    this.mSoundCard.stopRecord();
                    recordEnd(getResources().getString(R.string.recordEnd_if_end));
                }
                break;
        }
    }
}