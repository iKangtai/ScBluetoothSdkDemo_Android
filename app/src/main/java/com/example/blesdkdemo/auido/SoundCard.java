package com.example.blesdkdemo.auido;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioTrack;
import android.util.Log;

import com.example.blesdkdemo.MyApplication;
import com.example.blesdkdemo.util.Util;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@SuppressLint({"NewApi"})
public class SoundCard {
    private static final String TAG = "SoundCard";
    private DataOutputStream dos;
    private FileOutputStream fos;
    private int mAudioEncoding = 2;
    private AudioTrack mAudioTrack;
    private int mChannelConfiguration = 2;
    private Context mContext = MyApplication.getInstance();
    private int mTimesPerFrame = 50;
    private int mFreDiff;
    private int mFreDoubles = 1;
    private int mFrequency;
    private final int mN = 30;
    private short[] mPerDatasFilter = new short[30];
    private short[] mPerDatasSrc = new short[30];
    private int mPerFrameLastData = 0;
    private int mPlayBufFramesPerWindown = 6;
    private int mPlayBufWindowns = 3;
    private int mPlayIndex = 0;
    private int mSampleFrequency;
    private int mTicks = 0;
    private int mVol = 2;
    private int mWriteIndex = 0;
    private boolean mbCloseWavFile = false;
    private double[] mh = {0.0d, 2.5E-4d, 6.54E-4d, 1.11E-4d, -0.002612d, -0.00777d, -0.013704d, -0.016689d, -0.011909d, 0.00466d, 0.034184d, 0.073635d, 0.11594d, 0.15175d, 0.172298d, 0.172298d, 0.15175d, 0.11594d, 0.073635d, 0.034184d, 0.00466d, -0.011909d, -0.016689d, -0.013704d, -0.00777d, -0.002612d, 1.11E-4d, 6.54E-4d, 2.5E-4d, 0.0d};
    private boolean miPlayBufValid = false;
    private boolean miPlaying;
    private boolean pause = false;
    private boolean recordStart = false;
    private int timeoutCount;
    private int mDatasPerFrame = (this.mFrequency / (1000 / this.mTimesPerFrame));
    private int mPlayBufLen = ((this.mDatasPerFrame * this.mPlayBufWindowns) * this.mPlayBufFramesPerWindown);
    private short[] mPlayBuf = new short[this.mPlayBufLen];

    static /* synthetic */ int access$608(SoundCard soundCard) {
        int i = soundCard.mPlayIndex;
        soundCard.mPlayIndex = i + 1;
        return i;
    }

    public SoundCard(int i, int i2) {
        this.mSampleFrequency = i2;
        this.mFrequency = i;
        this.mFreDiff = this.mFrequency / this.mSampleFrequency;
        Initial();
        this.miPlaying = true;
        new Thread(new PlayDataCheckThread()).start();
        new Thread(new PlayingThread()).start();
    }

    private void Initial() {
        this.mDatasPerFrame = this.mFrequency / (1000 / this.mTimesPerFrame);
        this.mPlayBufWindowns = 3;
        this.mPlayBufFramesPerWindown = 6;
        this.mPlayBufLen = this.mDatasPerFrame * this.mPlayBufWindowns * this.mPlayBufFramesPerWindown;
        this.mPlayIndex = 0;
        this.mWriteIndex = 0;
        this.mPlayBuf = new short[this.mPlayBufLen];
        this.mAudioTrack = new AudioTrack(2, this.mFrequency, this.mChannelConfiguration, this.mAudioEncoding, this.mDatasPerFrame * this.mPlayBufFramesPerWindown, 1);
        for (int i = 0; i < 30; i++) {
            this.mPerDatasSrc[i] = 0;
            this.mPerDatasFilter[i] = 0;
        }
    }

    public void close() {
        this.miPlaying = false;
        if (this.mAudioTrack != null) {
            this.mAudioTrack.release();
            this.mAudioTrack = null;
        }
        stopRecord();
    }

    public void setPause(boolean z) {
        this.pause = z;
    }

    public void startRecord(String str) {
        this.recordStart = true;
        try {
            File file = new File(str);
            if (!file.exists()) {
                boolean createNewFile = file.createNewFile();
                String str2 = TAG;
                Log.e(str2, "newFile:" + createNewFile);
            }
            this.fos = new FileOutputStream(file);
            this.dos = new DataOutputStream(new BufferedOutputStream(this.fos));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopRecord() {
        this.recordStart = false;
        if (this.dos != null) {
            try {
                this.dos.close();
                this.dos = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (this.fos != null) {
            try {
                this.fos.close();
                this.fos = null;
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
    }

    class PlayingThread implements Runnable {
        PlayingThread() {
        }

        public void run() {
            try {
                SoundCard.this.mAudioTrack.play();
                while (SoundCard.this.miPlaying) {
                    if (SoundCard.this.pause) {
                        Thread.sleep(25);
                    } else if (SoundCard.this.miPlayBufValid) {
                        int i = SoundCard.this.mDatasPerFrame * SoundCard.this.mPlayBufFramesPerWindown;
                        int i2 = SoundCard.this.mPlayIndex * SoundCard.this.mDatasPerFrame * SoundCard.this.mPlayBufFramesPerWindown;
                        byte[] byteArray = Util.toByteArray(SoundCard.this.mPlayBuf);
                        int i3 = i2 * 2;
                        int write = SoundCard.this.mAudioTrack.write(byteArray, i3, i * 2);
                        if (SoundCard.this.recordStart && SoundCard.this.dos != null) {
                            SoundCard.this.dos.write(byteArray, i3, write);
                        }
                        SoundCard.access$608(SoundCard.this);
                        if (SoundCard.this.mPlayIndex >= SoundCard.this.mPlayBufWindowns) {
                            SoundCard.this.mPlayIndex = 0;
                        }
                    }
                }
                if (SoundCard.this.mAudioTrack != null) {
                    SoundCard.this.mAudioTrack.stop();
                    SoundCard.this.mAudioTrack.release();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setData() {
        if (this.mbCloseWavFile) {
            this.mbCloseWavFile = false;
        }
        if (this.mTicks >= 4 && this.miPlayBufValid) {
            StringBuilder sb = new StringBuilder();
            sb.append("200ms没有收到蓝牙数据，复位");
            int i = this.timeoutCount;
            this.timeoutCount = i + 1;
            sb.append(i);
            Log.d("err11", sb.toString());
            this.mTicks = 0;
            for (int i2 = 0; i2 < this.mPlayBuf.length; i2++) {
                this.mPlayBuf[i2] = 0;
            }
            this.miPlayBufValid = false;
            this.mPlayIndex = 0;
            this.mWriteIndex = 0;
            for (int i3 = 0; i3 < 30; i3++) {
                this.mPerDatasSrc[i3] = 0;
                this.mPerDatasFilter[i3] = 0;
            }
        }
        if (this.miPlayBufValid) {
            this.mTicks++;
        }
    }

    class PlayDataCheckThread implements Runnable {
        PlayDataCheckThread() {
        }

        public void run() {
            while (SoundCard.this.miPlaying) {
                try {
                    long currentTimeMillis = System.currentTimeMillis();
                    SoundCard.this.setData();
                    long currentTimeMillis2 = System.currentTimeMillis() - currentTimeMillis;
                    if (currentTimeMillis2 < ((long) SoundCard.this.mTimesPerFrame)) {
                        Thread.sleep(((long) SoundCard.this.mTimesPerFrame) - currentTimeMillis2);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r1v0, types: [int] */
    /* JADX WARNING: Unknown variable types count: 1 */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void WriteData(short[] r13, int r14) {
        SoundCard r12 = this;
        int r0 = r12.mFreDiff;
        r0 = r0 * r14;
        short[] r9 = new short[r0];
        short[] r10 = new short[r0];
        int r11 = 0;
        r12.mTicks = 0;
        r12.mPerFrameLastData = r10[r14 - 1];
        int r2 = r12.mPerFrameLastData;
        //L_0x0015
        for (int r1 = 0; r1 < r14; r1++) {
            //L_0x0018:
            for (int r3 = 0; r3 < r12.mFreDiff; ) {
                float r4 = (float) r13[r1] - r2;
                r4 = r4 / r12.mFreDiff;
                int r5 = r12.mFreDiff * r1 + r3;
                r3++;
                float r6 = r2 + r4 * r3;
                r10[r5] = (short) r6;
                //goto L_0x0018
            }
            //goto L_0x0035
            //L_0x0035:
            r2 = r13[r1];
            //goto L_0x0015
        }
        //r1 >= r14 goto L_0x003a:
        //L_0x003a
        short[] r4 = r12.mPerDatasSrc;
        double[] r5 = r12.mh;
        int r7 = 30;
        int r8 = r12.mVol;

        r12.Filter(r10, r9, r4, r5, r0, r7, r8);

        //L_0x004a:
        for (int i = 0; i < 30; i++) {
            short[] datasSrc = r12.mPerDatasSrc;
            datasSrc[i] = r10[r0 - 30 + i];
            //goto L_0x004a
        }
        //r1 >= r14 goto L_0x005a
        //L_0x005a:
        if (r12.mFreDoubles != 2) {
            //goto L_0x008b
        } else {
            //L_0x0060:
            for (int i = 0; i < r0; i++) {
                r10[i] = (short) Math.abs(r9[i]);
                //goto L_0x0060
            }
            //goto L_0x006e:
            //L_0x006e:
            r12.Filter(r10, r9, r12.mPerDatasFilter, r12.mh, r0, 30, 2);
            //L_0x007d:
            for (int i = 0; i < r14; i++) {
                short[] r1 = r12.mPerDatasFilter;
                r1[i] = r10[r0 + -30 + i];
                //goto L_0x007d
            }
            //goto L_0x008b

        }
        //L_0x008b:
        r14 = 1;
        if (r12.miPlayBufValid) {
            //goto L_0x00bf
            //L_0x00bf:
            r12.mWriteIndex = r12.mWriteIndex + r14;
            if (r12.mWriteIndex < r12.mPlayBufFramesPerWindown * r12.mPlayBufWindowns) {
                //goto L_0x00d0
            } else {
                r12.mWriteIndex = 0;
            }
            //L_0x00d0:
            if (r12.mWriteIndex / r12.mPlayBufFramesPerWindown != r12.mPlayIndex) {
                //goto L_0x00ee
            } else {
                r12.mWriteIndex = r12.mPlayIndex + r14 * r12.mPlayBufFramesPerWindown;
                if (r12.mWriteIndex < r12.mPlayBufFramesPerWindown * r12.mPlayBufWindowns) {
                    //goto L_0x00ee
                } else {
                    r12.mWriteIndex = 0;
                }
            }
            //L_0x00ee:
            for (int i = 0; i < r0; i++) {
                r12.mPlayBuf[r12.mWriteIndex * r12.mDatasPerFrame + i] = r9[i];
                //int r11 = r11 + 1
                //goto L_0x00ee
            }
            //if (r11 >= r0) goto L_0x0100

        } else {
            if (r12.mWriteIndex != 0) {
                //goto L_0x0099
                //L_0x0099:
                r12.mWriteIndex = r12.mWriteIndex + r14;
                if (r12.mWriteIndex < r12.mPlayBufFramesPerWindown * r12.mPlayBufWindowns) {
                    //goto L_0x00aa
                } else {
                    r12.mWriteIndex = 0;
                    //goto L_0x00aa
                }
            } else {
                r12.mWriteIndex = r12.mPlayBufFramesPerWindown;
                //goto L_0x00aa
            }
            //L_0x00aa:
            for (int i = 0; i < r0; i++) {
                r12.mPlayBuf[r12.mWriteIndex * r12.mDatasPerFrame + i] = r9[i];
                //goto L_0x00aa
            }
            //if (r11 >= r0) goto L_0x00bc
            //L_0x00bc:
            r12.miPlayBufValid = true;
            //goto L_0x0100
        }
        //L_0x0100:
        return;


    }

    private void Filter(short[] sArr, short[] sArr2, short[] sArr3, double[] dArr, int i, int i2, int i3) {
        double d = (((double) (i3 - 1)) * 0.5d) + 1.0d;
        int i4 = i + i2;
        int[] iArr = new int[i4];
        for (int i5 = 0; i5 < i2; i5++) {
            iArr[i5] = (int) (((double) sArr3[i5]) * d);
            if (iArr[i5] > 32767) {
                iArr[i5] = 32767;
            } else if (iArr[i5] < -32767) {
                iArr[i5] = -32767;
            }
        }
        for (int i6 = i2; i6 < i4; i6++) {
            iArr[i6] = (int) (((double) sArr[i6 - i2]) * d);
            if (iArr[i6] > 32767) {
                iArr[i6] = 32767;
            } else if (iArr[i6] < -32767) {
                iArr[i6] = -32767;
            }
        }
        if (i >= i2) {
            for (int i7 = 0; i7 < i2; i7++) {
                sArr3[i7] = sArr[(i - i2) + i7];
            }
        } else {
            int i8 = i2 - i;
            int i9 = 0;
            while (i9 < i8) {
                sArr3[i9] = sArr3[i9 + i];
                i9++;
            }
            for (int i10 = 0; i10 < i; i10++) {
                sArr3[i9] = sArr[i10];
                i9++;
            }
        }
        for (int i11 = 0; i11 < i; i11++) {
            double d2 = 0.0d;
            for (int i12 = 0; i12 < i2; i12++) {
                d2 += dArr[i12] * ((double) iArr[(i11 - i12) + i2]);
            }
            double d3 = 32767.0d;
            if (d2 <= 32767.0d) {
                d3 = d2 < -32767.0d ? -32767.0d : d2;
            }
            sArr2[i11] = (short) ((int) d3);
        }
    }
}