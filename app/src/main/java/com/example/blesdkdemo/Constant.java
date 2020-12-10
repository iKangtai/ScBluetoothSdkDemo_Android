package com.example.blesdkdemo;

import android.media.AudioTrack;

import java.util.UUID;

/**
 * desc
 *
 * @author xiongyl 2020/11/5 17:36
 */
public class Constant {
    public static final int BLE_MODE = 1;
    public static UUID CLIENT_SERVICE = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    public static final String HEADSET_PLUG = "android.intent.action.HEADSET_PLUG";
    public static final String HISTORY_INTENT = "history_data";
    public static final int LINE_MODE = 2;
    public static final String MODE = "mode";
    public static final int ORIGINAL_SAMPLING = 2000;
    public static final int QUICKENING = 5000;
    public static final UUID READ_SERVICE = UUID.fromString("00005502-D102-11E1-9B23-00025B00A5A5");
    public static final int SAMPLING = 8000;
    public static final int MIN_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLING, 2, 2);
    public static final UUID SERVICE = UUID.fromString("00005500-D102-11E1-9B23-00025B00A5A5");
    public static final UUID WRITE_SERVICE = UUID.fromString("00005501-D102-11E1-9B23-00025B00A5A5");
    public static final int audioChannel = 2;
    public static final int audioEncoding = 2;

}
