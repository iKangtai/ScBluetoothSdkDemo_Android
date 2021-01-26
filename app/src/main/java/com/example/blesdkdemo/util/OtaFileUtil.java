package com.example.blesdkdemo.util;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.text.TextUtils;

import com.ikangtai.bluetoothsdk.util.LogUtils;

import java.io.File;

/**
 * desc
 *
 * @author xiongyl 2020/10/16 0:00
 */
public class OtaFileUtil {
    private String downloadURL;
    private String downloadJsonURL;
    private Context context;
    private long downloadId;
    private String latestVer;

    public OtaFileUtil(Context context, String latestVer, String downloadURL) {
        this.context = context;
        this.latestVer = latestVer;
        this.downloadJsonURL = downloadURL;
    }

    public void handleFirmwareImgMsg() {
        if (!TextUtils.isEmpty(downloadJsonURL)) {
            downloadURL = downloadJsonURL;
            downloadFirmwareImage();
        } else {
            LogUtils.e("下载固件时出现错误");
        }
    }

    private void downloadFirmwareImage() {
        String filePath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + getFileName(latestVer);
        File downloadFile = new File(filePath);
        if (downloadFile.exists()) {
            LogUtils.i("发现已下载了固件镜像文件 " + getFileName(latestVer) + ", 无需再次下载!");
            downloadId = -10001;
            Intent intent = new Intent(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            context.sendBroadcast(intent);
            //downloadFile.delete();
        } else {
            //创建下载任务,downloadUrl就是下载链接
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadURL));
            //设置通知栏标题
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            request.setTitle("OTA下载");
            request.setDescription("OTA二进制文件正在下载...");
            request.setAllowedOverRoaming(false);
            //指定下载路径和下载文件名
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, getFileNameTemp(latestVer));
            //获取下载管理器
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            //将下载任务加入下载队列，否则不会进行下载
            downloadId = downloadManager.enqueue(request);
            LogUtils.i("正在下载 ... downloadId = " + downloadId);
        }
    }

    public static String getFileName(String version) {
        String fileName = String.format("otaBinFile_complete_%s.img",version);
        return fileName.replaceAll("\\.", "_");
    }

    public static String getFileNameTemp(String version) {
        String fileName = String.format("otaBinFile_complete_temp_%s.img",version);
        return fileName.replaceAll("\\.", "_");
    }

    public long getDownloadId() {
        return downloadId;
    }

    public String getLatestVer() {
        return latestVer;
    }
}
