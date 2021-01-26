package com.example.blesdkdemo.ui;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.blesdkdemo.BleApplication;
import com.example.blesdkdemo.R;
import com.example.blesdkdemo.activity.InfoActivity;
import com.example.blesdkdemo.adapter.DeviceListAdapter;
import com.example.blesdkdemo.databinding.FragmentHomeBinding;
import com.example.blesdkdemo.txy.BleActivity;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.ikangtai.bluetoothsdk.Config;
import com.ikangtai.bluetoothsdk.ScPeripheralManager;
import com.ikangtai.bluetoothsdk.listener.ScanResultListener;
import com.ikangtai.bluetoothsdk.model.ScPeripheral;
import com.ikangtai.bluetoothsdk.util.BleTools;
import com.ikangtai.bluetoothsdk.util.FileUtil;
import com.ikangtai.bluetoothsdk.util.LogUtils;
import com.ikangtai.bluetoothsdk.util.ToastUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

/**
 * 扫描附近蓝牙设备
 *
 * @author xiongyl 2020/9/24 0:55
 */
public class HomeFragment extends Fragment {
    private ScPeripheralManager scPeripheralManager;
    private List<ScPeripheral> mDeviceList = new ArrayList<>();
    public final static int REQUEST_LOCATION_SETTINGS = 1000;
    public final static int REQUEST_BLE_SETTINGS_CODE = 1001;
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
        String logFilePath = new File(FileUtil.createRootPath(getContext()), "log_test.txt").getAbsolutePath();
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
        Config config = new Config.Builder().logWriter(logWriter).forceOutsideSendAck(false).build();
        //Config config = new Config.Builder().logFilePath(logFilePath).build();
        //sdk init
        scPeripheralManager.init(getContext(), config);

        //Register to receive Bluetooth switch broadcast
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(receiver, filter);
        //Scan nearby Bluetooth devices
        fragmentHomeBinding.searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkBleFeatures()) {
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
                String macAddress = scPeripheral.getMacAddress();
                BleApplication.getInstance().appPreferences.saveLastDeviceAddress(macAddress);
                Intent intent = null;
                if (scPeripheral.getDeviceType() == BleTools.TYPE_UNKNOWN) {
                    appendConsoleContent(getString(R.string.unsupported_device));
                    return;
                } else if (scPeripheral.getDeviceType() == BleTools.TYPE_LJ_TXY) {
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
        }, 20000);
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
            XXPermissions.with(getActivity())
                    .permission(Permission.Group.LOCATION)
                    .request(new OnPermission() {
                        @Override
                        public void hasPermission(List<String> granted, boolean isAll) {
                            if (isAll) {
                                //do something
                            }
                        }

                        @Override
                        public void noPermission(List<String> denied, boolean quick) {
                            if (quick) {
                                AlertDialog.Builder builder = new AlertDialog.Builder(getContext()).setTitle(R.string.tips)
                                        .setMessage(R.string.request_location_premisson).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                XXPermissions.gotoPermissionSettings(getContext());
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
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(receiver);
        if (scPeripheralManager != null) {
            scPeripheralManager.stopScan();
            scPeripheralManager.disconnectPeripheral();
        }
    }
}