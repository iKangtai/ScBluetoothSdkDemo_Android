package com.example.blesdkdemo;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/**
 * App data cache
 *
 * @author xiongyl 2021/1/21 22:10
 */
public class AppInfo {
    /**
     * Device connection interface
     */
    private boolean isDeviceConnectActive = false;

    /**
     * OAD connection interface
     */
    private boolean isOADConnectActive = false;
    /**
     * Bluetooth status on/off
     */
    private boolean bluetoothState;
    /**
     * Thermometer status on/off
     */
    private boolean thermometerState;
    /**
     * Whether it is on the binding page
     */
    private boolean isBindActivityActive = false;
    private static volatile AppInfo instance;

    private AppInfo() {
    }

    public static AppInfo getInstance() {
        if (instance == null) {
            synchronized (AppInfo.class) {
                if (instance == null) {
                    instance = new AppInfo();
                }
            }
        }

        return instance;
    }

    public boolean isBindActivityActive() {
        return isBindActivityActive;
    }

    public void setBindActivityActive(boolean bindActivityActive) {
        isBindActivityActive = bindActivityActive;
    }

    public void setDeviceConnectActive(boolean deviceConnectActive) {
        isDeviceConnectActive = deviceConnectActive;
    }

    public boolean isDeviceConnectActive() {
        return isDeviceConnectActive;
    }

    public void setOADConnectActive(boolean OADConnectActive) {
        isOADConnectActive = OADConnectActive;
    }

    public boolean isOADConnectActive() {
        return isOADConnectActive;
    }

    public boolean isBluetoothState() {
        return bluetoothState;
    }

    public void setBluetoothState(boolean bluetoothState) {
        this.bluetoothState = bluetoothState;
    }

    public boolean isThermometerState() {
        return thermometerState;
    }

    public void setThermometerState(boolean thermometerState) {
        this.thermometerState = thermometerState;
    }

    /**
     * Does the app display degrees Celsius
     * @return
     */
    public boolean isTempUnitC() {
        return true;
    }

    public float getTemp(float temp) {

        if (isTempUnitC()) {
            return temp;
        } else {
            DecimalFormat decimalFormat = new DecimalFormat(".000");
            DecimalFormatSymbols dfs = new DecimalFormatSymbols();
            dfs.setDecimalSeparator('.');
            decimalFormat.setDecimalFormatSymbols(dfs);
            String tempF = decimalFormat.format(1.8 * temp + 32);
            return Float.valueOf(tempF.substring(0, tempF.length() - 1));
        }
    }

    public String getTempUnit() {
        return isTempUnitC() ? Keys.kTempUnitC : Keys.kTempUnitF;
    }

    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * 手机厂商
     *
     * @return
     */
    public static String getPhoneProducer() {
        String phoneProducer = android.os.Build.BRAND;
        return phoneProducer;
    }

    /**
     * 获取手机型号
     *
     * @return
     */
    public static String getPhoneModel() {
        String phoneModel = android.os.Build.MODEL;
        return phoneModel;
    }

}
