# ScBluetoothSdkDemo_Android

## Add SDK reference
    1.In the first way, copy the SDK aar file to the app/libs/ directory of the project, then configure gradle
        android {
            repositories {
                flatDir {
                dirs 'libs'
                }
            }
        }

        dependencies {
            implementation(name: 'scbluetoothlib-release-v1.0.0-alpha1', ext: 'aar')
        }


    2.The second way, to copy the ScBluetoothLib module configuration of Demo to the project, and then add implementation project(':ScBluetoothLib') to gradle dependencies

## Init before using SDK
    ScBluetoothClient.getInstance().init(getContext());

## Scan nearby Bluetooth devices
    //Before the scan starts, you need to check the positioning service switch above 6.0, the positioning authority of the system above 6.0, and the Bluetooth switch
    scBluetoothClient.startScanDevice(new ScanResultListener() {
        @Override
        public void onScannerResult(List<ScBluetoothDevice> deviceList) {

        }
    });
    After the scan is completed, you need to call scBluetoothClient.stopScanDevice()

## Connect the Bluetooth device
  scBluetoothClient.connectDevice(macAddress, receiveDataListenerAdapter);

## Manually release resources when the call is completed
    scBluetoothClient.stopScanDevice();
    scBluetoothClient.disconnectDevice();

## Obfuscation settings
    -dontwarn  com.ikangtai.bluetoothsdk.**
    -keep class com.ikangtai.bluetoothsdk.** {*;}

