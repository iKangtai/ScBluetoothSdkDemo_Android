package com.example.blesdkdemo.service;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.example.blesdkdemo.R;
import com.example.blesdkdemo.activity.MainActivity;

import java.util.List;

public class BleService extends Service {
    public static final String APP_FOREGROUND_SCAN_RESULT = "com.ikangtai.ble.app_foreground_scan_result";
    public final static int ALARM_SERVICE_ID = 2001;
    private String title;
    private String message;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() == null) {
            return START_STICKY;
        }

        //获取返回的错误码
        int errorCode = intent.getIntExtra(BluetoothLeScanner.EXTRA_ERROR_CODE, -1);//ScanSettings.SCAN_FAILED_*
        //获取到的蓝牙设备的回调类型
        int callbackType = intent.getIntExtra(BluetoothLeScanner.EXTRA_CALLBACK_TYPE, -1);//ScanSettings.CALLBACK_TYPE_*
        Log.d("xyl", "errorCode:" + errorCode);
        if (errorCode == -1) {
            //扫描到蓝牙设备信息获取不到数据 广播通知宿主App开启扫描
            List<ScanResult> scanResults = (List<ScanResult>) intent.getSerializableExtra(BluetoothLeScanner.EXTRA_LIST_SCAN_RESULT);
            //发现扫描设备
            sendBroadcast(new Intent(BleService.APP_FOREGROUND_SCAN_RESULT));
            //stopSelf();
        } else {
            //此处为扫描失败的错误处理
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        title = getString(R.string.app_name);
        message = "蓝牙扫描服务正在运行...";
        startForeground(BleService.ALARM_SERVICE_ID, title, message, this);
    }

    public static Notification startForeground(int id, String title, String message, Service service) {
        Notification notification;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = service.getPackageName();
            Notification.Builder builder = new Notification.Builder(service.getApplicationContext(), service.getApplicationContext().getPackageName());
            builder.setContentTitle(title);
            builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm);
            builder.setContentText(message);
            //修改安卓8.1以上系统报错
            NotificationChannel notificationChannel = new NotificationChannel(channelId, "蓝牙扫描服务", NotificationManager.IMPORTANCE_MIN);
            notificationChannel.setImportance(NotificationManager.IMPORTANCE_MIN);
            notificationChannel.enableLights(false);//如果使用中的设备支持通知灯，则说明此通知通道是否应显示灯
            notificationChannel.setShowBadge(false);//是否显示角标
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_SECRET);
            NotificationManager manager = (NotificationManager) service.getSystemService(NOTIFICATION_SERVICE);
            manager.createNotificationChannel(notificationChannel);
            builder.setChannelId(channelId);
            Intent intent = new Intent(service, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pendingIntent;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                pendingIntent = PendingIntent.getActivity(service,
                        0, intent, PendingIntent.FLAG_IMMUTABLE);
            } else {
                pendingIntent = PendingIntent.getActivity(service,
                        0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            }
            builder.setContentIntent(pendingIntent);
            notification = builder.build();
        } else {
            Notification.Builder builder = new Notification.Builder(service.getApplicationContext());
            builder.setVibrate(null);
            builder.setSound(null);
            builder.setLights(0, 0, 0);
            notification = builder.build();
        }
        service.startForeground(id, notification);
        return notification;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
