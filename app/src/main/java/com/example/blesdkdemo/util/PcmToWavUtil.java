package com.example.blesdkdemo.util;

import android.media.AudioRecord;
import android.util.Log;

import com.example.blesdkdemo.Constant;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class PcmToWavUtil {
    private static final String TAG = "PcmToWavUtil";
    private int mBufferSize;
    private int mChannel;
    private int mEncoding;
    private int mSampleRate;

    public PcmToWavUtil() {
        this.mSampleRate = Constant.SAMPLING;
        this.mChannel = 2;
        this.mEncoding = 2;
        this.mBufferSize = AudioRecord.getMinBufferSize(this.mSampleRate, this.mChannel, this.mEncoding);
    }

    public PcmToWavUtil(int i, int i2, int i3) {
        this.mSampleRate = Constant.SAMPLING;
        this.mChannel = 2;
        this.mEncoding = 2;
        this.mSampleRate = i;
        this.mChannel = i2;
        this.mEncoding = i3;
        this.mBufferSize = AudioRecord.getMinBufferSize(this.mSampleRate, this.mChannel, this.mEncoding);
    }

    public boolean pcmToWav(String str, String str2) {
        long j = (long) this.mSampleRate;
        long j2 = (long) (((this.mSampleRate * 16) * 1) / 8);
        byte[] bArr = new byte[this.mBufferSize];
        try {
            File file = new File(str);
            File file2 = new File(str2);
            FileInputStream fileInputStream = new FileInputStream(file);
            FileOutputStream fileOutputStream = new FileOutputStream(file2);
            long size = fileInputStream.getChannel().size();
            FileOutputStream fileOutputStream2 = fileOutputStream;
            FileInputStream fileInputStream2 = fileInputStream;
            writeWaveFileHeader(fileOutputStream, size, size + 36, j, 1, j2);
            while (fileInputStream2.read(bArr) != -1) {
                FileOutputStream fileOutputStream3 = fileOutputStream2;
                fileOutputStream3.write(bArr);
                fileOutputStream2 = fileOutputStream3;
            }
            fileInputStream2.close();
            fileOutputStream2.close();
            if (file.exists()) {
                boolean delete = file.delete();
                Log.e(TAG, "pcmToWav: " + delete);
            }
            return file2.exists();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void writeWaveFileHeader(FileOutputStream fileOutputStream, long j, long j2, long j3, int i, long j4) throws IOException {
        fileOutputStream.write(new byte[]{82, 73, 70, 70, (byte) ((int) (j2 & 255)), (byte) ((int) ((j2 >> 8) & 255)), (byte) ((int) ((j2 >> 16) & 255)), (byte) ((int) ((j2 >> 24) & 255)), 87, 65, 86, 69, 102, 109, 116, 32, 16, 0, 0, 0, 1, 0, (byte) i, 0, (byte) ((int) (j3 & 255)), (byte) ((int) ((j3 >> 8) & 255)), (byte) ((int) ((j3 >> 16) & 255)), (byte) ((int) ((j3 >> 24) & 255)), (byte) ((int) (j4 & 255)), (byte) ((int) ((j4 >> 8) & 255)), (byte) ((int) ((j4 >> 16) & 255)), (byte) ((int) ((j4 >> 24) & 255)), 4, 0, 16, 0, 100, 97, 116, 97, (byte) ((int) (j & 255)), (byte) ((int) ((j >> 8) & 255)), (byte) ((int) ((j >> 16) & 255)), (byte) ((int) ((j >> 24) & 255))}, 0, 44);
    }
}