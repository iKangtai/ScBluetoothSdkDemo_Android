package com.example.blesdkdemo.ui;

import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.blesdkdemo.R;
import com.example.blesdkdemo.databinding.FragmentInfoBinding;
import com.example.blesdkdemo.service.BleService;
import com.example.blesdkdemo.util.OadFileUtil;
import com.example.blesdkdemo.util.OtaFileUtil;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.ikangtai.bluetoothsdk.BleCommand;
import com.ikangtai.bluetoothsdk.ScPeripheralManager;
import com.ikangtai.bluetoothsdk.http.respmodel.CheckFirmwareVersionResp;
import com.ikangtai.bluetoothsdk.listener.CheckFirmwareVersionListener;
import com.ikangtai.bluetoothsdk.listener.ReceiveDataListenerAdapter;
import com.ikangtai.bluetoothsdk.model.BleCommandData;
import com.ikangtai.bluetoothsdk.model.ScPeripheral;
import com.ikangtai.bluetoothsdk.model.ScPeripheralData;
import com.ikangtai.bluetoothsdk.util.BleCode;
import com.ikangtai.bluetoothsdk.util.BleTools;
import com.ikangtai.bluetoothsdk.util.LogUtils;
import com.ikangtai.bluetoothsdk.util.ToastUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InfoFragment extends Fragment {
    private String macAddress;
    private ScPeripheralManager scPeripheralManager;
    public final static int REQUEST_LOCATION_SETTINGS = 1000;
    public final static int REQUEST_BLE_SETTINGS_CODE = 1001;
    private FragmentInfoBinding fragmentInfoBinding;
    private InfoViewModel infoViewModel;
    private Button btnSyncData, btnSyncTime, btnGetUnit, btnSyncUnitC, btnSyncUnitF, btnConnectState, btnDisconnect, btnGetTime, btnGetMeasureMode, btnSetMeasureMode1, btnSetMeasureMode2, btnSetMeasureMode3, btnGetPower, btnGetMeasureTime, btnSetMeasuringTime, btnSetPreheatTime, btnOtaUpgrade, btnGetHistoryData, btnClearHistoryData, btnOadUpgrade, btnDeviceInfo, btnDeviceOfflineTest, btnDeviceStartTest, btnDeviceReboot;
    private ProgressDialog dialog;
    private ScPeripheral scPeripheral;
    private OadFileUtil oadFileUtil;
    private OtaFileUtil otaFileUtil;
    private boolean startScan;
    private ReceiveDataListenerAdapter receiveDataListenerAdapter = new ReceiveDataListenerAdapter() {
        @Override
        public void onReceiveData(String macAddress, List<ScPeripheralData> scPeripheralDataList) {
            disMissProgress();
            appendConsoleContent("New data received " + macAddress);
            if (!scPeripheralDataList.isEmpty()) {
                if (scPeripheral.getDeviceType() == BleTools.TYPE_PAPER_BOX) {
                    for (ScPeripheralData temperature : scPeripheralDataList) {
                        if (temperature != null && temperature.getPaperResult() != null) {
                            appendConsoleContent(temperature.getDate() + "  " + temperature.getPaperResult().toString());
                        }
                    }
                } else {
                    for (ScPeripheralData temperature : scPeripheralDataList) {
                        if (temperature != null) {
                            StringBuffer result = new StringBuffer();
                            result.append("收到试纸结果：\n");
                            result.append(temperature.getDate());
                            result.append("\n");
                            result.append(temperature.getTemp());
                            appendConsoleContent(result.toString());
                            showErrorMessage(result.toString());
                        }
                    }
                }
            } else {
                appendConsoleContent(getString(R.string.not_data_record));
            }
        }

        @Override
        public void onReceiveError(String macAddress, int code, String msg) {
            /**
             * The code see {@link BleCode}
             */
            LogUtils.d("onReceiveError:" + code + "  " + msg);
            checkConnectDialog();
            appendConsoleContent("error in connecting " + macAddress);
            showErrorMessage(BleCode.getMessage(code));
        }

        @Override
        public void onConnectionStateChange(String macAddress, int state) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                appendConsoleContent("The device is connected " + macAddress);
                infoViewModel.getIsSearching().setValue(false);
                infoViewModel.getIsConnecting().setValue(false);
                infoViewModel.getIsConnect().setValue(true);
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                ToastUtils.show(getContext(), scPeripheral.getDeviceName() + " disconnected");
                appendConsoleContent("Device disconnected " + macAddress);
                infoViewModel.getIsSearching().setValue(false);
                infoViewModel.getIsConnecting().setValue(false);
                infoViewModel.getIsConnect().setValue(false);
                checkConnectDialog();
            }
        }

        @Override
        public void onReceiveCommandData(String macAddress, int type, int resultCode, String value) {
            super.onReceiveCommandData(macAddress, type, resultCode, value);
            /**
             * The type see {@link BleCommand}
             */
            LogUtils.d("onReceiveCommandData:" + type + "  " + resultCode + " " + value);
            appendConsoleContent(type + " command send resultCode:" + resultCode + " value:" + value);
            if (resultCode == BleCommand.ResultCode.RESULT_FAIL) {
                disMissProgress();
                String errorMessage = "send command failed, please restart the thermometer to start again";
                if (!TextUtils.isEmpty(value)) {
                    if (scPeripheral.getDeviceType() == BleTools.TYPE_PAPER_BOX) {
                        errorMessage = BleCode.getPaperBoxMessage(Integer.decode(value));
                    } else {
                        errorMessage = BleCode.getMessage(Integer.decode(value));
                    }
                }
                showErrorMessage(errorMessage);
            } else if (type == BleCommand.GET_FIRMWARE_VERSION) {
                disMissProgress();
                scPeripheral.setVersion(value);
            } else if (scPeripheral.getDeviceType() == BleTools.TYPE_AKY_3 || scPeripheral.getDeviceType() == BleTools.TYPE_AKY_4) {
                if (type == BleCommand.THERMOMETER_OTA_UPGRADE) {
                    switch (resultCode) {
                        case BleCommand.ResultCode.RESULT_OTA_START:
                            appendConsoleContent("device ota start");
                            break;
                        case BleCommand.ResultCode.RESULT_OTA_FAIL:
                            String errorMessage = "OTA failed, please restart the thermometer to start again";
                            if (!TextUtils.isEmpty(value)) {
                                errorMessage = BleCode.oTaErrors.get(Integer.decode(value));
                            }
                            showErrorMessage(errorMessage);
                            break;
                    }
                }
            } else if (scPeripheral.getDeviceType() == BleTools.TYPE_SMART_THERMOMETER) {
                if (type == BleCommand.GET_THERMOMETER_OAD_IMG_TYPE) {
                    appendConsoleContent("Found newer firmware version, download to update!");
                    oadFileUtil.handleFirmwareImgABMsg(Integer.valueOf(value));
                } else if (type == BleCommand.THERMOMETER_OAD_UPGRADE) {
                    switch (resultCode) {
                        case BleCommand.ResultCode.RESULT_OAD_START:
                            appendConsoleContent("device oad start");
                            break;
                        case BleCommand.ResultCode.RESULT_OAD_PROGRESS:
                            if (Integer.valueOf(value) < 99) {
                                appendConsoleContent(value + "% completed");
                            } else {
                                appendConsoleContent(value + "% completed,verifying the success of the upgrade...");
                            }
                            break;
                        case BleCommand.ResultCode.RESULT_OAD_END:
                            appendConsoleContent("device oad end");
                            showErrorMessage("OAD finish, please restart device to check the success of the upgrade...");
                            break;
                        case BleCommand.ResultCode.RESULT_OAD_FAIL:
                            String errorMessage = "OAD failed, please restart the thermometer to start again";
                            if (!TextUtils.isEmpty(value)) {
                                errorMessage = BleCode.oadErrors.get(Integer.decode(value));
                            }
                            showErrorMessage(errorMessage);
                            break;
                    }
                }
            } else if (scPeripheral.getDeviceType() == BleTools.TYPE_PAPER_BOX) {
                if (type == BleCommand.DEVICE_INFO_NOTIFY) {
                    String message = "";
                    int result = Integer.valueOf(value);
                    if (result == 1) {
                        message = "试剂卡插入";
                    } else if (result == 2) {
                        message = " 读取卡信息成功，并开始检测（离线模式生效）";
                        showProgress("正在测量...\n请勿拔卡");
                    }
                    appendConsoleContent(message);
                } else if (type == BleCommand.GET_DEVICE_INFO) {
                    if (value.contains(",")) {
                        // 当前检测状态（1 byte，0 未检测、1 检测中），
                        // 是否存在历史数据（1 byte，未同步的数据数量），
                        // 是否有检测卡（1 byte，0 已插卡、1 未插卡），
                        // 是否有检测id（1 byte，0 无检测 ID，1 有检测 ID），
                        // 反应时间（2 byte，孵育时间）
                        String[] result = value.split(",");
                        int state = Integer.valueOf(result[0]);
                        int hasHistory = Integer.valueOf(result[1]);
                        int hasPaper = Integer.valueOf(result[2]);
                        int hasId = Integer.valueOf(result[3]);
                        int testTime = Integer.valueOf(result[4]);
                        int leftTime = Integer.valueOf(result[5]);
                        appendConsoleContent(String.format("当前检测状态：%s\n存在历史数据：%d\n是否有检测卡：%s\n是否有检测id：%s\n反应时间：%d\n剩余时间：%d", state == 0 ? "未检测" : "检测中", hasHistory, hasPaper == 0 ? "已插卡" : "未插卡", hasId == 0 ? "无检测ID" : "有检测ID", testTime, leftTime));
                    }
                } else if (type == BleCommand.GET_DEVICE_DATA) {
                    showProgress("正在同步试纸数据...\n请勿拔卡");
                } else if (type == BleCommand.DEVICE_START_TEST) {
                    showProgress("正在测量...\n请勿拔卡");
                }
            }
        }
    };

    private void showErrorMessage(String errorMessage) {
        new AlertDialog.Builder(getContext())
                .setTitle("Tips")
                .setIcon(R.mipmap.ic_launcher_round)
                .setMessage(errorMessage)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    private void checkConnectDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
            getActivity().finish();
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentInfoBinding = FragmentInfoBinding.inflate(inflater, container, false);
        infoViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getActivity().getApplication())).get(InfoViewModel.class);
        fragmentInfoBinding.setModel(infoViewModel);
        fragmentInfoBinding.setLifecycleOwner(this);
        return fragmentInfoBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View contentView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(contentView, savedInstanceState);
        scPeripheral = (ScPeripheral) getActivity().getIntent().getSerializableExtra("connectDevice");
        macAddress = scPeripheral.getMacAddress();
        infoViewModel.getDeviceType().setValue(scPeripheral.getDeviceType());
        scPeripheralManager = ScPeripheralManager.getInstance();

        //Register to receive Bluetooth switch broadcast
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.APP_FOREGROUND_SCAN_RESULT);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getActivity().registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED);
            getActivity().registerReceiver(oadFileDownloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            getActivity().registerReceiver(receiver, intentFilter);
            getActivity().registerReceiver(oadFileDownloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
        initView();
        //Sync data from Bluetooth device
        if (btnSyncData != null)
            btnSyncData.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showProgress("");
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.GET_DEVICE_DATA);
                }
            });
        //Sync phone time to Bluetooth device
        if (btnSyncTime != null)
            btnSyncTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_TIME);
                }
            });
        if (btnGetUnit != null)
            btnGetUnit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.GET_THERMOMETER_UNIT);
                }
            });
        //Sync unit c to Bluetooth device
        if (btnSyncUnitC != null)
            btnSyncUnitC.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //set unit c
                    BleCommandData bleCommandData = new BleCommandData();
                    bleCommandData.setParam1(BleCommand.ThermometerUnit.THERMOMETER_UNIT_C);
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_THERMOMETER_UNIT, bleCommandData);
                }
            });
        //Sync unit f to Bluetooth device
        if (btnSyncUnitF != null)
            btnSyncUnitF.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //set unit f
                    BleCommandData bleCommandData = new BleCommandData();
                    bleCommandData.setParam1(BleCommand.ThermometerUnit.THERMOMETER_UNIT_F);
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_THERMOMETER_UNIT, bleCommandData);
                }
            });
        //Get the connection status of the device, the default is STATE_DISCONNECTED
        if (btnConnectState != null)
            btnConnectState.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int connectStatus = scPeripheralManager.getConnectState(macAddress);
                    if (connectStatus == BluetoothProfile.STATE_CONNECTED) {
                        appendConsoleContent("connected " + macAddress);
                    } else if (connectStatus == BluetoothProfile.STATE_DISCONNECTED) {
                        appendConsoleContent("not connected " + macAddress);
                    }
                }
            });
        //Bluetooth device needs to be disconnected after use
        if (btnDisconnect != null)
            btnDisconnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    infoViewModel.getIsSearching().setValue(false);
                    infoViewModel.getIsConnecting().setValue(false);
                    infoViewModel.getIsConnect().setValue(false);
                    scPeripheralManager.disconnectPeripheral(macAddress);
                }
            });


        if (btnGetTime != null)
            btnGetTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.GET_TIME);
                }
            });
        if (btnGetMeasureMode != null)
            btnGetMeasureMode.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.GET_THERMOMETER_MODE);
                }
            });
        if (btnSetMeasureMode1 != null)
            btnSetMeasureMode1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BleCommandData bleCommandData = new BleCommandData();
                    bleCommandData.setParam1(BleCommand.MeasureMode.PREDICTION);
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SET_THERMOMETER_MODE, bleCommandData);
                }
            });
        if (btnSetMeasureMode2 != null)
            btnSetMeasureMode2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BleCommandData bleCommandData = new BleCommandData();
                    bleCommandData.setParam1(BleCommand.MeasureMode.ORAL_CAVITY);
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SET_THERMOMETER_MODE, bleCommandData);
                }
            });
        if (btnSetMeasureMode3 != null)
            btnSetMeasureMode3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BleCommandData bleCommandData = new BleCommandData();
                    bleCommandData.setParam1(BleCommand.MeasureMode.ARMPIT);
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SET_THERMOMETER_MODE, bleCommandData);
                }
            });
        if (btnGetPower != null)
            btnGetPower.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.GET_POWER);
                }
            });
        if (btnGetMeasureTime != null)
            btnGetMeasureTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.GET_THERMOMETER_MEASURE_TIME);
                }
            });
        if (btnSetMeasuringTime != null)
            btnSetMeasuringTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    final EditText editText = new EditText(getContext());
                    editText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                    new AlertDialog.Builder(getContext()).setTitle(R.string.input_measure_time)
                            .setIcon(android.R.drawable.sym_def_app_icon)
                            .setView(editText)
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String value = editText.getText().toString();
                                    if (!TextUtils.isEmpty(value)) {
                                        BleCommandData bleCommandData = new BleCommandData();
                                        if (scPeripheral.getDeviceType() == BleTools.TYPE_PAPER_BOX) {
                                            bleCommandData.setValue(Integer.valueOf(value));
                                            scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SET_MEASURE_TIME, bleCommandData);
                                        } else {
                                            bleCommandData.setParam1(Integer.valueOf(value));
                                            scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SET_THERMOMETER_MEASURE_TIME2, bleCommandData);
                                        }
                                    }
                                }
                            }).setNegativeButton(getString(R.string.cancel), null).show();
                }
            });
        if (btnSetPreheatTime != null)
            btnSetPreheatTime.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final EditText editText = new EditText(getContext());
                    editText.setInputType(EditorInfo.TYPE_CLASS_NUMBER);
                    new AlertDialog.Builder(getContext()).setTitle(R.string.input_preheat_time)
                            .setIcon(android.R.drawable.sym_def_app_icon)
                            .setView(editText)
                            .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    String value = editText.getText().toString();
                                    if (!TextUtils.isEmpty(value)) {
                                        BleCommandData bleCommandData = new BleCommandData();
                                        bleCommandData.setParam1(Integer.valueOf(value));
                                        scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SET_THERMOMETER_MEASURE_TIME1, bleCommandData);
                                    }
                                }
                            }).setNegativeButton(getString(R.string.cancel), null).show();
                }
            });
        if (btnOtaUpgrade != null)
            btnOtaUpgrade.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!TextUtils.isEmpty(scPeripheral.getVersion())) {
                        boolean mockData = false;
                        scPeripheralManager.checkFirmwareVersion(scPeripheral, mockData, new CheckFirmwareVersionListener() {
                            @Override
                            public void checkSuccess(final CheckFirmwareVersionResp.Data data) {
                                if (Double.parseDouble(data.getVersion()) > Double.parseDouble(scPeripheral.getVersion())) {
                                    otaFileUtil = new OtaFileUtil(getContext(), data.getVersion(), data.getFileUrl());
                                    otaFileUtil.handleFirmwareImgMsg();
                                } else {
                                    showErrorMessage(getString(R.string.already_latest_ver));
                                }
                            }

                            @Override
                            public void checkFail() {
                                showErrorMessage(getString(R.string.already_latest_ver));
                            }
                        });
                    }
                }
            });
        if (btnGetHistoryData != null)
            btnGetHistoryData.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.GET_DEVICE_HISTORY_DATA);
                }
            });
        if (btnClearHistoryData != null)
            btnClearHistoryData.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (scPeripheral.getDeviceType() == BleTools.TYPE_PAPER_BOX) {
                        scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.CLEAR_DEVICE_DATA);
                    } else {
                        scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.CLEAR_THERMOMETER_DATA);
                    }
                }
            });
        if (btnOadUpgrade != null) {
            btnOadUpgrade.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scPeripheralManager.checkFirmwareVersion(scPeripheral, new CheckFirmwareVersionListener() {
                        @Override
                        public void checkSuccess(CheckFirmwareVersionResp.Data data) {
                            if (Double.parseDouble(data.getVersion()) > Double.parseDouble(scPeripheral.getVersion())) {
                                oadFileUtil = new OadFileUtil(getContext(), data.getVersion(), data.getFileUrl());
                                //get device current image type
                                scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.GET_THERMOMETER_OAD_IMG_TYPE);
                            } else {
                                showErrorMessage(getString(R.string.already_latest_ver));
                            }
                        }

                        @Override
                        public void checkFail() {
                            showErrorMessage(getString(R.string.already_latest_ver));
                        }
                    });

                }
            });
        }
        fragmentInfoBinding.connectDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showConnectDevice();
            }
        });
        if (btnDeviceInfo != null)
            btnDeviceInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.GET_DEVICE_INFO);
                }
            });
        if (btnDeviceOfflineTest != null)
            btnDeviceOfflineTest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.DEVICE_OFFLINE_TEST);
                }
            });
        if (btnDeviceStartTest != null)
            btnDeviceStartTest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.DEVICE_START_TEST);
                }
            });
        if (btnDeviceReboot != null)
            btnDeviceReboot.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.DEVICE_REBOOT);
                }
            });
        showConnectDevice();
    }

    private void showConnectDevice() {
        appendConsoleContent("Prepare to connect the device " + macAddress);
        if (!checkBleFeatures()) {
            return;
        }
        if (TextUtils.isEmpty(macAddress)) {
            return;
        }
        dialog = new ProgressDialog(getContext());
        dialog.setMessage("Connecting, please wait...");
        //dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                infoViewModel.getIsConnecting().setValue(false);
                getActivity().finish();
            }
        });
        dialog.show();
        infoViewModel.getIsConnecting().setValue(true);
        //Connect a Bluetooth device
        scPeripheralManager.connectPeripheral(macAddress, receiveDataListenerAdapter);
    }

    private void disMissProgress() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void showProgress(String message) {
        disMissProgress();
        dialog = new ProgressDialog(getContext());
        dialog.setMessage(message);
        //dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {

            }
        });
        dialog.show();
    }

    private void initView() {
        switch (scPeripheral.getDeviceType()) {
            case BleTools.TYPE_SMART_THERMOMETER:
                getView().findViewById(R.id.thermometer_menu_layout_vs).setVisibility(View.VISIBLE);
                break;
            case BleTools.TYPE_BASE_THERMOMETER:
                getView().findViewById(R.id.normal_thermometer_menu_layout_vs).setVisibility(View.VISIBLE);
                break;
            case BleTools.TYPE_EWQ:
                getView().findViewById(R.id.ewq_menu_layout_vs).setVisibility(View.VISIBLE);
                break;
            case BleTools.TYPE_AKY_3:
                getView().findViewById(R.id.aky3_menu_layout_vs).setVisibility(View.VISIBLE);
                break;
            case BleTools.TYPE_AKY_4:
                getView().findViewById(R.id.aky4_menu_layout_vs).setVisibility(View.VISIBLE);
                break;
            case BleTools.TYPE_IFEVER_TEM_TICK:
                getView().findViewById(R.id.tem_tick_menu_layout_vs).setVisibility(View.VISIBLE);
                break;
            case BleTools.TYPE_PAPER_BOX:
                getView().findViewById(R.id.paper_box_menu_layout_vs).setVisibility(View.VISIBLE);
                break;
        }
        btnSyncData = getView().findViewById(R.id.btn_sync_data);
        btnSyncTime = getView().findViewById(R.id.btn_sync_time);
        btnGetUnit = getView().findViewById(R.id.btn_get_unit);
        btnSyncUnitC = getView().findViewById(R.id.btn_sync_unit_c);
        btnSyncUnitF = getView().findViewById(R.id.btn_sync_unit_f);
        btnConnectState = getView().findViewById(R.id.btn_connect_state);
        btnDisconnect = getView().findViewById(R.id.btn_disconnect);
        btnGetTime = getView().findViewById(R.id.btn_get_time);
        btnGetMeasureMode = getView().findViewById(R.id.btn_get_measure_mode);
        btnSetMeasureMode1 = getView().findViewById(R.id.btn_set_measure_mode1);
        btnSetMeasureMode2 = getView().findViewById(R.id.btn_set_measure_mode2);
        btnSetMeasureMode3 = getView().findViewById(R.id.btn_set_measure_mode3);
        btnGetPower = getView().findViewById(R.id.btn_get_power);
        btnGetMeasureTime = getView().findViewById(R.id.btn_get_measure_time);
        btnSetMeasuringTime = getView().findViewById(R.id.btn_set_measuring_time);
        btnSetPreheatTime = getView().findViewById(R.id.btn_set_preheat_time);
        btnOtaUpgrade = getView().findViewById(R.id.btn_ota_upgrade);
        btnGetHistoryData = getView().findViewById(R.id.btn_get_history_data);
        btnClearHistoryData = getView().findViewById(R.id.btn_clear_history_data);
        btnOadUpgrade = getView().findViewById(R.id.btn_oad_upgrade);

        btnDeviceInfo = getView().findViewById(R.id.btn_device_info);
        btnDeviceOfflineTest = getView().findViewById(R.id.btn_device_offline_test);
        btnDeviceStartTest = getView().findViewById(R.id.btn_device_start_test);
        btnDeviceReboot = getView().findViewById(R.id.btn_device_reboot);
    }

    public void appendConsoleContent(String massage) {
        StringBuffer stringBuffer = infoViewModel.getConsoleContent().getValue();
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer();
        }
        stringBuffer.append(massage);
        stringBuffer.append("\n");
        infoViewModel.getConsoleContent().setValue(stringBuffer);
        fragmentInfoBinding.consoleScrollView.post(new Runnable() {
            @Override
            public void run() {
                fragmentInfoBinding.consoleScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    /**
     * Before the scan starts, you need to check the positioning service switch above 6.0, the positioning authority of the system above 6.0, and the Bluetooth switch
     *
     * @return
     */
    private boolean checkBleFeatures() {
        //Check Bluetooth Location Service
        if (!BleTools.isLocationEnable(getContext())) {
            Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivityForResult(locationIntent, REQUEST_LOCATION_SETTINGS);
            return false;
        }
        //Check Bluetooth location permission
        if (!BleTools.checkBlePermission(getContext())) {
            String[] permissions;
            if (BleTools.getTargetSdkVersionCode(getContext()) >= Build.VERSION_CODES.S && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                permissions = new String[]{Permission.BLUETOOTH_CONNECT, Permission.BLUETOOTH_SCAN, Permission.BLUETOOTH_ADVERTISE};
            } else {
                permissions = new String[]{Permission.ACCESS_COARSE_LOCATION, Permission.ACCESS_FINE_LOCATION};
            }
            XXPermissions.with(getActivity())
                    .permission(permissions)
                    .request(new OnPermissionCallback() {
                        @Override
                        public void onGranted(List<String> permissions, boolean all) {
                            if (all) {
                                //do something
                            }
                        }

                        @Override
                        public void onDenied(List<String> permissions, boolean never) {
                            if (never) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setTitle(R.string.tips)
                                        .setMessage(R.string.request_location_premisson).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                XXPermissions.startPermissionActivity(getContext());
                                            }
                                        }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                dialogInterface.dismiss();
                                            }
                                        });
                                builder.create().show();

                            } else {
                                appendConsoleContent(getString(R.string.request_location_premisson));
                            }
                        }
                    });
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

    private BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF) {
                    LogUtils.d("Bluetooth is off");
                    appendConsoleContent("Bluetooth off");
                } else if (state == BluetoothAdapter.STATE_ON) {
                    LogUtils.d("Bluetooth is on");
                    appendConsoleContent("Bluetooth is on");
                }
            } else if (TextUtils.equals(action, BleService.APP_FOREGROUND_SCAN_RESULT)) {
                scPeripheralManager.connectPeripheral(macAddress, receiveDataListenerAdapter);
            }
        }

    };

    private BroadcastReceiver oadFileDownloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(DownloadManager.ACTION_DOWNLOAD_COMPLETE)) {
                long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -10001);
                if (oadFileUtil != null && oadFileUtil.getDownloadId() == downloadId) {
                    LogUtils.i("The OAD binary file download is complete, and the DFU upgrade begins!");
                    String filePath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + OadFileUtil.getFileName(oadFileUtil.getOadFileType(), oadFileUtil.getLatestVer());
                    if (downloadId != -10001) {
                        String filePathTemp = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + OadFileUtil.getFileNameTemp(oadFileUtil.getOadFileType(), oadFileUtil.getLatestVer());
                        new File(filePathTemp).renameTo(new File(filePath));
                    }
                    if (!new File(filePath).exists()) {
                        LogUtils.i("OADMainActivity OAD, download file fail");
                        return;
                    }
                    LogUtils.i("OADMainActivity OAD, filePath = " + filePath);

                    BleCommandData bleCommandData = new BleCommandData();
                    bleCommandData.setOadImgFilepath(filePath);
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.THERMOMETER_OAD_UPGRADE, bleCommandData);
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

                    BleCommandData bleCommandData = new BleCommandData();
                    bleCommandData.setOtaTime(3);
                    bleCommandData.setOtaImgFilepath(filePath);
                    scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.THERMOMETER_OTA_UPGRADE, bleCommandData);
                }
            }
        }
    };

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_LOCATION_SETTINGS) {
            boolean openLocationServer = BleTools.isLocationEnable(getContext());
            if (openLocationServer) {
                LogUtils.e("Location service: The user manually sets the location service");
                appendConsoleContent(getString(R.string.location_service_turn_on));
            } else {
                LogUtils.e("Location service: The user manually set the location service is not enabled");
                appendConsoleContent(getString(R.string.location_service_turn_off));
            }
        } else if (requestCode == REQUEST_BLE_SETTINGS_CODE) {
            boolean enable = BleTools.isLocationEnable(getContext());
            if (!enable) {
                appendConsoleContent(getString(R.string.request_location_premisson_tips));
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (infoViewModel.getIsConnecting().getValue()) {
            if (scPeripheralManager != null) {
                scPeripheralManager.stopBackgroundScan();
                scPeripheralManager.connectPeripheral(macAddress, receiveDataListenerAdapter);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (infoViewModel.getIsConnecting().getValue()) {
            if (scPeripheralManager != null) {
                scPeripheralManager.stopScan();
                List<String> bindMacAddressList = new ArrayList<>();
                bindMacAddressList.add(macAddress);
                scPeripheralManager.startBackgroundScan(bindMacAddressList);
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(receiver);
        getActivity().unregisterReceiver(oadFileDownloadReceiver);
        disMissProgress();
        if (scPeripheralManager != null) {
            scPeripheralManager.stopScan();
            scPeripheralManager.stopBackgroundScan();
            scPeripheralManager.disconnectPeripheral();
        }
    }
}