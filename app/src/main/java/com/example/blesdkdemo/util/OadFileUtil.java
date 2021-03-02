package com.example.blesdkdemo.util;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import com.ikangtai.bluetoothsdk.util.BleParam;
import com.ikangtai.bluetoothsdk.util.LogUtils;

import java.io.File;

/**
 * Devices OAD Upgrade
 *
 * @author xiongyl 2020/10/16 0:00
 */
public class OadFileUtil {
    //private String downloadURL="https://yunchengfile.oss-cn-beijing.aliyuncs.com/firmware/A31_danger";
    //private String downloadURL ="https://api.premom.com/firmwares/third";
    private String downloadURL = "https://yunchengfile.oss-cn-beijing.aliyuncs.com/firmware/A31";
    //https://yunchengfile.oss-cn-beijing.aliyuncs.com/firmware/A31/A31_3.67.bin
    //https://yunchengfile.oss-cn-beijing.aliyuncs.com/firmware/A31/B31_3.67.bin
    private Context context;
    private long downloadId;
    private int oadFileType;
    private String version = "3.66";

    public OadFileUtil(Context context) {
        this.context = context;
    }

    public void handleFirmwareImgABMsg(int imgType) {
        this.oadFileType = imgType;
        int firmwareImgAB = imgType;
        if (firmwareImgAB == BleParam.FIRMWARE_IMAGE_REVERSION_A) {
            downloadURL += "/B31_3.66.bin";
            LogUtils.i("The current firmware uses version A, download and upgrade firmware B.");
            downloadFirmwareImage(BleParam.FIRMWARE_IMAGE_REVERSION_A);
        } else if (firmwareImgAB == BleParam.FIRMWARE_IMAGE_REVERSION_B) {
            downloadURL += "/A31_3.66.bin";
            LogUtils.i("The current firmware uses version B, download and upgrade firmware A.");
            downloadFirmwareImage(BleParam.FIRMWARE_IMAGE_REVERSION_B);
        } else {
            LogUtils.e("An error occurred while downloading the firmware, not A/B");
        }
    }

    private void downloadFirmwareImage(int type) {
        String filePath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + getFileName(type, version);
        File downloadFile = new File(filePath);
        downloadFile.delete();
        if (downloadFile.exists()) {
            LogUtils.i("Found that the firmware image file has been downloaded " + getFileName(type, version) + ", no need to download again!");
            downloadId = -10001;
            Intent intent = new Intent(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            context.sendBroadcast(intent);
            //downloadFile.delete();
        } else {
            //Create a download task, downloadUrl is the download link
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadURL));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            request.setTitle("OAD download");
            request.setDescription("OAD binaries are downloading...");
            request.setAllowedOverRoaming(false);
            //Specify the download path and download file name
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, getFileNameTemp(type, version));
            //Get download manager
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            //Add the download task to the download queue, otherwise it will not download
            downloadId = downloadManager.enqueue(request);
            LogUtils.i("download ... downloadId = " + downloadId);
        }
    }

    public static String getFileName(int imgType, String version) {
        String fileName = String.format(imgType + "otaBinFile_complete_%s.img", version);
        return fileName.replaceAll("\\.", "_");
    }

    public static String getFileNameTemp(int imgType, String version) {
        String fileName = String.format(imgType + "oadBinFile_complete_temp_%s.img", version);
        return fileName.replaceAll("\\.", "_");
    }

    public long getDownloadId() {
        return downloadId;
    }

    public int getOadFileType() {
        return oadFileType;
    }

    public String getVersion() {
        return version;
    }
}
