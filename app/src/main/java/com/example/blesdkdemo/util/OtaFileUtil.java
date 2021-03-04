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
 * Devices OTA Upgrade
 *
 * @author xiongyl 2020/10/16 0:00
 */
public class OtaFileUtil {
    //BleTools.TYPE_AKY_3
    //file:///android_asset/img/yc_v4.06.img
    //BleTools.TYPE_AKY_4
    //file:///android_asset/img/YC-399B-TEST-V0.img
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
            LogUtils.e("An error occurred while downloading the firmware");
        }
    }

    private void downloadFirmwareImage() {
        String filePath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + getFileName(latestVer);
        File downloadFile = new File(filePath);
        if (downloadFile.exists()) {
            LogUtils.i("Found that the firmware image file has been downloaded " + getFileName(latestVer) + ", no need to download again!");
            downloadId = -10001;
            Intent intent = new Intent(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            context.sendBroadcast(intent);
            //downloadFile.delete();
        } else {
            //Create a download task, downloadUrl is the download link
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadURL));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            request.setTitle("OTA download");
            request.setDescription("OTA binaries are downloading...");
            request.setAllowedOverRoaming(false);
            //Specify the download path and download file name
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, getFileNameTemp(latestVer));
            //Get download manager
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            //Add the download task to the download queue, otherwise it will not download
            downloadId = downloadManager.enqueue(request);
            LogUtils.i("download ... downloadId = " + downloadId);
        }
    }

    public static String getFileName(String version) {
        String fileName = String.format("otaBinFile_complete_%s.img", version);
        return fileName.replaceAll("\\.", "_");
    }

    public static String getFileNameTemp(String version) {
        String fileName = String.format("otaBinFile_complete_temp_%s.img", version);
        return fileName.replaceAll("\\.", "_");
    }

    public long getDownloadId() {
        return downloadId;
    }

    public String getLatestVer() {
        return latestVer;
    }
}
