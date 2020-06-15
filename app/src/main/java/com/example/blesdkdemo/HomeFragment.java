package com.example.blesdkdemo;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hjq.permissions.OnPermission;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;
import com.ikangtai.bluetoothsdk.BleCommand;
import com.ikangtai.bluetoothsdk.ScBluetoothClient;
import com.ikangtai.bluetoothsdk.listener.ReceiveDataListenerAdapter;
import com.ikangtai.bluetoothsdk.listener.ScanResultListener;
import com.ikangtai.bluetoothsdk.model.DeviceData;
import com.ikangtai.bluetoothsdk.model.ScBluetoothDevice;
import com.ikangtai.bluetoothsdk.util.BleTools;
import com.ikangtai.bluetoothsdk.util.LogUtils;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    private String macAddress;
    private ScBluetoothClient scBluetoothClient;
    private List<ScBluetoothDevice> mDeviceList = new ArrayList<>();
    public final static int REQUEST_LOCATION_SETTINGS = 1000;
    public final static int REQUEST_BLE_SETTINGS_CODE = 1001;
    public TextView deviceListTv;
    public TextView dataListTv;
    private ReceiveDataListenerAdapter receiveDataListenerAdapter = new ReceiveDataListenerAdapter() {
        @Override
        public void onReceiveData(String macAddress, List<DeviceData> deviceDataList) {
            StringBuffer sb = new StringBuffer();
            for (DeviceData temperature : deviceDataList) {
                if (temperature != null) {
                    sb.append(temperature.getDate()).append("  ").append(temperature.getTemp());
                    sb.append("\n");
                }
            }
            dataListTv.setText(sb.toString());
        }

        @Override
        public void onReceiveError(String macAddress, int code, String msg) {
            Log.d("ble", code + "  " + msg);
        }

        @Override
        public void onConnectionStateChange(String macAddress, int state) {
            if (state == BluetoothProfile.STATE_CONNECTED) {
                Log.d("ble", "The device is connected.");

            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d("ble", "The device has been disconnected.");
            }
        }

        @Override
        public void onReceiveCommandData(String macAddress, int type, boolean state, String value) {
            Log.d("ble", type + "  " + state + " " + value);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        scBluetoothClient = ScBluetoothClient.getInstance();
        scBluetoothClient.init(getContext());

        //Register to receive Bluetooth switch broadcast
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        getActivity().registerReceiver(receiver, filter);

        View contentView = inflater.inflate(R.layout.fragment_home, container, false);
        deviceListTv = contentView.findViewById(R.id.device_list);
        dataListTv = contentView.findViewById(R.id.temp_list);

        // Get the address of a device that has been discovered
        final SharedPreferences sharedPreferences = getActivity().getSharedPreferences("blesdkdemo", Context.MODE_PRIVATE);
        macAddress = sharedPreferences.getString("address", null);
        //Scan nearby Bluetooth devices
        contentView.findViewById(R.id.btn_find_device).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!checkBleFeatures()) {
                    return;
                }
                scBluetoothClient.startScanDevice(new ScanResultListener() {
                    @Override
                    public void onScannerResult(List<ScBluetoothDevice> deviceList) {
                        if (deviceList != null) {
                            for (int i = 0; i < deviceList.size(); i++) {
                                ScBluetoothDevice scBluetoothDevice = deviceList.get(i);
                                if (!mDeviceList.contains(scBluetoothDevice)) {
                                    mDeviceList .add(scBluetoothDevice);
                                }
                            }
                        }
                        StringBuffer sb = new StringBuffer();
                        for (ScBluetoothDevice scBluetoothDevice : mDeviceList) {
                            sb.append(scBluetoothDevice.getDeviceName() + " " + scBluetoothDevice.getMacAddress());
                            sb.append("\n");
                        }
                        deviceListTv.setText(sb.toString());
                    }
                });
            }
        });

        //Stop scan devices
        contentView.findViewById(R.id.btn_stop_scan).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scBluetoothClient.stopScanDevice();
                if (!mDeviceList.isEmpty()) {
                    macAddress = mDeviceList.get(0).getMacAddress();
                    sharedPreferences.edit().putString("address", macAddress).commit();
                }
                deviceListTv.setText(deviceListTv.getText().toString() + "\nScan end");
            }
        });
        //Connect a Bluetooth device
        contentView.findViewById(R.id.btn_connect_device).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (!checkBleFeatures()) {
                    return;
                }
                if (TextUtils.isEmpty(macAddress)) {
                    return;
                }
                scBluetoothClient.connectDevice(macAddress, receiveDataListenerAdapter);
            }
        });
        //Get the connection status of the device, the default is STATE_DISCONNECTED
        contentView.findViewById(R.id.btn_connect_state).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int connectStatus = scBluetoothClient.getConnectState(macAddress);
                if (connectStatus == BluetoothProfile.STATE_CONNECTED) {
                    ((Button) v).setText("Connection Status = connected");
                } else if (connectStatus == BluetoothProfile.STATE_DISCONNECTED) {
                    ((Button) v).setText("Connection Status = not connected");

                }
            }
        });
        //Sync phone time to Bluetooth device
        contentView.findViewById(R.id.btn_sync_time).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scBluetoothClient.sendDeviceCommand(macAddress, BleCommand.SYNC_TIME);
            }
        });
        //Sync unit to Bluetooth device
        contentView.findViewById(R.id.btn_sync_unit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //set unit c
                scBluetoothClient.sendDeviceCommand(macAddress, BleCommand.SYNC_THERMOMETER_UNIT_C);
                //set unit f
                //scBluetoothClient.sendDeviceCommand(macAddress, BleCommand.SYNC_THERMOMETER_UNIT_F);
            }
        });
        //Bluetooth device needs to be disconnected after use
        contentView.findViewById(R.id.btn_disconnect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scBluetoothClient.disconnectDevice(macAddress);
            }
        });
        return contentView;
    }

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
                                Toast.makeText(getContext(), R.string.request_location_premisson, Toast.LENGTH_LONG).show();
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
                    Log.d("ble", "Bluetooth off");
                } else if (state == BluetoothAdapter.STATE_ON) {
                    Log.d("ble", "Bluetooth is on");
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
                Toast.makeText(getContext(), R.string.location_service_turn_on, Toast.LENGTH_LONG).show();
            } else {
                LogUtils.e("Location service: The user manually set the location service is not enabled");
                Toast.makeText(getContext(), R.string.location_service_turn_off, Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == REQUEST_BLE_SETTINGS_CODE) {
            boolean enable = BleTools.isLocationEnable(getContext());
            if (!enable) {
                Toast.makeText(getContext(), R.string.request_location_premisson_tips, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().unregisterReceiver(receiver);
        if (scBluetoothClient != null) {
            scBluetoothClient.stopScanDevice();
            scBluetoothClient.disconnectDevice();
        }
    }
}