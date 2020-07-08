package com.example.blesdkdemo;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.blesdkdemo.databinding.FragmentHomeBinding;
import com.hjq.permissions.OnPermission;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.ikangtai.bluetoothsdk.BleCommand;
import com.ikangtai.bluetoothsdk.Config;
import com.ikangtai.bluetoothsdk.ScPeripheralManager;
import com.ikangtai.bluetoothsdk.listener.ReceiveDataListenerAdapter;
import com.ikangtai.bluetoothsdk.listener.ScanResultListener;
import com.ikangtai.bluetoothsdk.model.ScPeripheral;
import com.ikangtai.bluetoothsdk.model.ScPeripheralData;
import com.ikangtai.bluetoothsdk.util.BleTools;
import com.ikangtai.bluetoothsdk.util.FileUtil;
import com.ikangtai.bluetoothsdk.util.LogUtils;

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

public class HomeFragment extends Fragment {
    private String macAddress;
    private ScPeripheralManager scPeripheralManager;
    private List<ScPeripheral> mDeviceList = new ArrayList<>();
    public final static int REQUEST_LOCATION_SETTINGS = 1000;
    public final static int REQUEST_BLE_SETTINGS_CODE = 1001;
    private FragmentHomeBinding fragmentHomeBinding;
    private HomeViewModel homeViewModel;
    private ReceiveDataListenerAdapter receiveDataListenerAdapter = new ReceiveDataListenerAdapter() {
        @Override
        public void onReceiveData(String macAddress, List<ScPeripheralData> scPeripheralDataList) {
            appendConsoleContent("New data received " + macAddress);
            if (!scPeripheralDataList.isEmpty()) {
                for (ScPeripheralData temperature : scPeripheralDataList) {
                    if (temperature != null) {
                        appendConsoleContent(temperature.getDate() + "  " + temperature.getTemp());
                    }
                }
            } else {
                appendConsoleContent(getString(R.string.not_data_record));
            }
        }

        @Override
        public void onReceiveError(String macAddress, int code, String msg) {
            Log.d("ble", code + "  " + msg);
            appendConsoleContent("error in connecting " + macAddress);
        }

        @Override
        public void onConnectionStateChange(String macAddress, int state) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                appendConsoleContent("The device is connected " + macAddress);
                homeViewModel.getIsSearching().setValue(false);
                homeViewModel.getIsConnecting().setValue(false);
                homeViewModel.getIsConnect().setValue(true);
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                appendConsoleContent("Device disconnected " + macAddress);
                homeViewModel.getIsSearching().setValue(false);
                homeViewModel.getIsConnecting().setValue(false);
                homeViewModel.getIsConnect().setValue(false);
            }
        }

        @Override
        public void onReceiveCommandData(String macAddress, int type, boolean state, String value) {
            Log.d("ble", type + "  " + state + " " + value);
            appendConsoleContent(type + " command sending " + state);
        }
    };

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
        Config config = new Config.Builder().logWriter(logWriter).build();
        //Config config = new Config.Builder().logFilePath(logFilePath).build();
        //sdk init
        scPeripheralManager.init(getContext(), config);

        //Register to receive Bluetooth switch broadcast
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(receiver, filter);

        // Get the address of a device that has been discovered
        macAddress = MyApplication.getInstance().appPreferences.getLastDeviceAddress();
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
//                                    if (scBluetoothDevice.getDeviceType() == BleTools.TYPE_UNKNOWN) {
//                                        continue;
//                                    }
                                    mDeviceList.add(scBluetoothDevice);
                                    fragmentHomeBinding.recyclerView.getAdapter().notifyItemInserted(mDeviceList.size() - 1);
                                }
                            }
                        }
                    }
                });
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

        //Sync data from Bluetooth device
        fragmentHomeBinding.btnSyncData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.GET_DEVICE_DATA);
            }
        });
        //Sync phone time to Bluetooth device
        fragmentHomeBinding.btnSyncTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_TIME);
            }
        });
        //Sync unit to Bluetooth device
        fragmentHomeBinding.btnSyncUnit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //set unit c
                scPeripheralManager.sendPeripheralCommand(macAddress, BleCommand.SYNC_THERMOMETER_UNIT_C);
                //set unit f
                //scBluetoothClient.sendPeripheralCommand(macAddress, BleCommand.SYNC_THERMOMETER_UNIT_F);
            }
        });
        //Get the connection status of the device, the default is STATE_DISCONNECTED
        fragmentHomeBinding.btnConnectState.setOnClickListener(new View.OnClickListener() {
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
        fragmentHomeBinding.btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                homeViewModel.getIsSearching().setValue(false);
                homeViewModel.getIsConnecting().setValue(false);
                homeViewModel.getIsConnect().setValue(false);
                scPeripheralManager.disconnectPeripheral(macAddress);
            }
        });
        DeviceListAdapter deviceListAdapter = new DeviceListAdapter(mDeviceList);
        deviceListAdapter.setItemClickListener(new DeviceListAdapter.ItemClickListener() {
            @Override
            public void onClick(ScPeripheral scBluetoothDevice) {
                macAddress = scBluetoothDevice.getMacAddress();
                appendConsoleContent("Prepare to connect the device " + macAddress);
                if (!checkBleFeatures()) {
                    return;
                }
                if (TextUtils.isEmpty(macAddress)) {
                    return;
                }
                homeViewModel.getIsSearching().setValue(false);
                homeViewModel.getIsConnecting().setValue(true);
                homeViewModel.getIsConnect().setValue(false);
                //Connect a Bluetooth device
                scPeripheralManager.connectPeripheral(macAddress, receiveDataListenerAdapter);
            }
        });
        fragmentHomeBinding.recyclerView.setAdapter(deviceListAdapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        fragmentHomeBinding.recyclerView.setLayoutManager(layoutManager);
    }

    private void appendConsoleContent(String massage) {
        StringBuffer stringBuffer = homeViewModel.getConsoleContent().getValue();
        if (stringBuffer == null) {
            stringBuffer = new StringBuffer();
        }
        stringBuffer.append(massage);
        stringBuffer.append("\n");
        homeViewModel.getConsoleContent().setValue(stringBuffer);
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
                    Log.d("ble", "Bluetooth is ff");
                    appendConsoleContent("Bluetooth off");
                } else if (state == BluetoothAdapter.STATE_ON) {
                    Log.d("ble", "Bluetooth is on");
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