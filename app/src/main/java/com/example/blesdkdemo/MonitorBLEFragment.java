package com.example.blesdkdemo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothProfile;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blesdkdemo.auido.AudioCompress;
import com.example.blesdkdemo.auido.SoundCard;
import com.example.blesdkdemo.ui.OnFhrListener;
import com.example.blesdkdemo.util.Util;
import com.ikangtai.bluetoothsdk.BleCommand;
import com.ikangtai.bluetoothsdk.ScPeripheralManager;
import com.ikangtai.bluetoothsdk.listener.ReceiveDataListenerAdapter;
import com.ikangtai.bluetoothsdk.model.ScPeripheral;
import com.ikangtai.bluetoothsdk.model.ScPeripheralData;
import com.ikangtai.bluetoothsdk.util.BleCode;
import com.ikangtai.bluetoothsdk.util.BleUtils;
import com.ikangtai.bluetoothsdk.util.LogUtils;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;


public class MonitorBLEFragment extends BaseMonitorFragment implements View.OnClickListener, OnFhrListener {
    public static final int BLE_TIME_OUT = 5000;
    public static final int REQUEST_CODE_BLE_ENABLE = 1;
    private static final String TAG = "MainBLEFragment_TAG";
    private static final int originalSampleFre = 20000;
    private int deviceNum = 0;
    private BluetoothGatt gatt;
    private AudioCompress mAudioCompress = new AudioCompress();
    private byte[] mRevDataBuf = new byte[232];
    private final int mRevDataBufMaxLen = 200;
    private int mRevDataLength = 0;
    private SoundCard mSoundCard;
    private int retryCount = 0;
    private int timeOutCoefficient = 1;
    private ScPeripheralManager scPeripheralManager;
    private ScPeripheral scPeripheral;
    private String macAddress;
    private ProgressDialog dialog;

    static /* synthetic */ int access$1608(MonitorBLEFragment monitorBLEFragment) {
        int i = monitorBLEFragment.mRevDataLength;
        monitorBLEFragment.mRevDataLength = i + 1;
        return i;
    }

    static /* synthetic */ int access$408(MonitorBLEFragment monitorBLEFragment) {
        int i = monitorBLEFragment.deviceNum;
        monitorBLEFragment.deviceNum = i + 1;
        return i;
    }

    static /* synthetic */ int access$608(MonitorBLEFragment monitorBLEFragment) {
        int i = monitorBLEFragment.retryCount;
        monitorBLEFragment.retryCount = i + 1;
        return i;
    }

    static /* synthetic */ int access$808(MonitorBLEFragment monitorBLEFragment) {
        int i = monitorBLEFragment.timeOutCoefficient;
        monitorBLEFragment.timeOutCoefficient = i + 1;
        return i;
    }

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
                Log.e(MonitorBLEFragment.TAG, "onConnectionStateChange:  连接成功");
                MonitorBLEFragment.this.mainActivity.setWarning(false, "");
                MonitorBLEFragment.this.monitorView.setConnect(true);
                MonitorBLEFragment.this.mSoundCard.setPause(false);
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                Toast.makeText(MonitorBLEFragment.this.mContext, MonitorBLEFragment.this.getResources().getString(R.string.disconnected), Toast.LENGTH_SHORT).show();
                if (BaseMonitorFragment.isRecording) {
                    MonitorBLEFragment.this.mSoundCard.stopRecord();
                    MonitorBLEFragment.this.recordEnd(MonitorBLEFragment.this.getResources().getString(R.string.recordEnd_if_disconnected));
                }
                if (MonitorBLEFragment.this.gatt != null) {
                    MonitorBLEFragment.this.gatt.close();
                    MonitorBLEFragment.this.gatt = null;
                }
                mainActivity.setConnectIcon(R.drawable.ble_disconnect);
                MonitorBLEFragment.this.mRevDataLength = 0;
                MonitorBLEFragment.this.mRevDataBuf = new byte[232];
                MonitorBLEFragment.this.isConnected = false;
                MonitorBLEFragment.this.monitorView.setBreakType(1);
                MonitorBLEFragment.this.mFHR = 0;
                MonitorBLEFragment.this.tv_FHR.setText("");
                MonitorBLEFragment.this.tv_FHR.setBackground(MonitorBLEFragment.this.getResources().getDrawable(R.drawable.ic_fhr_text));
                Log.e(MonitorBLEFragment.TAG, "onConnectionStateChange:  断开连接");
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
            LogUtils.d("onReceiveCommandData:" + type + "  " + resultCode + " " + BleUtils.byte2hex(value));
            if (type == BleCommand.GET_DEVICE_DATA) {
                byte[] data = value;
                if (MonitorBLEFragment.this.isRunning) {
                    Message obtain = Message.obtain();
                    obtain.what = 1;
                    obtain.arg1 = data.length;
                    obtain.obj = data;
                    //Log.d("decode_frame", " data==111  " + BleUtils.byte2hex(value));
                    //MonitorBLEFragment.this.bleDataHandle.sendMessage(obtain);
                    MonitorBLEFragment.this.handleData(data);
                }
            }
        }
    };

    private void showErrorMessage(String errorMessage) {
        new AlertDialog.Builder(getContext())
                .setTitle("Tips")
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
    // com.laijiayiliao.myapplication.ui.fragment.BaseMonitorFragment, android.support.v4.app.Fragment
    public void onActivityCreated(@Nullable Bundle bundle) {
        super.onActivityCreated(bundle);
        scPeripheral = (ScPeripheral) getActivity().getIntent().getSerializableExtra("connectDevice");
        macAddress = scPeripheral.getMacAddress();
        scPeripheralManager = ScPeripheralManager.getInstance();
        mainActivity.setConnectIcon(R.drawable.ble_disconnect);
        Log.e(TAG, "onActivityCreated: VERSION  " + Build.VERSION.SDK_INT);
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
        //dialog.setCancelable(false);
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
    // com.laijiayiliao.myapplication.ui.fragment.BaseMonitorFragment, android.support.v4.app.Fragment
    public void onPause() {
        this.mSoundCard.setPause(true);
        super.onPause();
    }

    @Override
    // com.laijiayiliao.myapplication.ui.fragment.BaseMonitorFragment, android.support.v4.app.Fragment
    public void onStop() {
        super.onStop();
        if (isRecording) {
            this.mSoundCard.stopRecord();
            recordEnd(getResources().getString(R.string.recordEnd_if_leave));
        }
    }

    @Override
    // com.laijiayiliao.myapplication.ui.fragment.BaseMonitorFragment, android.support.v4.app.Fragment
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
        if (this.gatt != null) {
            this.gatt.close();
            this.gatt = null;
        }
        Log.e(TAG, "关闭fragment！！");
        if (scPeripheralManager != null) {
            scPeripheralManager.stopScan();
            scPeripheralManager.disconnectPeripheral();
        }
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }

    @Override // com.laijiayiliao.myapplication.ui.monitorView.OnFhrListener
    public void getFHR(int i, int i2) {
        if (isRecording && i2 == -1) {
            disConnect();
            Log.e(TAG, "ble_disConnect: ");
        }
        if (i2 > 30) {
            TextView textView = this.tv_FHR;
            textView.setText("" + i2);
            this.tv_FHR.setBackground(null);
            return;
        }
        this.tv_FHR.setText("");
        this.tv_FHR.setBackground(getResources().getDrawable(R.drawable.ic_fhr_text));
        this.mFHR = 0;
    }

    private void checkPermission() {
        this.needPermission = false;
        if (ContextCompat.checkSelfPermission(this.mContext, "android.permission.ACCESS_COARSE_LOCATION") != 0) {
            this.needPermission = true;
            Toast.makeText(this.mContext, getResources().getString(R.string.phone_no_ble), Toast.LENGTH_LONG).show();
        }
    }

    private void initAudio() {
        this.mSoundCard = new SoundCard(Constant.SAMPLING, Constant.ORIGINAL_SAMPLING);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void bleTimeOut(@NonNull final String str) {
        this.mainHandler.postDelayed(new Runnable() {
            /* class com.laijiayiliao.myapplication.ui.fragment.MonitorBLEFragment.AnonymousClass1 */

            public void run() {
                if (!MonitorBLEFragment.this.isConnected) {
                    MonitorBLEFragment.this.mainActivity.setWarning(true, str);
                    MonitorBLEFragment.this.mainActivity.setWarningOnClickListener(new View.OnClickListener() {
                        /* class com.laijiayiliao.myapplication.ui.fragment.MonitorBLEFragment.AnonymousClass1.AnonymousClass1 */

                        public void onClick(View view) {
                            MonitorBLEFragment.this.mainActivity.setWarningOnClickListener(null);
                            MonitorBLEFragment.this.mainActivity.setWarning(true, MonitorBLEFragment.this.getString(R.string.ble_scan_again));
                            MonitorBLEFragment.access$608(MonitorBLEFragment.this);
                            if (MonitorBLEFragment.this.retryCount > 5) {
                                MonitorBLEFragment.this.showDialog(MonitorBLEFragment.this.getString(R.string.ble_retry));
                            }
                        }
                    });
                    if (MonitorBLEFragment.this.deviceNum > 9) {
                        MonitorBLEFragment.this.mainActivity.setWarning(true, MonitorBLEFragment.this.getString(R.string.ble_device_much));
                        MonitorBLEFragment.access$808(MonitorBLEFragment.this);
                        MonitorBLEFragment.this.deviceNum = 0;
                        return;
                    }
                    return;
                }
                MonitorBLEFragment.this.mainActivity.setWarning(false, "");
                MonitorBLEFragment.this.retryCount = 0;
                MonitorBLEFragment.this.deviceNum = 0;
                MonitorBLEFragment.this.timeOutCoefficient = 1;
            }
        }, (long) (this.timeOutCoefficient * BLE_TIME_OUT));
    }

    private void handleData(byte[] data) {
        String strMsgLog;
        int count = 0;
        int i;
        try {
            int i2 = data.length;
            byte[] bArr = data;
            int i3 = 58;

            short[] sArr = new short[116];
            for (int i4 = 0; i4 < i2; i4++) {
                mRevDataBuf[mRevDataLength] = bArr[i4];
                MonitorBLEFragment.access$1608(this);
                if (mRevDataLength >= 232) {
                    mRevDataLength = 0;
                }
            }
            //Log.d("decode_frame", " data==222  " + BleUtils.byte2hex(bArr));
            //Log.d("decode_frame", " data==333  " + BleUtils.byte2hex(mRevDataBuf));
            if (mRevDataLength >= 58) {
                int i5 = mRevDataLength - 58;
                int i6 = 0;
                int i7 = -1;
                while (true) {
                    byte[] bArr2 = new byte[116];
                    if (i6 > i5) {
                        break;
                    }
                    for (int i8 = 0; i8 < i3; i8++) {
                        bArr2[i8] = mRevDataBuf[i6 + i8];
                    }
                    //Log.d("decode_frame", " data==444  " + BleUtils.byte2hex(bArr2));
                    i7 = mAudioCompress.decode_frame(bArr2, sArr, i3);
                    Log.e("decode_frame", "result== " + i7);
                    strMsgLog = "";
                    int i9 = 0;
                    while (i9 < i3) {
                        strMsgLog += String.format("%02X ", Byte.valueOf(bArr2[i9]));
                        i9++;
                        i3 = 58;
                    }
                    strMsgLog = "";
                    for (int i10 = 0; i10 < 116; i10++) {
                        strMsgLog += String.format("%02X ", Short.valueOf(sArr[i10]));
                    }
                    if (i7 != -1) {
                        break;
                    }
                    i6++;
                    i3 = 58;
                }
                if (i7 != -1) {
                    i = (mRevDataLength - i6) - 58;
                    mFHR = i7;
                    monitorView.addFHR(i7);
                    mSoundCard.WriteData(sArr, 100);
                    //Log.e("decode_frame", "err11: 正确字节数 =" + i + " mRevDataLength" + mRevDataLength + " i6: " + i6);
                } else {
                    i = mRevDataLength - i6;
                    StringBuilder sb = new StringBuilder();
                    sb.append("错误字节数 = ");
                    int i11 = count;
                    count = i11 + 1;
                    sb.append(i11);
                    Log.e("decode_frame", "err11: " + sb.toString());
                }
                for (int i12 = 0; i12 < i; i12++) {
                    mRevDataBuf[i12] = mRevDataBuf[(mRevDataLength - i) + i12];
                }
                mRevDataLength = i;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void disConnect() {
        if (this.gatt != null) {
            this.gatt.disconnect();
        }
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
                    return;
                } else if (!this.isConnected) {
                    Toast.makeText(this.mContext, getResources().getString(R.string.ununited), Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    setQuickening();
                    return;
                }
            case R.id.btn_switch:
                if (this.needPermission) {
                    Toast.makeText(this.mContext, getResources().getString(R.string.no_ble_permission), Toast.LENGTH_LONG).show();
                    return;
                } else if (!this.isConnected) {
                    Toast.makeText(this.mContext, getResources().getString(R.string.ununited), Toast.LENGTH_SHORT).show();
                    return;
                } else if (Util.getFreeMB() < 60) {
                    Toast.makeText(this.mContext, getResources().getString(R.string.free_space_insufficient), Toast.LENGTH_SHORT).show();
                    return;
                } else if (!isRecording) {
                    recordStart();
                    this.mSoundCard.startRecord(this.audio_path_name);
                    return;
                } else {
                    this.mSoundCard.stopRecord();
                    recordEnd(getResources().getString(R.string.recordEnd_if_end));
                    return;
                }
            default:
                return;
        }
    }
}