package com.example.blesdkdemo.model;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import com.example.blesdkdemo.AppInfo;
import com.example.blesdkdemo.Constant;
import com.example.blesdkdemo.Keys;
import com.example.blesdkdemo.R;
import com.example.blesdkdemo.contract.BleContract;
import com.example.blesdkdemo.info.TemperatureInfo;
import com.example.blesdkdemo.util.CheckBleFeaturesUtil;
import com.ikangtai.bluetoothsdk.BleCommand;
import com.ikangtai.bluetoothsdk.Config;
import com.ikangtai.bluetoothsdk.ScPeripheralManager;
import com.ikangtai.bluetoothsdk.info.HardwareInfo;
import com.ikangtai.bluetoothsdk.listener.ReceiveDataListenerAdapter;
import com.ikangtai.bluetoothsdk.listener.ScanResultListener;
import com.ikangtai.bluetoothsdk.model.BleCommandData;
import com.ikangtai.bluetoothsdk.model.HardwareModel;
import com.ikangtai.bluetoothsdk.model.ScPeripheral;
import com.ikangtai.bluetoothsdk.model.ScPeripheralData;
import com.ikangtai.bluetoothsdk.util.BleTools;
import com.ikangtai.bluetoothsdk.util.DateUtil;
import com.ikangtai.bluetoothsdk.util.FileUtil;
import com.ikangtai.bluetoothsdk.util.LogUtils;
import com.ikangtai.bluetoothsdk.util.ToastUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import androidx.fragment.app.Fragment;


/**
 * Handle the synchronized temperature of the connected thermometer
 *
 * @author xiongyl 2021/1/30 20:16
 */
public class BleModel {
    private Context context;
    private Activity activity;
    private Fragment fragment;
    private ScPeripheralManager scPeripheralManager;
    private ReceiveDataListenerAdapter receiveDataListenerAdapter;
    private boolean mScanning;
    private List<HardwareInfo> hardwareInfoList;
    private BleContract.IPresenter blePresenter;
    private ScPeripheral connectScPeripheral;
    private Handler handler;
    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            scanLeDevice();
        }
    };
    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    LogUtils.e("Phone Bluetooth is turned off");
                    refreshBluetoothState(false);
                } else if (state == BluetoothAdapter.STATE_ON) {
                    LogUtils.e("Phone Bluetooth is turned on");
                    refreshBluetoothState(true);
                    if (AppInfo.getInstance().isOADConnectActive()) {
                        return;
                    }
                    restartScan();
                }
            }
        }
    };

    /**
     * Restart scanning
     */
    public void restartScan() {
        stopScan();
        startScan();
    }

    public void init(BleContract.IPresenter blePresenter, Activity activity) {
        init(blePresenter, activity, null);
    }

    public void init(BleContract.IPresenter blePresenter, Fragment fragment) {
        init(blePresenter, null, fragment);
    }

    private void init(BleContract.IPresenter blePresenter, Activity activity, Fragment fragment) {
        this.blePresenter = blePresenter;
        this.fragment = fragment;
        this.activity = activity;
        if (activity != null) {
            context = activity;
        } else if (fragment != null) {
            context = fragment.getContext();
        }
        handler = new Handler(context.getMainLooper());
        this.initBleSdk();
        this.registerBleReceiver();
        this.refreshDeviceList();
    }

    private void registerBleReceiver() {
        //Register to receive Bluetooth switch broadcast
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        if (activity != null) {
            activity.registerReceiver(receiver, filter);
        } else if (fragment != null) {
            fragment.getActivity().registerReceiver(receiver, filter);
        }
    }

    private void initBleSdk() {
        scPeripheralManager = ScPeripheralManager.getInstance();
        String logFilePath = new File(FileUtil.createRootPath(context), "bleSdkLog.txt").getAbsolutePath();
        BufferedWriter logWriter = null;
        try {
            logWriter = new BufferedWriter(new FileWriter(logFilePath, true), 2048);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /**
         * There are two ways to configure log
         * 1. {@link Config.Builder#logWriter(Writer)}
         * 2. {@link Config.Builder#logFilePath(String)}
         */
        Config config = new Config.Builder().logWriter(logWriter).build();
        //sdk init
        scPeripheralManager.init(context, Constant.appId, Constant.appSecret, Constant.unionId, config);
        receiveDataListenerAdapter = new ReceiveDataListenerAdapter() {
            private long endConnBLETime;
            private long distanceTime = 5 * 60 * 1000;

            @Override
            public void onReceiveData(String macAddress, List<ScPeripheralData> scPeripheralDataList) {
                if (scPeripheralDataList != null && scPeripheralDataList.size() > 0) {
                    CopyOnWriteArrayList temperatureInfoList = new CopyOnWriteArrayList<>();
                    //处理离线温度
                    for (int i = 0; i < scPeripheralDataList.size(); i++) {
                        TemperatureInfo temperatureInfo = new TemperatureInfo();
                        temperatureInfo.setMeasureTime(DateUtil.getStringToDate(scPeripheralDataList.get(i).getDate()));
                        temperatureInfo.setTemperature(scPeripheralDataList.get(i).getTemp());
                        temperatureInfoList.add(temperatureInfo);
                    }

                    LogUtils.i("Prepare to return body temperature data>>>");
                    filterTempWithValidTime(temperatureInfoList);
                    //notifyUserTemperature(temperatureInfoList);
                    if (temperatureInfoList != null
                            && temperatureInfoList.size() > 0) {
                        blePresenter.onReceiveTemperatureData(temperatureInfoList);
                        LogUtils.i("End of processing body temperature data");
                    } else {
                        blePresenter.onReceiveTemperatureData(temperatureInfoList);
                        LogUtils.i("End of processing body temperature data ---> The body temperature has been received but the body temperature data does not meet the specifications");
                    }
                }
            }

            @Override
            public void onReceiveError(String macAddress, int code, String msg) {
                super.onReceiveError(macAddress, code, msg);
                LogUtils.d("onReceiveError:" + code + "  " + msg);
            }

            @Override
            public void onReceiveCommandData(String macAddress, int type, int resultCode, String value) {
                super.onReceiveCommandData(macAddress, type, resultCode, value);
                if (resultCode == BleCommand.ResultCode.RESULT_FAIL) {
                    return;
                }
                if (AppInfo.getInstance().isOADConnectActive()) {
                    return;
                }
                LogUtils.d("onReceiveCommandData:" + type + "  " + resultCode + " " + value);
                switch (type) {
                    case BleCommand.GET_FIRMWARE_VERSION:
                        BleCommandData commandData = new BleCommandData();
                        commandData.setParam1(AppInfo.getInstance().isTempUnitC() ? 1 : 2);
                        scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_THERMOMETER_UNIT, commandData);
                        if (connectScPeripheral != null) {
                            connectScPeripheral.setVersion(value);
                            if (blePresenter != null) {
                                blePresenter.updateBleDeviceInfo(connectScPeripheral);
                            }
                        }
                        break;
                }
            }

            @Override
            public void onReceiveCommandData(String macAddress, int type, int resultCode, byte[] value) {
                super.onReceiveCommandData(macAddress, type, resultCode, value);
                if (resultCode == BleCommand.ResultCode.RESULT_FAIL) {
                    return;
                }
                if (AppInfo.getInstance().isOADConnectActive()) {
                    return;
                }
                //todo receive byte data
            }

            @Override
            public void onConnectionStateChange(String macAddress, int state) {
                super.onConnectionStateChange(macAddress, state);
                if (AppInfo.getInstance().isOADConnectActive()) {
                    return;
                }
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    LogUtils.i("The device is connected " + macAddress);
                    endConnBLETime = System.currentTimeMillis();
                    LogUtils.i("connected!");
                    refreshBleState(macAddress, true);
                    if (connectScPeripheral != null && connectScPeripheral.getDeviceType() == BleTools.TYPE_LJ_TXY) {
                        if (blePresenter != null) {
                            blePresenter.updateBleDeviceInfo(connectScPeripheral);
                        }
                    }
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    LogUtils.i("Device disconnected " + macAddress);
                    long currentTime = System.currentTimeMillis();
                    if (endConnBLETime == 0 || (currentTime - endConnBLETime) > distanceTime) {
                        LogUtils.i("disconnect!");
                        endConnBLETime = currentTime;
                    }
                    refreshBleState(macAddress, false);
                    if (AppInfo.getInstance().isBindActivityActive() || !HardwareModel.hardwareList(context).isEmpty()) {
                        restartScan();
                    }
                }
            }
        };
        scPeripheralManager.addReceiveDataListener(receiveDataListenerAdapter);
    }

    /**
     * Refresh Bluetooth connection status
     *
     * @param state
     */
    private void refreshBluetoothState(boolean state) {
        AppInfo.getInstance().setBluetoothState(state);
        if (blePresenter != null) {
            blePresenter.refreshBluetoothState(state);
        }
    }

    /**
     * Refresh the thermometer connection status
     *
     * @param state
     */
    private void refreshBleState(String macAddress, boolean state) {
        AppInfo.getInstance().setThermometerState(state);
        if (blePresenter != null) {
            blePresenter.refreshBleState(macAddress, state);
        }
    }

    public void filterTempWithValidTime(CopyOnWriteArrayList<TemperatureInfo> temperatureInfoList) {
        if (temperatureInfoList != null) {
            LogUtils.i("Start - filter invalid body temperature data");

            //Filter out data that deviates from the temperature value
            if (temperatureInfoList.size() != 0) {
                for (int i = temperatureInfoList.size() - 1; i >= 0; i--) {
                    if (temperatureInfoList.get(i).getTemperature() >= Keys.C_MAX
                            || temperatureInfoList.get(i).getTemperature() < Keys.C_MIN) {
                        temperatureInfoList.remove(i);
                    }
                }
                LogUtils.i("Filter out the data other than the temperature 32-43, and the number of remaining items: " + temperatureInfoList.size());
                if (temperatureInfoList.size() == 0) {
                    if (!AppInfo.getInstance().isBindActivityActive()) {
                        ToastUtils.show(context, context.getString(R.string.bbt_valid_value_post_fail));
                    }
                }
            }
            LogUtils.i("End - filter invalid body temperature data");
        }
    }

    public void startScan() {
        if (!mScanning) {
            handler.postDelayed(scanRunnable, 1500);
        }
    }

    public void stopScan() {
        mScanning = false;
        LogUtils.i("Stop scanning");
        handler.removeCallbacks(scanRunnable);
        try {
            scPeripheralManager.stopScan();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void scanLeDevice() {
        if (activity != null && !CheckBleFeaturesUtil.checkBleFeatures(activity)) {
            return;
        } else if (fragment != null && !CheckBleFeaturesUtil.checkBleFeatures(fragment)) {
            return;
        }
        mScanning = true;
        scPeripheralManager.startScan(new ScanResultListener() {
            @Override
            public void onScannerResult(List<ScPeripheral> deviceList) {
                if (deviceList != null && mScanning) {
                    for (int i = 0; i < deviceList.size(); i++) {
                        ScPeripheral scBluetoothDevice = deviceList.get(i);
                        if (scBluetoothDevice.getDeviceType() == BleTools.TYPE_UNKNOWN) {
                            continue;
                        }
                        if (AppInfo.getInstance().isBindActivityActive() || hardwareInfoList != null && !hardwareInfoList.isEmpty() && hardwareInfoList.get(0).getHardMacId().contains(scBluetoothDevice.getMacAddress())) {
                            connectScPeripheral = scBluetoothDevice;
                            String deviceAddr = connectScPeripheral.getMacAddress();
                            LogUtils.i("Device has been scanned! Stop scanning! " + deviceAddr);
                            stopScan();
                            LogUtils.i("Start requesting to connect to the device:" + scBluetoothDevice.getMacAddress());
                            scPeripheralManager.connectPeripheral(scBluetoothDevice.getMacAddress());
                            break;
                        }
                    }

                }
            }
        });

    }

    public void refreshDeviceList() {
        hardwareInfoList = HardwareModel.hardwareList(context);
    }


    public void destroy() {
        if (activity != null) {
            activity.unregisterReceiver(receiver);
        } else if (fragment != null) {
            fragment.getActivity().unregisterReceiver(receiver);
        }
        if (mScanning) {
            stopScan();
        }
        scPeripheralManager.disconnectPeripheral();
        scPeripheralManager.removeReceiveDataListener(receiveDataListenerAdapter);
    }
}
