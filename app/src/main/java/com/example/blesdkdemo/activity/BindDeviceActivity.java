package com.example.blesdkdemo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.blesdkdemo.AppInfo;
import com.example.blesdkdemo.R;
import com.example.blesdkdemo.contract.BleContract;
import com.example.blesdkdemo.info.TemperatureInfo;
import com.example.blesdkdemo.presenter.BlePresenter;
import com.example.blesdkdemo.util.CheckBleFeaturesUtil;
import com.example.blesdkdemo.view.TopBar;
import com.example.blesdkdemo.view.dialog.BleAlertDialog;
import com.example.blesdkdemo.view.dialog.FirmwareUpdateDialog;
import com.example.blesdkdemo.view.loading.LoadingView;
import com.ikangtai.bluetoothsdk.ScPeripheralManager;
import com.ikangtai.bluetoothsdk.http.respmodel.CheckFirmwareVersionResp;
import com.ikangtai.bluetoothsdk.info.HardwareInfo;
import com.ikangtai.bluetoothsdk.listener.CheckFirmwareVersionListener;
import com.ikangtai.bluetoothsdk.model.HardwareModel;
import com.ikangtai.bluetoothsdk.model.ScPeripheral;
import com.ikangtai.bluetoothsdk.util.BleTools;
import com.ikangtai.bluetoothsdk.util.LogUtils;
import com.ikangtai.bluetoothsdk.util.ToastUtils;

import java.util.List;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

/**
 * desc
 *
 * @author xiongyl 2021/4/1 19:31
 */
public class BindDeviceActivity extends AppCompatActivity {
    public static final String TAG = BindDeviceActivity.class.getSimpleName();
    private TopBar topBar;
    public TextView stepFirstState;
    public TextView stepSecondState;
    private TextView stepThirdState;
    private LoadingView stepFirstLoading, stepSecondLoading, stepThirdLoading;
    private HardwareInfo hardwareInfo;
    private BlePresenter presenter;

    /**
     * Receive thermometer status
     */
    public void syncBLeState(boolean isConn) {
        if (isConn) {
            stepSecondLoading.finishLoading();
            stepSecondState.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.device_binding_page_pic_device_selected, 0);
            stepThirdLoading.startLoading();
            ToastUtils.show(this, getString(R.string.binding));
        } else {
            stepSecondLoading.initLoading();
            stepSecondState.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.device_binding_page_pic_device_unselected, 0);
            handleScanSate();
        }
    }

    /**
     * Receive device Bluetooth status
     *
     * @param isOpen
     */
    public void synBluetoothState(boolean isOpen) {
        if (isOpen) {
            Log.i(TAG, "STATE_OFF");
            handleScanSate();
        } else {
            Log.i(TAG, "STATE_OFF");
            stepFirstLoading.initLoading();
            stepSecondLoading.initLoading();
            stepFirstState.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.device_binding_page_pic_bluetooth_unselected, 0);
        }
    }

    /**
     * Receive device info
     *
     * @param scPeripheral
     */
    public void synBleDeviceInfo(ScPeripheral scPeripheral) {
        if (scPeripheral != null) {
            handleFirmwareInfo(scPeripheral);
            checkFirmwareVersion(scPeripheral);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppInfo.getInstance().setBindActivityActive(true);
        setContentView(R.layout.activity_bind_device);
        initView();
        presenter = new BlePresenter(this, new BleContract.IView() {
            @Override
            public void onReceiveTemperatureData(List<TemperatureInfo> temperatureInfoList) {

            }

            @Override
            public void onSaveTemperatureData(TemperatureInfo temperatureInfo) {

            }

            @Override
            public void onReceiveBleDeviceInfo(ScPeripheral scPeripheral) {
                synBleDeviceInfo(scPeripheral);
            }

            @Override
            public void refreshBluetoothState(boolean state) {
                synBluetoothState(state);
            }

            @Override
            public void refreshBleState(String macAddress, boolean state) {
                syncBLeState(state);
            }
        });
    }

    private void initView() {
        topBar = findViewById(R.id.topBar);
        topBar.setOnTopBarClickListener(new TopBar.OnTopBarClickListener() {
            @Override
            public void leftClick() {
                finish();
            }

            @Override
            public void midLeftClick() {

            }

            @Override
            public void midRightClick() {

            }

            @Override
            public void rightClick() {

            }
        });
        stepFirstLoading = findViewById(R.id.stepFirstLoading3);
        stepSecondLoading = findViewById(R.id.stepSecondLoading3);
        stepThirdLoading = findViewById(R.id.stepThirdLoading3);
        stepFirstState = findViewById(R.id.stepFirstState3);
        stepSecondState = findViewById(R.id.stepSecondState3);
        stepThirdState = findViewById(R.id.stepThirdState3);
    }

    public void handleScanSate() {
        if (BleTools.checkBleEnable()) {
            stepFirstLoading.finishLoading();
            stepFirstState.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.device_binding_page_pic_bluetooth_selected, 0);
            stepSecondLoading.startLoading();
        } else {
            stepFirstLoading.initLoading();
            stepSecondLoading.initLoading();
            stepFirstState.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.device_binding_page_pic_bluetooth_unselected, 0);
        }
    }

    /**
     * Bind the device and save the scanned device information
     *
     * @param scPeripheral
     */
    private void handleFirmwareInfo(ScPeripheral scPeripheral) {
        LogUtils.i("Prepare to bind the device");
        hardwareInfo = HardwareInfo.toHardwareInfo(scPeripheral);
        HardwareModel.saveHardwareInfo(BindDeviceActivity.this, hardwareInfo);
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "onResume");
        if (!AppInfo.getInstance().isThermometerState() && !AppInfo.getInstance().isOADConnectActive()) {
            presenter.startScan();
            handleScanSate();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy");
        AppInfo.getInstance().setBindActivityActive(false);
        presenter.destroy();
    }

    /**
     * Check whether the firmware version needs to be upgraded
     * mockData=true ,Mock Check whether the firmware version needs to be upgraded
     *
     * @see ScPeripheralManager.getInstance().checkFirmwareVersion( ScPeripheral scPeripheral, boolean isMockData, CheckFirmwareVersionListener checkFirmwareVersionListener)
     */
    public void checkFirmwareVersion(ScPeripheral scPeripheral) {
        Log.i(TAG, "check firmware version");
        boolean mockData = false;
        ScPeripheralManager.getInstance().checkFirmwareVersion(scPeripheral,mockData, new CheckFirmwareVersionListener() {
            @Override
            public void checkSuccess(final CheckFirmwareVersionResp.Data data) {
                if (Double.parseDouble(data.getVersion()) > Double.parseDouble(hardwareInfo.getHardwareVersion())) {
                    new BleAlertDialog(BindDeviceActivity.this).builder()
                            .setTitle(getString(R.string.tips))
                            .setMsg(getString(R.string.device_upate_tips))
                            .setCancelable(false)
                            .setCanceledOnTouchOutside(false)
                            .setPositiveButton(getString(R.string.ok), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    new FirmwareUpdateDialog(BindDeviceActivity.this, hardwareInfo, data).builder().initEvent(new FirmwareUpdateDialog.IEvent() {
                                        @Override
                                        public void onDismiss() {
                                            bindSuccess();
                                        }
                                    }).show();
                                }
                            }).show();
                } else {
                    bindSuccess();
                }
            }

            @Override
            public void checkFail() {
                bindSuccess();
            }
        });

    }

    private void bindSuccess() {
        Log.i(TAG, "The user successfully binds the thermometer");
        stepThirdLoading.finishLoading();
        stepThirdState.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.device_binding_page_pic_check_selected, 0);
        ToastUtils.show(getApplicationContext(), getString(R.string.bind_success));
        startActivity(new Intent(BindDeviceActivity.this, BindResultActivity.class));
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        CheckBleFeaturesUtil.handBleFeaturesResult(this, requestCode, resultCode);
    }
}

