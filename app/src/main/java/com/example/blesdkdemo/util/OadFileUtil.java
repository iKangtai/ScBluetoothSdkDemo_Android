package com.example.blesdkdemo.util;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

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
    private String downloadURL;
    private String downloadJsonURL;
    private Context context;
    private long downloadId;
    private int oadFileType;
    private String latestVer;

    public OadFileUtil(Context context, String latestVer, String downloadURL) {
        this.context = context;
        this.latestVer = latestVer;
        this.downloadJsonURL = downloadURL;
    }

    public void handleFirmwareImgABMsg(int imgType) {
        this.oadFileType = imgType;
        int firmwareImgAB = imgType;
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(downloadJsonURL);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (jsonObject != null && firmwareImgAB == BleParam.FIRMWARE_IMAGE_REVERSION_A) {
            try {
                downloadURL = jsonObject.getString("B");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            LogUtils.i("The current firmware uses version A, download and upgrade firmware B.");
            downloadFirmwareImage(BleParam.FIRMWARE_IMAGE_REVERSION_A);
        } else if (jsonObject != null && firmwareImgAB == BleParam.FIRMWARE_IMAGE_REVERSION_B) {
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
    }

    private void downloadFirmwareImage(int type) {
        String filePath = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + getFileName(type, latestVer);
        File downloadFile = new File(filePath);
        if (downloadFile.exists()) {
            LogUtils.i("Found that the firmware image file has been downloaded " + getFileName(type, latestVer) + ", no need to download again!");
            downloadId = -10001;
            Intent intent = new Intent(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
            context.sendBroadcast(intent);
            //downloadFile.delete();
        } else {
            //Create a download task, downloadUrl is the download link
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(downloadURL));
            //Set the title of the notification bar
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
            request.setTitle("OAD download");
            request.setDescription("OAD binaries are downloading...");
            request.setAllowedOverRoaming(false);
            //Specify the download path and download file name
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, getFileNameTemp(type, latestVer));
            //Get download manager
            DownloadManager downloadManager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            //Add the download task to the download queue, otherwise it will not download
            downloadId = downloadManager.enqueue(request);
            LogUtils.i("downloading ... downloadId = " + downloadId);
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

    public String getLatestVer() {
        return latestVer;
    }
}
