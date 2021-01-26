# ScBluetoothSdkDemo_Android

## Demo

<https://github.com/iKangtai/ScBluetoothSdkDemo_Android.git>
## Access Guide
### SDK features

| Function item                    |  Function description 		|
| ------------------------- | ------------  	|
| Scan for nearby Bluetooth devices          | Scan for Bluetooth devices near the phone and refresh the device list every second |
| Connect to Shecare thermometer to synchronize data&nbsp;&nbsp;&nbsp;&nbsp;| Connect the thermometer to synchronize data, set the thermometer temperature unit and time, and get the firmware version |
| Connect Shecare forehead thermometer to synchronize data&nbsp;&nbsp;| Connect the forehead thermometer to synchronize data and get the firmware version number |

### Integrated SDK
1.The first way
Add the new maven warehouse address of Bluetooth SDK in the buildscript and allprojects sections of the project build.gradle configuration script:
```java
	maven { url 'https://dl.bintray.com/ikangtaijcenter123/ikangtai' }
```
Add the statistics SDK library dependency in the dependencies section of the project App corresponding build.gradle configuration script:
```java
    implementation 'com.ikangtai.buletoothsdk:ScBuletoothLib:1.1.6'
```
2.The second way,to copy the SDK aar file to the app/libs/ directory of the project, and then configure gradle
```java
        android {
            repositories {
                flatDir {
                dirs 'libs'
                }
            }
        }

        dependencies {
            implementation(name: 'scbluetoothlib-release-v1.1.6', ext: 'aar')
        }
```
3.The third way,to copy the ScBluetoothLib module configuration of Demo to the project, and then add the implementation project (':ScBluetoothLib') to establish the dependency

### Permission granted

The SDK requires the host APP to grant the following permissions:


| Permission                      |  Use 		|
| ------------------------- | ------------  |
| BLUETOOTH                 | Permission required to connect to Bluetooth devices |
| BLUETOOTH_ADMIN           | Permission required to connect to Bluetooth devices |
| ACCESS_COARSE_LOCATION&nbsp;&nbsp;&nbsp;&nbsp;| From the perspective of security and power consumption in Android 7.0, BLE scanning is tied to location permissions |
| ACCESS_FINE_LOCATION      | From the perspective of security and power consumption in Android 7.0, BLE scanning is tied to location permissions |
| READ_EXTERNAL_STORAGE     | Read and write log files |
| WRITE_EXTERNAL_STORAGE    | Read and write log files |



An example of the AndroidManifest.xml manifest file is given below:
```java
<manifest ……>
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
<application ……>
```

### Init SDK

Before using the SDK, you need to call the init function in the host application UI thread, and the related callback functions will also be in the UI thread:
```java
    ScPeripheralManager.getInstance().init(getContext());
```
### Scan for nearby Bluetooth devices

The scanning method is a continuous process. The latest device list is returned every second. You need to manually call scPeripheralManager.stopScan() after the scan is completed.
```java
    if (!checkBleFeatures()) {
        return;
    }
    scPeripheralManager.startScan(new ScanResultListener() {
        /**
     	  * When scanning nearby devices, call back the device list through this method.
          * The method call will be triggered once per second until called the stopScanDevice method.
     	  * @param deviceList
     	  */
        public void onScannerResult(List<ScPeripheral> deviceList) {

        }
    });
```

Before starting to scan, you need to check the location service switch above 6.0, the location permission of the system above 6.0, and the Bluetooth switch. For specific implementation methods, please see checkBleFeatures() in Demo:
```java
   private boolean checkBleFeatures() {
        //Check Bluetooth Location Service
        if (!BleTools.isLocationEnable(getContext())) {
            Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(locationIntent, REQUEST_LOCATION_SETTINGS);
            return false;
        }
        //Check Bluetooth location permission
        if (!BleTools.checkBlePermission(getContext())) {
            XXPermissions.with(getActivity())
                    .permission(Permission.Group.LOCATION)
                    .request(callback);
            return false;
        }
        //Check the Bluetooth switch
        if (!BleTools.checkBleEnable()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLE_SETTINGS_CODE);
            return false;
        }
        return true;
    }
```

### Connect Bluetooth device

```java
  ReceiveDataListenerAdapter receiveDataListenerAdapter = new ReceiveDataListenerAdapter() {
        /**
         *
         * @param macAddress
         * @param Scan to the device list, return the latest device list every second
         */
        public void onReceiveData(String macAddress, List<ScPeripheralData> scPeripheralDataList) {
            appendConsoleContent("New data received " + macAddress);
        }
        /**
          * The method called when there is an error in the connected device.
          *
          * @param macAddress
          * @param code errorCode ,see{@link com.ikangtai.bluetoothsdk.util.BleCode}
          * @param msg errorMsg
          */
        public void onReceiveError(String macAddress, int code, String msg) {
            LogUtils.d("onReceiveError:" + code + "  " + msg);
        }

        /**
          * This method will be called when the status of the connected device changes.
          *
          * @param macAddress
          * @param state connect state
          */
        public void onConnectionStateChange(String macAddress, int state) {
            if (state == BluetoothProfile.STATE_CONNECTED) {

            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {

            }
        }

        /**
          *
          * @param macAddress
          * @param command type，type see {@link com.ikangtai.bluetoothsdk.BleCommand}
          * @param The status returned by the execution command will return true or false.
          * @param The result value returned by executing the command.
          */
        public void onReceiveCommandData(String macAddress, int type, boolean state, String value) {
            LogUtils.d("onReceiveCommandData:" + type + "  " + state + " " + value);
        }

        /**
          *
          * @param macAddress
          * @param command type，type see {@link com.ikangtai.bluetoothsdk.BleCommand}
          * @param The status returned by the execution command will return true or false.
          * @param The result value byte[]data returned by executing the command,data stream from fetal heart rate monitoring.
          */
        public void onReceiveCommandData(String macAddress, int type, boolean state, byte[] value) {
            LogUtils.d("onReceiveCommandData:" + type + "  " + resultCode + " " + BleUtils.byte2hex(value));
        }
    };
  scPeripheralManager.connectPeripheral(macAddress, receiveDataListenerAdapter);
```
Disconnect manually after use to release resources.

```java
    scPeripheralManager.stopScan();
    scPeripheralManager.disconnectPeripheral();
```

### Interact with the device
The command is called by scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand), and the result of the sending command will be received in ReceiveDataListener.onReceiveCommandData(String macAddress, int type, boolean state, String value).

Get device temperature data
```java
  scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.GET_DEVICE_DATA);
```
Synchronize the system time to the device

```java
  scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_TIME);
```
Modify the device temperature ℃ unit

```java
  scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_THERMOMETER_UNIT_C);
```
Modify the device temperature ℉ unit

```java
  scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_THERMOMETER_UNIT_F);
```

List of commands supported by the SDK:

| Type				|  Description 		|
| -------------------- 	| ------------  |
| SEND_TEMP_ACK = -1	| Send the number of received temperature commands; externally unavailable (sdk internally processed) |
| SYNC_THERMOMETER_UNIT_C = 0 &nbsp;&nbsp;&nbsp;&nbsp;| The thermometer unit is set to Celsius |
| SYNC_THERMOMETER_UNIT_F = 1 | The thermometer unit is set to Fahrenheit |
| SYNC_TIME = 2 			  | Synchronize system time to thermometer |
| GET_TIME = 3 				  | Get the thermometer time (currently not supported)|
| GET_POWER = 4				  | Get the thermometer battery |
| GET_FIRMWARE_VERSION = 5	  | Get the firmware version number of the thermometer |
| GET_DEVICE_DATA = 6		  | Get the data in the thermometer (can only get it once) |
| GET_EWQ_FIRMWARE_VERSION = 7| Get the version number of the forehead thermometer |
| GET_EWQ_DEVICE_DATA = 8	  | Get forehead thermometer data |


The thermometer supports the instruction set:{SEND_TEMP_ACK, SYNC_THERMOMETER_UNIT_C, SYNC_THERMOMETER_UNIT_F, SYNC_TIME, GET_FIRMWARE_VERSION, GET_DEVICE_DATA}
Forehead temperature gun supports instruction set:{GET_EWQ_FIRMWARE_VERSION, GET_EWQ_DEVICE_DATA}

### Confusion configuration
If your application uses code obfuscation, please add the following configuration to avoid SDK being unavailable due to incorrect obfuscation.
```java
    -dontwarn  com.ikangtai.bluetoothsdk.**
    -keep class com.ikangtai.bluetoothsdk.** {*;}
```
### View log
You can control whether the SDK run debug log is output and the output path by calling the following methods. By default, the SDK run debug log is turned on. The user can manually close it.
You can also filter the "sc-ble-log" Tag through Locat to display SDK specific logs.


```java
    /**
     * There are two ways to configure log
     * 1. {@link Config.Builder#logWriter(Writer)}
     * 2. {@link Config.Builder#logFilePath(String)}
     */
    LogUtils.LOG_SWITCH=true;
    Config config = new Config.Builder().logWriter(logWriter).build();
    //Config config = new Config.Builder().logFilePath(logFilePath).build();
    scPeripheralManager.init(getContext(), config);
```

### Error code description

Every time the SDK interface is called, a correct or incorrect return code may be obtained. Developers can debug the interface according to the return code information and troubleshoot errors.


| Code				|  Description 		|
| -------------------- 	| ------------  |
| -1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;| Unknown error, sdk cannot handle the error |
| -2 					| macAddress error |
| -3					| The system location switch is not turned on |
| -4 					| App lacks location permission |
| -5 					| Bluetooth is not turned on |
| -6					| The device does not currently support this command |
| -7					| The device is not connected|
| -8					| The number of currently connected devices exceeds the upper limit (up to 5) |

### Q&A
- Why do I receive the onReceveData callback if I did not send the SYNC_DATA command after connecting the thermometer
>After the thermometer is successfully connected inside the SDK, it will actively obtain offline data through the "SYNC_DATA command", call back through onReceveData, and synchronize the system time with the thermometer through the "SYNC_TIME command" after the data is acquired.
- SDK instruction call timing
> When the thermometer is not connected, the sending command will directly return an error code. When a command is sent during the connection process, the command in the command queue will be called after the connection is successful. The SDK processes one instruction at a time and sends multiple identical instructions at the same time, and the SDK executes it only once. Send multiple different instructions at the same time, the SDK will be stored in the instruction queue and executed in sequence.
- ACK command
> When receiving the temperature data, the SDK needs to reply the "SYNC_ACK command" of the thermometer, and bring the number of the current received. When the synchronized temperature data is greater than 10, it will be divided into a group of 10 data. Normally, this instruction is processed internally by the SDK and no additional processing is required. Sending this command by the SDK may cause repeated temperature data to be received.
If you really want to call the ACK instruction externally, you need to modify Config, new Config.Builder().forceOutsideSendAck(true), and then call scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SEND_TEMP_ACK, scPeripheralDataList.size());
- SDK operations need to be in the UI thread
> Currently, when the SDK calls back data, it will actively switch to the UI thread
- Failed to set temperature unit, failed to synchronize time
> Failed to send instructions is usually because the thermometer is not connected or the connection is unstable. It is recommended to check the SDK log output to troubleshoot the problem.
- The thermometer is always synchronized to repeat the temperature
> There are abnormal temperature data in the clinical thermometer, which causes the SDK to process the temperature error. You can contact the SDK provider to carry it, and pay attention to the log file.

## SDK introduction

### ScPeripheralManager
```java
public class ScPeripheralManager {

    /**
     * Start scanning nearby devices and call back the device list through this {@link ScanResultListener#onScannerResult(List)}.
     * The method call will be triggered once per second until called the {@link #stopScan()} method.
     *
     * @param scanResultListener
     */
    public void startScan(ScanResultListener scanResultListener) {
        BleManager.getInstance().startScanAllDevice(scanResultListener);
    }

    /**
     * Stop scan device
     */
    public void stopScan() {
        BleManager.getInstance().stopScanDevice();
    }

    /**
     * Add listeners to a set of listeners that will send events during device connection.
     * When you want to disconnect the device, you need to actively call {@link ScPeripheralManager#removeReceiveDataListener(ReceiveDataListener)}
     *
     * @param receiveDataListener the listener to be added to the current set of listeners for {@link com.ikangtai.bluetoothsdk.BleManager}.
     */
    public void addReceiveDataListener(ReceiveDataListener receiveDataListener) {
        BleManager.getInstance().addReceiveDataListener(receiveDataListener);
    }

    /**
     * Removes a listener from the set listening to data updates for {@link com.ikangtai.bluetoothsdk.BleManager}.
     *
     * @param receiveDataListener ,the listener to be removed from the current set of update listeners for {@link com.ikangtai.bluetoothsdk.BleManager}.
     *
     */
    public void removeReceiveDataListener(ReceiveDataListener receiveDataListener) {
        BleManager.getInstance().removeReceiveDataListener(receiveDataListener);
    }

    /**
     * Connect the device,need set global listeners {@link #addReceiveDataListener(ReceiveDataListener)} listening to data updates.
     *
     * @param macAddress
     */
    public void connectPeripheral(String macAddress) {
        BleManager.getInstance().startConnectDevice(macAddress, null);
    }

    /**
     * Connect the device,adds a listener to the set of listeners
     * that are sent data update events for {@link com.ikangtai.bluetoothsdk.BleManager}.
     *
     * @param macAddress
     * @param receiveDataListener
     */

    public void connectPeripheral(String macAddress, ReceiveDataListener receiveDataListener) {
        BleManager.getInstance().startConnectDevice(macAddress, receiveDataListener);
    }

    /**
     * Send device command
     *
     * @param type
     */
    public void sendPeripheraleCommand(String macAddress, int type) {
        BleManager.getInstance().sendDeviceCommand(macAddress, type);
    }

    /**
     * Get device connection state.
     *
     * @param macAddress
     * @return either one of state {@link android.bluetooth.BluetoothProfile }
     */
    public int getConnectState(String macAddress) {
        return BleManager.getInstance().getConnectState(macAddress);
    }

    /**
     * Disconnect the device
     *
     * @param macAddress
     */
    public void disconnectPeripheral(String macAddress) {
        BleManager.getInstance().disconnect(macAddress);
    }

    public void disconnectPeripheral(String macAddress, ReceiveDataListener receiveDataListener) {
        BleManager.getInstance().disconnect(macAddress, receiveDataListener);
    }

    /**
     * Disconnect all devices
     */
    public void disconnectPeripheral() {
        BleManager.getInstance().disconnect();
    }
}

```