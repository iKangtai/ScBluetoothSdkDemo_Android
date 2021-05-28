# ScBluetoothSdkDemo_Android

## Demo

<http://fir.ikangtai.cn/qkxb>

## 国际化
[English](README.md) | 中文文档

## 访问指南
### SDK功能

| 功能                    |  描述 		|
| ------------------------- | ------------  	|
| 扫描附近的蓝牙设备          | 扫描手机附近的蓝牙设备，并每秒刷新设备列表 |
| 连接到Shecare温度计以同步数据&nbsp;&nbsp;&nbsp;&nbsp;| 连接温度计以同步数据，设置温度计的温度单位和时间，并获取固件版本 |
| 连接Shecare额温枪同步数据&nbsp;&nbsp;| 连接额温枪同步数据并获取固件版本号 |
| 连接Shecare胎心仪同步数据&nbsp;&nbsp;| 连接胎心仪同步数据并获取固件版本号 |
| 连接Shecare体温贴同步数据&nbsp;&nbsp;| 连接体温贴同步数据并获取固件版本号 |

### 集成 SDK
1.第一种方式
在项目App对应的build.gradle配置脚本的“dependencies”部分中添加统计信息SDK库依赖关系：
```java
    implementation 'com.ikangtai.buletoothsdk:ScBuletoothLib:1.2.3.3'
```
2.第二种方法，将SDK aar文件复制到项目的app/libs/目录，然后配置gradle
```java
        android {
            repositories {
                flatDir {
                dirs 'libs'
                }
            }
        }

        dependencies {
            implementation(name: 'scbluetoothlib-release-v1.2.3.3', ext: 'aar')
        }
```
3.第三种方式，将Demo的ScBluetoothLib模块配置复制到项目中，然后添加实现项目（':ScBluetoothLib'）建立依赖关系

### 请求权限

SDK要求APP授予以下权限：


| Permission                      |  Use 		|
| ------------------------- | ------------  |
| BLUETOOTH                 | 连接到蓝牙设备所需的权限 |
| BLUETOOTH_ADMIN           | 连接到蓝牙设备所需的权限 |
| ACCESS_COARSE_LOCATION&nbsp;&nbsp;&nbsp;&nbsp;| Android 7.0从安全性和功耗的角度考虑，BLE扫描与位置权限相关联 |
| ACCESS_FINE_LOCATION      | Android 7.0从安全性和功耗的角度考虑，BLE扫描与位置权限相关联 |
| READ_EXTERNAL_STORAGE     | 读写日志文件 |
| WRITE_EXTERNAL_STORAGE    | 读写日志文件 |



下面给出了AndroidManifest.xml清单文件的示例：
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

### 初始化SDK

在使用SDK之前，您需要在主机应用程序UI线程中调用init函数，并且相关的回调函数也将在UI线程中：
```java
    ScPeripheralManager.getInstance().init(getContext());
```
### 扫描附近的蓝牙设备

扫描方法是一个连续的过程。 每秒返回最新的设备列表。 扫描完成后，您需要手动调用scPeripheralManager.stopScan().
```java
    if (!checkBleFeatures()) {
        return;
    }
    scPeripheralManager.startScan(new ScanResultListener() {
        /**
     	  * 扫描附近的设备时，通过此方法调用设备列表。
          * 该方法调用将每秒触发一次，直到调用stopScanDevice方法为止。
     	  * @param deviceList
     	  */
        public void onScannerResult(List<ScPeripheral> deviceList) {

        }
    });
```

在开始扫描之前，您需要检查6.0以上的位置服务开关，6.0以上的系统的位置许可以及Bluetooth开关。 有关具体的实现方法，请参见Demo中的checkBleFeatures():
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

### 连接蓝牙设备

```java
  ReceiveDataListenerAdapter receiveDataListenerAdapter = new ReceiveDataListenerAdapter() {
        /**
         *
         * @param macAddress
         * @param 扫描到设备列表，每秒返回最新的设备列表
         */
        public void onReceiveData(String macAddress, List<ScPeripheralData> scPeripheralDataList) {
            appendConsoleContent("New data received " + macAddress);
        }
        /**
          * 当连接的设备出现错误时调用的方法。
          *
          * @param macAddress
          * @param code errorCode ,see{@link com.ikangtai.bluetoothsdk.util.BleCode}
          * @param msg errorMsg
          */
        public void onReceiveError(String macAddress, int code, String msg) {
            LogUtils.d("onReceiveError:" + code + "  " + msg);
        }

        /**
          * 当所连接设备的状态更改时，将调用此方法。
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
          * @param 指令类型，type see {@link com.ikangtai.bluetoothsdk.BleCommand}
          * @param 执行命令返回的状态将返回true或false。
          * @param 执行命令返回的结果
          */
        public void onReceiveCommandData(String macAddress, int type, boolean state, String value) {
            LogUtils.d("onReceiveCommandData:" + type + "  " + state + " " + value);
        }

        /**
          *
          * @param macAddress
          * @param 指令类型，type see {@link com.ikangtai.bluetoothsdk.BleCommand}
          * @param 执行命令返回的状态将返回true或false。
          * @param 执行命令返回的结果值byte [] data，目前来自胎心仪监测的数据流。
          */
        public void onReceiveCommandData(String macAddress, int type, boolean state, byte[] value) {
            LogUtils.d("onReceiveCommandData:" + type + "  " + resultCode + " " + BleUtils.byte2hex(value));
        }
    };
  scPeripheralManager.connectPeripheral(macAddress, receiveDataListenerAdapter);
```
使用后手动断开连接以释放资源。

```java
    scPeripheralManager.stopScan();
    scPeripheralManager.disconnectPeripheral();
```

### Interact with the device
该命令由scPeripheralManager.sendPeripheralCommand（macAddress，BleCommand）调用，并且发送命令的结果将在ReceiveDataListener.onReceiveCommandData(String macAddress, int type, boolean state, String value)返回.

获取设备数据
```java
  scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.GET_DEVICE_DATA);
```
将系统时间同步到设备

```java
  scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_TIME);
```
修改设备温度℃单位

```java
  scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_THERMOMETER_UNIT_C);
```
修改设备温度℉单位

```java
  scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_THERMOMETER_UNIT_F);
```

SDK支持的命令列表：

| 类型				|  描述 		|
| -------------------- 	| ------------  |
| SEND_TEMP_ACK = -1	| 发送接收到的温度指令数，外部不可用（SDK内部已处理） |
| SYNC_THERMOMETER_UNIT_C = 0 &nbsp;&nbsp;&nbsp;&nbsp;| 温度计单位设置为摄氏度(已废弃) |
| SYNC_THERMOMETER_UNIT_F = 1 | 温度计单位设置为华氏度(已废弃) |
| SYNC_TIME = 2 			  | 将系统时间与温度计同步 |
| GET_TIME = 3 				  | 获取温度计时间 |
| GET_POWER = 4				  | 获取温度计电量 |
| GET_FIRMWARE_VERSION = 5	  | 获取温度计的固件版本号 |
| GET_DEVICE_DATA = 6		  | 在温度计中获取数据（只能获取一次） |
| GET_EWQ_FIRMWARE_VERSION = 7| 获取额温枪的版本号 |
| GET_THERMOMETER_UNIT = 8	  | 获取温度计单位 |
| SET_THERMOMETER_MODE = 9	  | 设定温度计测温模式 |
| GET_THERMOMETER_MODE = 10	  | 获取温度计测温模式 |
| GET_THERMOMETER_MEASURE_TIME = 11	  | 获取温度计的温度测量时间  |
| SET_THERMOMETER_MEASURE_TIME1 = 12	  | 设置温度计的温度测量时间1 |
| SET_THERMOMETER_MEASURE_TIME2 = 13	  | 设置温度计的温度测量时间2 |
| CLEAR_THERMOMETER_DATA = 14	  | 清除温度计数据 |
| GET_DEVICE_HISTORY_DATA = 15	  | 获取体温计的历史数据 |
| SEND_HISTORY_DATA_ACK = 16	  | 发送临床体温计确认的历史数据，外部不可用（SDK内部已处理） |
| SYNC_THERMOMETER_UNIT = 17	  | 同步温度计单位 |
| THERMOMETER_OTA_UPGRADE = 18	  | 设备ota升级 |
| GET_THERMOMETER_OAD_IMG_TYPE = 19	  | 获取设备oad img类型 |
| THERMOMETER_OAD_UPGRADE = 20	  | 设备oad升级 |
| IFEVER_DEVICE_VERIFY = 21	  | IFEVER设备安全验证 |

温度计支持以下指令集：
```java
{SEND_TEMP_ACK, SYNC_THERMOMETER_UNIT_C, SYNC_THERMOMETER_UNIT_F, SYNC_TIME, GET_FIRMWARE_VERSION, GET_DEVICE_DATA, SYNC_THERMOMETER_UNIT, GET_POWER, GET_THERMOMETER_OAD_IMG_TYPE, THERMOMETER_OAD_UPGRADE};
```
额温枪支持以下指令集：
```java
{GET_EWQ_FIRMWARE_VERSION, GET_EWQ_DEVICE_DATA}
```
安康源三代温度计支持以下指令集：
```java
{SEND_TEMP_ACK, SYNC_TIME, GET_TIME, GET_FIRMWARE_VERSION, GET_DEVICE_DATA, SET_THERMOMETER_MODE, GET_THERMOMETER_MODE, GET_THERMOMETER_MEASURE_TIME, SET_THERMOMETER_MEASURE_TIME1, SET_THERMOMETER_MEASURE_TIME2, CLEAR_THERMOMETER_DATA, GET_DEVICE_HISTORY_DATA, SYNC_THERMOMETER_UNIT, GET_THERMOMETER_UNIT, GET_POWER,THERMOMETER_OTA_UPGRADE};
```
安康源四代温度计支持以下指令集：
```java
{SEND_TEMP_ACK, SYNC_TIME, GET_TIME, GET_FIRMWARE_VERSION, GET_DEVICE_DATA, GET_THERMOMETER_MEASURE_TIME, SET_THERMOMETER_MEASURE_TIME1, SET_THERMOMETER_MEASURE_TIME2, CLEAR_THERMOMETER_DATA, GET_DEVICE_HISTORY_DATA, SYNC_THERMOMETER_UNIT, GET_THERMOMETER_UNIT, GET_POWER, THERMOMETER_OTA_UPGRADE};
```
胎心仪支持以下指令集：
{}
体温贴支持以下指令集：
```java
{SYNC_TIME, GET_FIRMWARE_VERSION, GET_DEVICE_DATA, GET_POWER, IFEVER_DEVICE_VERIFY};
```


### 混淆配置
如果您的应用程序使用代码混淆，请添加以下配置，以避免由于混淆不正确而导致SDK不可用。
```java
    -dontwarn  com.ikangtai.bluetoothsdk.**
    -keep class com.ikangtai.bluetoothsdk.** {*;}
```
### 查看log
可以通过调用以下方法来控制是否输出SDK运行调试日志以及输出路径。 默认情况下，SDK运行调试日志处于打开状态。 用户可以手动关闭它。
也可以通过Locat过滤“ sc-ble-log”标签，以显示SDK特定的日志。

```java
    /**
     * 有两种配置日志的方法
     * 1. {@link Config.Builder#logWriter(Writer)}
     * 2. {@link Config.Builder#logFilePath(String)}
     */
    LogUtils.LOG_SWITCH=true;
    Config config = new Config.Builder().logWriter(logWriter).build();
    //Config config = new Config.Builder().logFilePath(logFilePath).build();
    scPeripheralManager.init(getContext(), config);
```

### 错误码说明

每次调用SDK接口时，都可能获得正确或不正确的返回码。 开发人员可以根据返回码信息调试接口并排除错误。

| Code				|  Description 		|
| -------------------- 	| ------------  |
| -1&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;| 未知错误，SDK无法处理该错误 |
| -2 					| macAddress错误 |
| -3					| 系统位置开关未打开 |
| -4 					| 应用缺少位置权限 |
| -5 					| 蓝牙未打开 |
| -6					| 设备当前不支持此命令 |
| -7					| 设备未连接 |
| -8					| 当前连接的设备数超过上限（最多5个） |

### Q&A
- 如果在连接温度计后未发送SYNC_DATA命令，为什么会收到onReceveData回调
> 温度计成功连接到SDK之后，它将通过“ SYNC_DATA命令”主动获取脱机数据，通过onReceveData进行回调，并在获取数据后通过“ SYNC_TIME命令”将系统时间与温度计同步。
- SDK指令调用顺序
> 未连接温度计时，发送命令将直接返回错误代码。 在连接过程中发送命令时，连接成功后将调用命令队列中的命令。 SDK一次处理一条指令，并同时发送多条相同的指令，并且SDK仅执行一次。 同时发送多个不同的指令，SDK将存储在指令队列中并按顺序执行。
- ACK命令
> 接收温度数据时，SDK需要回复温度计的“ SYNC_ACK命令”，并携带接收到的电流编号。 当同步温度数据大于10时，它将被分为10个数据组。 通常，该指令由SDK内部处理，不需要其他处理。 SDK发送此命令可能会导致收到重复的温度数据。
如果您真的想从外部调用ACK指令，则需要修改Config，新建Config.Builder（）。forceOutsideSendAck（true），然后调用scPeripheralManager.sendPeripheralCommand（macAddress，BleCommand.SEND_TEMP_ACK，scPeripheralDataList.size());
- SDK操作需要在UI线程中
> 当前，当SDK回调数据时，它将主动切换到UI线程
- 无法设置温度单位，无法同步时间
> 无法发送指令通常是因为未连接温度计或连接不稳定。 建议检查SDK日志输出以解决问题。
- 温度计始终获取重复温度
> 通常体温计中有异常的温度数据，这会导致SDK处理温度错误。 您可以联系SDK提供商程序进行咨询，需要发送日志文件。

## SDK介绍

### ScPeripheralManager
```java
public class ScPeripheralManager {

    /**
     * 开始扫描附近的设备，并通过此{@link ScanResultListener＃onScannerResult（List）}调用设备列表。
     * 该方法调用将每秒触发一次，直到调用{@link #stopScan()}方法为止。
     *
     * @param scanResultListener
     */
    public void startScan(ScanResultListener scanResultListener) {
        BleManager.getInstance().startScanAllDevice(scanResultListener);
    }

    /**
     * 停止扫描设备
     */
    public void stopScan() {
        BleManager.getInstance().stopScanDevice();
    }

    /**
     * 将Listener添加到一组侦听器，这些Listener将在设备连接期间发送事件。
     * 当想断开设备的连接时，需要主动调用{@link ScPeripheralManager＃removeReceiveDataListener（ReceiveDataListener）}
     *
     * @param receiveDataListener将被添加到{@link com.ikangtai.bluetoothsdk.BleManager}的当前侦听器集中的侦听器。
     */
    public void addReceiveDataListener(ReceiveDataListener receiveDataListener) {
        BleManager.getInstance().addReceiveDataListener(receiveDataListener);
    }

    /**
     * 从集合中删除一个监听器，以监听{@link com.ikangtai.bluetoothsdk.BleManager}的数据更新。
     *
     * @param receiveDataListener，将从{@link com.ikangtai.bluetoothsdk.BleManager}的当前更新侦听器集中删除的侦听器。
     *
     */
    public void removeReceiveDataListener(ReceiveDataListener receiveDataListener) {
        BleManager.getInstance().removeReceiveDataListener(receiveDataListener);
    }

    /**
     * 连接设备，需要设置全局侦听器{@link #addReceiveDataListener（ReceiveDataListener）}侦听数据更新。
     *
     * @param macAddress
     */
    public void connectPeripheral(String macAddress) {
        BleManager.getInstance().startConnectDevice(macAddress, null);
    }

    /**
     * 连接设备，将侦听器添加到侦听器集
     * 发送给{@link com.ikangtai.bluetoothsdk.BleManager}的数据更新事件。
     *
     * @param macAddress
     * @param receiveDataListener
     */

    public void connectPeripheral(String macAddress, ReceiveDataListener receiveDataListener) {
        BleManager.getInstance().startConnectDevice(macAddress, receiveDataListener);
    }

    /**
     * 发送设备命令
     *
     * @param type
     */
    public void sendPeripheraleCommand(String macAddress, int type) {
        BleManager.getInstance().sendDeviceCommand(macAddress, type);
    }

    /**
     * 获取设备连接状态。
     *
     * @param macAddress
     * @return 获取连接状态{@link android.bluetooth.BluetoothProfile}
     */
    public int getConnectState(String macAddress) {
        return BleManager.getInstance().getConnectState(macAddress);
    }

    /**
     * 断开设备
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
     * 断开所有设备
     */
    public void disconnectPeripheral() {
        BleManager.getInstance().disconnect();
    }
}

```