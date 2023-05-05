package com.example.blesdkdemo.view.dialog;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.DownloadManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.widget.TextView;

import com.example.blesdkdemo.AppInfo;
import com.example.blesdkdemo.R;
import com.example.blesdkdemo.util.OadFileUtil;
import com.example.blesdkdemo.util.OtaFileUtil;
import com.example.blesdkdemo.view.RoundProgressBar;
import com.ikangtai.bluetoothsdk.BleCommand;
import com.ikangtai.bluetoothsdk.ScPeripheralManager;
import com.ikangtai.bluetoothsdk.http.respmodel.CheckFirmwareVersionResp;
import com.ikangtai.bluetoothsdk.info.HardwareInfo;
import com.ikangtai.bluetoothsdk.listener.ReceiveDataListenerAdapter;
import com.ikangtai.bluetoothsdk.listener.ScanResultListener;
import com.ikangtai.bluetoothsdk.model.BleCommandData;
import com.ikangtai.bluetoothsdk.model.HardwareModel;
import com.ikangtai.bluetoothsdk.model.ScPeripheral;
import com.ikangtai.bluetoothsdk.model.ScPeripheralData;
import com.ikangtai.bluetoothsdk.util.BleCode;
import com.ikangtai.bluetoothsdk.util.BleTools;
import com.ikangtai.bluetoothsdk.util.LogUtils;

import java.io.File;
import java.util.List;

import androidx.appcompat.app.AppCompatDialog;


/**
 * 固件升级dialog
 */
public class FirmwareUpdateDialog extends BaseShecareDialog {
    private static final int GET_FMV_SUCCESS = 0;
    private static final int GET_FMV_FAILURE = 1;
    private Context context;
    private RoundProgressBar roundProgressBar;
    private TextView updateTipsTv;
    private IEvent event;
    private HardwareInfo hardwareInfo;
    private CheckFirmwareVersionResp.Data versionData;

    private ScPeripheralManager scPeripheralManager;
    private ReceiveDataListenerAdapter receiveDataListenerAdapter;
    private OadFileUtil oadFileUtil;
    private OtaFileUtil otaFileUtil;
    public boolean OAD_COMPLETE = false;
    private int progressNum;
    private String latestNetFirmVer;
    private boolean mProgramming;
    private boolean isConnected;

    public FirmwareUpdateDialog(Context context, HardwareInfo hardwareInfo, CheckFirmwareVersionResp.Data data) {
        this.context = context;
        this.hardwareInfo = hardwareInfo;
        this.versionData = data;
    }

    private void initBleSdk() {
        scPeripheralManager = ScPeripheralManager.getInstance();
        receiveDataListenerAdapter = new ReceiveDataListenerAdapter() {
            @Override
            public void onReceiveData(String macAddress, List<ScPeripheralData> scPeripheralDataList) {

            }

            @Override
            public void onReceiveCommandData(String macAddress, int type, int resultCode, String value) {
                super.onReceiveCommandData(macAddress, type, resultCode, value);
                if (!TextUtils.equals(macAddress, hardwareInfo.getHardMacId())) {
                    return;
                }
                if (resultCode == BleCommand.ResultCode.RESULT_FAIL) {
                    return;
                }
                switch (type) {
                    case BleCommand.GET_FIRMWARE_VERSION:
                        if (OAD_COMPLETE && !TextUtils.isEmpty(value)) {
                            String firmwareVerUsing = value;
                            if (oadFileUtil != null) {
                                if (TextUtils.equals(firmwareVerUsing, oadFileUtil.getLatestVer()) || versionData.isMockData()) {
                                    LogUtils.i("检查固件升级成功:" + firmwareVerUsing);
                                    showUpgradeState(context.getString(R.string.oad_suceess),
                                            context.getString(R.string.ok));
                                    upgradeOADSuccess();
                                } else {
                                    LogUtils.i("检查固件升级失败:" + firmwareVerUsing);
                                    showUpgradeState(context.getString(R.string.oad_fail) + "\n" + context.getString(R.string.firmware_update_content),
                                            context.getString(R.string.ok));
                                }
                            } else if (otaFileUtil != null) {
                                if (TextUtils.equals(firmwareVerUsing, otaFileUtil.getLatestVer()) || versionData.isMockData()) {
                                    LogUtils.i("检查OTA升级成功:" + firmwareVerUsing);
                                    showUpgradeState(context.getString(R.string.oad_suceess),
                                            context.getString(R.string.ok));
                                    upgradeOADSuccess();
                                } else {
                                    LogUtils.i("检查OTA升级失败:" + firmwareVerUsing);
                                    showUpgradeState(context.getString(R.string.oad_fail) + "\n" + context.getString(R.string.firmware_update_content),
                                            context.getString(R.string.ok));
                                }
                            }
                        }

                        break;
                    case BleCommand.GET_THERMOMETER_OAD_IMG_TYPE:
                        if (!TextUtils.isEmpty(value)) {
                            oadFileUtil.handleFirmwareImgABMsg(Integer.valueOf(value));
                        }
                        break;
                    case BleCommand.THERMOMETER_OAD_UPGRADE:
                        switch (resultCode) {
                            case BleCommand.ResultCode.RESULT_OAD_START:
                                progressNum = 0;
                                displayStats();
                                updateGui();
                                break;
                            case BleCommand.ResultCode.RESULT_OAD_PROGRESS:
                                progressNum = Integer.valueOf(value);
                                displayStats();
                                break;
                            case BleCommand.ResultCode.RESULT_OAD_END:
                                progressNum = 100;
                                displayStats();
                                break;
                            case BleCommand.ResultCode.RESULT_OAD_FAIL:
                                String errorMessage = context.getString(R.string.oad_fail);
                                if (!TextUtils.isEmpty(value)) {
                                    errorMessage = BleCode.oadErrors.get(Integer.decode(value));
                                }
                                updateConnect(errorMessage);
                                break;
                        }
                    case BleCommand.THERMOMETER_OTA_UPGRADE:
                        switch (resultCode) {
                            case BleCommand.ResultCode.RESULT_OTA_START:
                                progressNum = 0;
                                displayStats();
                                updateGui();
                                break;
                            case BleCommand.ResultCode.RESULT_OTA_PROGRESS:
                                progressNum = Float.valueOf(value).intValue();
                                displayStats();
                                break;
                            case BleCommand.ResultCode.RESULT_OTA_END:
                                progressNum = 100;
                                displayStats();
                                break;
                            case BleCommand.ResultCode.RESULT_OTA_FAIL:
                                String errorMessage = context.getString(R.string.oad_fail);
                                if (!TextUtils.isEmpty(value)) {
                                    errorMessage = BleCode.oTaErrors.get(Integer.decode(value));
                                }
                                updateConnect(errorMessage);
                                break;
                        }
                        break;
                }
            }

            @Override
            public void onConnectionStateChange(String macAddress, int state) {
                super.onConnectionStateChange(macAddress, state);
                if (!TextUtils.equals(macAddress, hardwareInfo.getHardMacId())) {
                    return;
                }
                if (state == BluetoothProfile.STATE_CONNECTED) {
                    LogUtils.i("The device is connected " + macAddress);
                    AppInfo.getInstance().setThermometerState(true);
                    if (OAD_COMPLETE && versionData.isMockData() && versionData.getType() == 3) {
                        LogUtils.i("检查固件升级成功");
                        showUpgradeState(context.getString(R.string.oad_suceess),
                                context.getString(R.string.ok));
                        upgradeOADSuccess();
                    }
                } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                    LogUtils.i("Device disconnected " + macAddress);
                    AppInfo.getInstance().setThermometerState(false);
                    if (OAD_COMPLETE) {
                        restartScan();
                    }
                }
                refreshBleSate();
            }
        };
        scPeripheralManager.addReceiveDataListener(receiveDataListenerAdapter);
    }

    private Handler handler;
    private boolean mScanning;
    private Runnable scanRunnable = new Runnable() {
        @Override
        public void run() {
            scanLeDevice();
        }
    };

    public void restartScan() {
        stopScan();
        startScan();
    }

    public void startScan() {
        if (!mScanning) {
            if (handler == null) {
                handler = new Handler();
            }
            handler.postDelayed(scanRunnable, 1500);
        }
    }

    public void stopScan() {
        mScanning = false;
        LogUtils.i("Stop scanning");
        if (handler != null) {
            handler.removeCallbacks(scanRunnable);
        }
        try {
            scPeripheralManager.stopScan();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void scanLeDevice() {
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
                        String deviceAddr = scBluetoothDevice.getMacAddress();
                        if (TextUtils.equals(deviceAddr, hardwareInfo.getHardMacId())) {
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

    public FirmwareUpdateDialog builder() {
        View view = LayoutInflater.from(context).inflate(
                R.layout.layout_dialog_firmware_update, null);
        roundProgressBar = view.findViewById(R.id.firmware_update_progress);
        updateTipsTv = view.findViewById(R.id.firmware_update_title);
        WindowManager windowManager = (WindowManager) context
                .getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        view.setMinimumWidth((int) (display.getWidth() * 0.9));
        // 定义Dialog布局和参数
        dialog = new AppCompatDialog(context, R.style.BleAlertDialogStyle);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(view);
        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                AppInfo.getInstance().setOADConnectActive(false);
                stopScan();
                context.unregisterReceiver(downloadReceiver);
                scPeripheralManager.removeReceiveDataListener(receiveDataListenerAdapter);
                if (event != null) {
                    event.onDismiss();
                }
            }
        });
        AppInfo.getInstance().setOADConnectActive(true);
        initBleSdk();
        context.registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        LogUtils.i("获取设备当前固件版本号 = " + hardwareInfo.getHardwareVersion() + ", 开始获取网络上对应固件最新的版本号!");
        if (hardwareInfo.getHardType() == HardwareInfo.HARD_TYPE_THERMOMETER || hardwareInfo.getHardType() == HardwareInfo.HARD_TYPE_TXY || versionData.isMockData()) {
            refreshBleSate();
        } else {
            LogUtils.i("检查固件升级失败:不支持");
            showUpgradeState(String.format(context.getString(R.string.oad_unSupported), hardwareInfo.getHardwareVersion()),
                    context.getString(R.string.ok));
        }
        return this;
    }

    public FirmwareUpdateDialog show() {
        if (dialog != null) {
            dialog.show();
        }
        return this;
    }

    public FirmwareUpdateDialog setCancelable(boolean cancel) {
        dialog.setCancelable(cancel);
        return this;
    }

    public FirmwareUpdateDialog setCanceledOnTouchOutside(boolean cancel) {
        dialog.setCanceledOnTouchOutside(cancel);
        return this;
    }

    public void checkVersion() {
        int type = versionData.getType();
        String downloadURL = versionData.getFileUrl();
        String latestVer = versionData.getVersion();
        if (type == 1) {
            oadFileUtil = new OadFileUtil(context, latestVer, downloadURL);
            handleOADFirmVerMsg(GET_FMV_SUCCESS, latestVer);
            LogUtils.i("获取OAD版本信息成功: version" + latestVer + "  url:" + downloadURL);
        } else if (type == 2) {
            otaFileUtil = new OtaFileUtil(context, latestVer, downloadURL);
            handleOADFirmVerMsg(GET_FMV_SUCCESS, latestVer);
            LogUtils.i("获取OTA版本信息成功: version" + latestVer + "  url:" + downloadURL);
        } else if (type == 3) {
            otaFileUtil = new OtaFileUtil(context, latestVer, downloadURL);
            handleOADFirmVerMsg(GET_FMV_SUCCESS, latestVer);
            LogUtils.i("获取OTA版本信息成功: version" + latestVer + "  url:" + downloadURL);
        } else {
            LogUtils.i("检查固件升级失败:不支持");
            showUpgradeState(String.format(context.getString(R.string.oad_unSupported), hardwareInfo.getHardwareVersion()),
                    context.getString(R.string.ok));
        }
    }

    public void handleOADFirmVerMsg(int respCode, String latestVer) {
        if (respCode == GET_FMV_SUCCESS) {
            checkFirmVerNeedOAD(hardwareInfo.getHardwareVersion(), latestVer);
        } else {
            updateConnect(context.getString(R.string.get_firmver_fail) + "\n" + context.getString(R.string.firmware_update_content));
        }
    }

    /**
     * 检查是否有最新OAD供用户下载使用
     *
     * @param firmVerUsing
     * @param netFirmVer
     */
    private void checkFirmVerNeedOAD(String firmVerUsing, String netFirmVer) {
        double firmwareVerUsing = Double.valueOf(firmVerUsing);
        double netFirmwareVer = Double.valueOf(netFirmVer);
        /**
         *  对于 1代固件 只支持 >= 1.6版本 才能 OAD
         *  对于 2代固件 只支持 >= 2.96版本 才能 OAD
         *  对于 3代固件 均可 OAD
         */
        if (hardwareInfo.getHardHardwareType() == HardwareInfo.HW_GENERATION_1) {
            updateConnect(String.format(context.getString(R.string.oad_unSupported), firmVerUsing));
        } else if (hardwareInfo.getHardHardwareType() == HardwareInfo.HW_GENERATION_2) {
            updateConnect(String.format(context.getString(R.string.oad_unSupported),
                    firmVerUsing));
        } else if (netFirmwareVer - firmwareVerUsing > 0.00001) {
            updateConnect(String.format(context.getString(R.string.need_oad),
                    netFirmVer));
            latestNetFirmVer = netFirmVer;
            if (versionData.isMockData() || versionData.getType() == 3) {
                if (otaFileUtil != null) {
                    otaFileUtil.handleFirmwareImgMsg();
                } else if (oadFileUtil != null) {
                    oadFileUtil.handleFirmwareImgABMsg(0);
                } else {
                    updateConnect(String.format(context.getString(R.string.oad_unSupported),
                            firmVerUsing));
                }
            } else {
                if (otaFileUtil != null) {
                    otaFileUtil.handleFirmwareImgMsg();
                } else if (oadFileUtil != null) {
                    scPeripheralManager.sendPeripheralCommand(hardwareInfo.getHardMacId(), BleCommand.GET_THERMOMETER_OAD_IMG_TYPE);
                } else {
                    updateConnect(String.format(context.getString(R.string.oad_unSupported),
                            firmVerUsing));
                }
            }
        } else {
            //用户版本已是最新
            updateConnect(context.getString(R.string.already_latest_ver));
        }

    }

    private void refreshBleSate() {
        if (AppInfo.getInstance().isThermometerState()) {
            if (!OAD_COMPLETE) {
                updateConnect(context.getString(R.string.oad_check_ver));
                checkVersion();
            }
        } else {
            if (!OAD_COMPLETE) {
                if (progressNum >= 99) {
                    LogUtils.i("固件升级过程中断开体温计升级成功进度100");
                    showUpgradeState(context.getString(R.string.oad_suceess),
                            context.getString(R.string.ok));
                    //upgradeOADSuccess();
                } else {
                    LogUtils.i("固件升级过程中断开体温计");
                    if (startAnimator != null) {
                        startAnimator.cancel();
                    }
                    updateConnect(context.getString(R.string.oad_disconnected));
                    showUpgradeState(context.getString(R.string.oad_disconnected) + "\n" + context.getString(R.string.firmware_update_content),
                            context.getString(R.string.ok));
                }
            }
        }
    }

    public void updateProgress(int progress) {
        if (roundProgressBar != null) {
            roundProgressBar.setProgress(progress);
        }
    }

    public void updateConnect(String content) {
        if (updateTipsTv != null) {
            updateTipsTv.setText(content);
        }
    }

    private void updateGui() {
        if (mProgramming) {
            updateConnect(context.getString(R.string.firmware_update_title));
        } else {
            updateProgress(0);
        }
    }

    private void displayStats() {
        if (progressNum < 99) {
            mProgramming = true;
        } else {
            mProgramming = false;
        }
        updateProgress(progressNum);
        if (progressNum >= 99) {
            // 检查是否升级成功, 在连接断开之后 若OAD_COMPLETE = true, 则 5s 之后再次扫描, 连接, 读取版本号
            OAD_COMPLETE = true;
            updateConnect(context.getString(R.string.oad_check_succ_g23));
        }
    }

    public void showUpgradeState(String content, String confirmContent) {
        new BleAlertDialog(context).builder()
                .setTitle(context.getString(R.string.tips))
                .setMsg(content, Gravity.LEFT)
                .setCancelable(false)
                .setCanceledOnTouchOutside(false)
                .setPositiveButton(confirmContent, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        FirmwareUpdateDialog.this.dissmiss();
                    }
                }).show();
    }

    /**
     * 升级OAD版本成功，同步到服务器
     */
    public void upgradeOADSuccess() {
        if (!TextUtils.isEmpty(hardwareInfo.getHardMacId())) {
            LogUtils.i("固件升级成功,将升级后的信息进行同步 latestNetFirmVer:" + latestNetFirmVer);
            hardwareInfo.setHardwareVersion(latestNetFirmVer);
            HardwareModel.updateHardwareInfo(context, hardwareInfo);
        }
    }

    private BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -10001);
                if (oadFileUtil != null && oadFileUtil.getDownloadId() == downloadId) {
                    LogUtils.i("The OAD binary file download is complete, and the DFU upgrade begins!");
                    String filePath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + OadFileUtil.getFileName(oadFileUtil.getOadFileType(), oadFileUtil.getLatestVer());
                    File imgFile = new File(filePath);
                    if (downloadId != -10001) {
                        String filePathTemp = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + OadFileUtil.getFileNameTemp(oadFileUtil.getOadFileType(), oadFileUtil.getLatestVer());
                        new File(filePathTemp).renameTo(imgFile);
                    }
                    if (!new File(filePath).exists()) {
                        LogUtils.i("OADMainActivity OAD, download file fail");
                        return;
                    }
                    LogUtils.i("OADMainActivity OAD, filePath = " + filePath);
                    if (versionData.isMockData()) {
                        mockUpgrade();
                    } else {
                        BleCommandData bleCommandData = new BleCommandData();
                        bleCommandData.setOadImgFilepath(filePath);
                        scPeripheralManager.sendPeripheralCommand(hardwareInfo.getHardMacId(), BleCommand.THERMOMETER_OAD_UPGRADE, bleCommandData);
                    }
                } else if (otaFileUtil != null && otaFileUtil.getDownloadId() == downloadId) {
                    LogUtils.i("The OTA binary file download is complete, and the DFU upgrade begins!");
                    String filePath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + OtaFileUtil.getFileName(otaFileUtil.getLatestVer());
                    File imgFile = new File(filePath);
                    if (downloadId != -10001) {
                        String filePathTemp = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + OtaFileUtil.getFileNameTemp(otaFileUtil.getLatestVer());
                        new File(filePathTemp).renameTo(imgFile);
                    }
                    if (!new File(filePath).exists()) {
                        LogUtils.i("OADMainActivity OTA, download file fail");
                        return;
                    }
                    LogUtils.i("OADMainActivity OTA, filePath = " + filePath);
                    if (versionData.isMockData() || versionData.getType() == 3) {
                        mockUpgrade();
                    } else {
                        BleCommandData bleCommandData = new BleCommandData();
                        bleCommandData.setOtaTime(3);
                        bleCommandData.setOtaImgFilepath(filePath);
                        scPeripheralManager.sendPeripheralCommand(hardwareInfo.getHardMacId(), BleCommand.THERMOMETER_OTA_UPGRADE, bleCommandData);
                    }
                }
            }
        }
    };
    private ValueAnimator startAnimator;

    private void mockUpgrade() {
        progressNum = 0;
        displayStats();
        updateGui();
        try {
            ValueAnimator.class.getMethod("setDurationScale", float.class).invoke(null, 1f);
        } catch (Throwable t) {
            t.printStackTrace();
        }
        int percent = 100;
        startAnimator = ValueAnimator.ofInt(0, percent);
        startAnimator.setDuration(10000);
        startAnimator.setInterpolator(new AccelerateInterpolator());
        startAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                progressNum = (int) valueAnimator.getAnimatedValue();
                displayStats();
            }

        });
        startAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                progressNum = 100;
                displayStats();
            }
        });
        startAnimator.start();
    }

    public FirmwareUpdateDialog initEvent(IEvent event) {
        this.event = event;
        return this;
    }

    public interface IEvent {
        void onDismiss();
    }

}

