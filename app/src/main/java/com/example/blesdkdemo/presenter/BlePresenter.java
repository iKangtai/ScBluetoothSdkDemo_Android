package com.example.blesdkdemo.presenter;

import android.app.Activity;

import androidx.fragment.app.Fragment;

import com.example.blesdkdemo.contract.BleContract;
import com.example.blesdkdemo.info.TemperatureInfo;
import com.example.blesdkdemo.model.BleModel;
import com.ikangtai.bluetoothsdk.model.ScPeripheral;

import java.util.List;

/**
 * desc
 *
 * @author xiongyl 2021/1/30 20:16
 */
public class BlePresenter implements BleContract.IPresenter {
    private BleModel bleModel;
    private BleContract.IView bleView;

    public BlePresenter(Activity activity, BleContract.IView bleView) {
        this.bleView = bleView;
        this.bleModel = new BleModel();
        this.bleModel.init(this, activity);
    }

    public BlePresenter(Fragment fragment, BleContract.IView bleView) {
        this.bleView = bleView;
        this.bleModel = new BleModel();
        this.bleModel.init(this, fragment);
    }

    @Override
    public void onReceiveTemperatureData(List<TemperatureInfo> temperatureInfoList) {
        this.bleView.onReceiveTemperatureData(temperatureInfoList);
    }

    @Override
    public void onSaveTemperatureData(TemperatureInfo temperatureInfo) {
        this.bleView.onSaveTemperatureData(temperatureInfo);
    }

    @Override
    public void refreshDeviceList() {
        this.bleModel.refreshDeviceList();
    }

    @Override
    public void updateBleDeviceInfo(ScPeripheral scPeripheral) {
        this.bleView.onReceiveBleDeviceInfo(scPeripheral);
    }

    @Override
    public void startScan() {
        this.bleModel.startScan();
    }

    @Override
    public void stopScan() {
        this.bleModel.stopScan();
    }

    @Override
    public void destroy() {
        this.bleModel.destroy();
    }

    @Override
    public void refreshBluetoothState(boolean state) {
        this.bleView.refreshBluetoothState(state);
    }

    @Override
    public void refreshBleState(String macAddress, boolean state) {
        this.bleView.refreshBleState(macAddress, state);
    }
}
