# ScBluetoothSdkDemo_Android

### Add SDK reference

```java
    1.In the first way, copy the SDK aar file to the app/libs/ directory of the project, then configure gradle
        android {
            repositories {
                flatDir {
                dirs 'libs'
                }
            }
        }

        dependencies {
            implementation(name: 'scbluetoothlib-release-v1.0.1', ext: 'aar')
        }


    2.The second way, to copy the ScBluetoothLib module configuration of Demo to the project, and then add implementation project(':ScBluetoothLib') to gradle dependencies
```

### Init before using SDK

```java
    ScPeripheralManager.getInstance().init(getContext());
```
### Log config
```java
    /**
     * There are two ways to configure log
     * 1. {@link Config.Builder#logWriter(Writer)}
     * 2. {@link Config.Builder#logFilePath(String)}
     */
    Config config = new Config.Builder().logWriter(logWriter).build();
    //Config config = new Config.Builder().logFilePath(logFilePath).build();
    scPeripheralManager.init(getContext(), config);
```
### Scan nearby Bluetooth devices

```java
    //Before the scan starts, you need to check the positioning service switch above 6.0, the positioning authority of the system above 6.0, and the Bluetooth switch
    scPeripheralManager.startScan(new ScanResultListener() {
        @Override
        public void onScannerResult(List<ScPeripheral> deviceList) {

        }
    });
    After the scan is completed, you need to call scPeripheralManager.stopScan()
```

### Connect the Bluetooth device

```java
  scPeripheralManager.connectPeripheral(macAddress, receiveDataListenerAdapter);
```
### Sync data from Bluetooth device

```java
  scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.GET_DEVICE_DATA);
```
### Sync phone time to Bluetooth device

```java
  scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_TIME);
```
### Sync unit c to Bluetooth device

```java
  scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_THERMOMETER_UNIT_C);
```
### Sync unit f to Bluetooth device

```java
  scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_THERMOMETER_UNIT_F);
```
### Manually release resources when the call is completed

```java
    scPeripheralManager.stopScan();
    scPeripheralManager.disconnectPeripheral();
```

### Obfuscation settings

```java
    -dontwarn  com.ikangtai.bluetoothsdk.**
    -keep class com.ikangtai.bluetoothsdk.** {*;}
```