package com.example.blesdkdemo.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.blesdkdemo.BleApplication;
import com.example.blesdkdemo.Constant;
import com.example.blesdkdemo.R;
import com.example.blesdkdemo.activity.BindDeviceActivity;
import com.example.blesdkdemo.activity.InfoActivity;
import com.example.blesdkdemo.adapter.DeviceListAdapter;
import com.example.blesdkdemo.databinding.FragmentHomeBinding;
import com.example.blesdkdemo.txy.BleActivity;
import com.example.blesdkdemo.util.CheckBleFeaturesUtil;
import com.ikangtai.bluetoothsdk.Config;
import com.ikangtai.bluetoothsdk.ScPeripheralManager;
import com.ikangtai.bluetoothsdk.info.HardwareInfo;
import com.ikangtai.bluetoothsdk.listener.ScanResultListener;
import com.ikangtai.bluetoothsdk.model.HardwareModel;
import com.ikangtai.bluetoothsdk.model.ScPeripheral;
import com.ikangtai.bluetoothsdk.util.BleTools;
import com.ikangtai.bluetoothsdk.util.FileUtil;
import com.ikangtai.bluetoothsdk.util.ToastUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

/**
 * Scan for nearby Bluetooth devices
 *
 * @author xiongyl 2020/9/24 0:55
 */
public class HomeFragment extends Fragment {
    private ScPeripheralManager scPeripheralManager;
    private List<ScPeripheral> mDeviceList = new ArrayList<>();
    private FragmentHomeBinding fragmentHomeBinding;
    private HomeViewModel homeViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        fragmentHomeBinding = FragmentHomeBinding.inflate(inflater, container, false);
        homeViewModel = new ViewModelProvider(this, new ViewModelProvider.AndroidViewModelFactory(getActivity().getApplication())).get(HomeViewModel.class);
        fragmentHomeBinding.setModel(homeViewModel);
        fragmentHomeBinding.setLifecycleOwner(this);
        return fragmentHomeBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View contentView, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(contentView, savedInstanceState);

        scPeripheralManager = ScPeripheralManager.getInstance();
        String logFilePath = new File(FileUtil.createRootPath(getContext()), "bleSdkLog.txt").getAbsolutePath();
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
        Config config = new Config.Builder().logWriter(logWriter).scanMode(Config.Builder.SCAN_MODE_LOW_LATENCY).build();
        //Config config = new Config.Builder().logFilePath(logFilePath).build();
        //sdk init
        scPeripheralManager.init(getContext(), Constant.appId, Constant.appSecret, Constant.unionId, config);
        //Scan nearby Bluetooth devices
        fragmentHomeBinding.searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!CheckBleFeaturesUtil.checkBleFeatures(HomeFragment.this)) {
                    return;
                }
                homeViewModel.getIsSearching().setValue(true);
                homeViewModel.getIsConnecting().setValue(false);
                homeViewModel.getIsConnect().setValue(false);
                scPeripheralManager.startScan(new ScanResultListener() {
                    @Override
                    public void onScannerResult(List<ScPeripheral> deviceList) {
                        if (deviceList != null) {
                            for (int i = 0; i < deviceList.size(); i++) {
                                ScPeripheral scBluetoothDevice = deviceList.get(i);
                                if (!mDeviceList.contains(scBluetoothDevice)) {
                                    mDeviceList.add(scBluetoothDevice);
                                    fragmentHomeBinding.recyclerView.getAdapter().notifyItemInserted(mDeviceList.size() - 1);
                                }
                            }
                        }
                    }
                });
                bleTimeOut();
            }
        });
        //Stop scan devices
        fragmentHomeBinding.stopSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeViewModel.getIsSearching().setValue(false);
                scPeripheralManager.stopScan();
            }
        });

        DeviceListAdapter deviceListAdapter = new DeviceListAdapter(mDeviceList);
        deviceListAdapter.setItemClickListener(new DeviceListAdapter.ItemClickListener() {
            @Override
            public void onClick(ScPeripheral scPeripheral) {
                homeViewModel.getIsSearching().setValue(false);
                scPeripheralManager.stopScan();
                if (scPeripheral.getDeviceType() == BleTools.TYPE_UNKNOWN) {
                    appendConsoleContent(getString(R.string.unsupported_device));
                    return;
                }
                HardwareInfo hardwareInfo = HardwareInfo.toHardwareInfo(scPeripheral);
                List<HardwareInfo> bindHardwareInfoList = HardwareModel.hardwareList(getContext());
                if (!bindHardwareInfoList.contains(hardwareInfo)) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setTitle(R.string.tips)
                            .setMessage(R.string.bind_device).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    startActivity(new Intent(getContext(), BindDeviceActivity.class));
                                }
                            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });
                    builder.create().show();
                    return;
                }
                String macAddress = scPeripheral.getMacAddress();
                BleApplication.getInstance().appPreferences.saveLastDeviceAddress(macAddress);
                Intent intent;
                if (scPeripheral.getDeviceType() == BleTools.TYPE_LJ_TXY || scPeripheral.getDeviceType() == BleTools.TYPE_LJ_TXY_168) {
                    intent = new Intent(getContext(), BleActivity.class);
                } else {
                    intent = new Intent(getContext(), InfoActivity.class);
                }
                intent.putExtra("connectDevice", scPeripheral);
                startActivity(intent);
            }
        });
        fragmentHomeBinding.recyclerView.setAdapter(deviceListAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        fragmentHomeBinding.recyclerView.setLayoutManager(layoutManager);
    }

    private void appendConsoleContent(String massage) {
        ToastUtils.show(getContext(), massage);
    }

    private void bleTimeOut() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                if (homeViewModel.getIsSearching().getValue()) {
                    homeViewModel.getIsSearching().setValue(false);
                    scPeripheralManager.stopScan();
                    if (mDeviceList.isEmpty()) {
                        appendConsoleContent(getString(R.string.ble_scan_timeout));
                    }
                }
            }
        }, 200000);
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        CheckBleFeaturesUtil.handBleFeaturesResult(getContext(), requestCode, resultCode);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (scPeripheralManager != null) {
            scPeripheralManager.stopScan();
            scPeripheralManager.disconnectPeripheral();
        }
    }
}