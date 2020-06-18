package com.example.blesdkdemo;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.blesdkdemo.databinding.DeviceListItemBinding;
import com.ikangtai.bluetoothsdk.model.ScBluetoothDevice;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * desc
 *
 * @author xiongyl 2020/6/18 22:35
 */
public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.DeviceListViewHolder> {
    private List<ScBluetoothDevice> devices;
    private ItemClickListener itemClickListener;

    public DeviceListAdapter(List<ScBluetoothDevice> devices) {
        this.devices = devices;
    }

    public void setItemClickListener(ItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }

    @NonNull
    @Override
    public DeviceListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        DeviceListItemBinding bind = DeviceListItemBinding.inflate(inflater, parent, false);
        return new DeviceListViewHolder(bind);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceListViewHolder holder, final int position) {

        holder.deviceListItemBinding.setDevice(devices.get(position));
        holder.deviceListItemBinding.setIsLastConnectedDevice(devices.get(position).getMacAddress() == MyApplication.getInstance().appPreferences.getLastDeviceAddress());
        holder.deviceListItemBinding.getRoot().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.onClick(devices.get(position));
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    public static class DeviceListViewHolder extends RecyclerView.ViewHolder {
        private DeviceListItemBinding deviceListItemBinding;

        public DeviceListViewHolder(DeviceListItemBinding binding) {
            super(binding.getRoot());
            this.deviceListItemBinding = binding;
        }

        public DeviceListItemBinding getDeviceListItemBinding() {
            return deviceListItemBinding;
        }
    }

    public interface ItemClickListener {
        void onClick(ScBluetoothDevice scBluetoothDevice);
    }

}
