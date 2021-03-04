package com.example.blesdkdemo.util;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import com.ikangtai.bluetoothsdk.http.respmodel.CheckFirmwareVersionResp;
import com.ikangtai.bluetoothsdk.util.BleParam;
import com.ikangtai.bluetoothsdk.util.LogUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

/**
 * Devices OAD Upgrade
 *
 * @author xiongyl 2020/10/16 0:00
 */
public class OadFileUtil {
    //private String downloadURL="https://yunchengfile.oss-cn-beijing.aliyuncs.com/firmware/A31_danger";
    //private String downloadURL ="https://api.premom.com/firmwares/third";
    //private String downloadURL = "https://yunchengfile.oss-cn-beijing.aliyuncs.com/firmware/A31";
    //https://yunchengfile.oss-cn-beijing.aliyuncs.com/firmware/A31/A31_3.67.bin
    //https://yunchengfile.oss-cn-beijing.aliyuncs.com/firmware/A31/B31_3.67.bin
    private String downloadURL;
    private Context context;
    private long downloadId;
    private int oadFileType;
    private CheckFirmwareVersionResp.Data checkFirmwareVersionData;

    public OadFileUtil(Context context, CheckFirmwareVersionResp.Data checkFirmwareVersionData) {
        this.context = context;
        this.checkFirmwareVersionData = checkFirmwareVersionData;
    }

    public void handleFirmwareImgABMsg(int imgType) {
        this.oadFileType = imgType;
        int firmwareImgAB = imgType;
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(checkFirmwareVersionData.getFileUrl());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (jsonObject != null) {
            if (firmwareImgAB == BleParam.FIRMWARE_IMAGE_REVERSION_A) {
                try {
                    downloadURL = jsonObject.getString("B");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                LogUtils.i("The current firmware uses version A, download and upgrade firmware B.");
                downloadFirmwareImage(BleParam.FIRMWARE_IMAGE_REVERSION_A);
            } else if (firmwareImgAB == BleParam.FIRMWARE_IMAGE_REVERSION_B) {
                try {
                    downloadURL = jsonObject.getString("A");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                LogUtils.i("The current firmware uses version B, download and upgrade firmware A.");
                downloadFirmwareImage(BleParam.FIRMWARE_IMAGE_REVERSION_B);
            } else {
                LogUtils.e("An error occurred while downloading the firmware, not A/B");
            }
        } else {
            LogUtils.e("An error occurred while downloading the firmware, not A/B");
        }
    }

    private void downloadFirmwareImage(int type) {
        String filePath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + getFileName(type, checkFirmwareVersionData.getVersion());
        File downloadFile = new File(filePath);
        downloadFile.delete();
        if (downloadFile.exists()) {
            LogUtils.i("Found that the firmware image file has been downloaded " + getFileName(type, checkFirmwareVersionData.getVersion()) + ", no need to download again!");
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
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, getFileNameTemp(type, checkFirmwareVersionData.getVersion()));
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
        return checkFirmwareVersionData.getVersion();
    }
}
