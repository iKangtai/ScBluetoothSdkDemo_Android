package com.example.blesdkdemo.contract;

import com.example.blesdkdemo.info.TemperatureInfo;
import com.ikangtai.bluetoothsdk.model.ScPeripheral;

import java.util.List;

/**
 * desc
 *
 * @author xiongyl 2021/1/30 20:14
 */
public class BleContract {
    public interface IView {
        /**
         * Receive thermometer temperature data
         *
         * @param temperatureInfoList
         */
        void onReceiveTemperatureData(List<TemperatureInfo> temperatureInfoList);

        /**
         * Save manually added temperature data
         *
         * @param temperatureInfo
         */
        void onSaveTemperatureData(TemperatureInfo temperatureInfo);

        void onReceiveBleDeviceInfo(ScPeripheral scPeripheral);

        void refreshBluetoothState(boolean state);

        void refreshBleState(String macAddress, boolean state);
    }

    public interface IPresenter {

        /**
         * Receive thermometer temperature data
         *
         * @param temperatureInfoList
         */
        void onReceiveTemperatureData(List<TemperatureInfo> temperatureInfoList);

        /**
         * Save manually added temperature
         *
         * @param temperatureInfo
         */
        void onSaveTemperatureData(TemperatureInfo temperatureInfo);

        /**
         * Refresh the list of bound devices
         */
        void refreshDeviceList();

        void updateBleDeviceInfo(ScPeripheral scPeripheral);

        /**
         * Start scanning for nearby devices
         */
        void startScan();

        /**
         * Stop scanning
         */
        void stopScan();

        /**
         * Release resources
         */
        void destroy();

        void refreshBluetoothState(boolean state);

        void refreshBleState(String macAddress, boolean state);
    }
}
