package com.example.blesdkdemo;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blesdkdemo.databinding.ActivityHistoryBinding;
import com.example.blesdkdemo.db.HistoryBean;
import com.example.blesdkdemo.db.HistoryDao;
import com.example.blesdkdemo.ui.FHRData;
import com.example.blesdkdemo.ui.FHRMonitorView;
import com.example.blesdkdemo.ui.OnFhrListener;
import com.example.blesdkdemo.ui.OnMonitorTouchListener;
import com.example.blesdkdemo.util.GsonUtil;
import com.google.gson.reflect.TypeToken;
import com.ikangtai.bluetoothsdk.util.LogUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

public class HistoryActivity extends BaseActivity implements OnFhrListener, View.OnClickListener {
    private static final String TAG = "HistoryActivity_TAG";
    private int audio_progress_max;
    private Runnable correctRunnable = new Runnable() {
        /* class com.laijiayiliao.myapplication.ui.activity.HistoryActivity.AnonymousClass7 */

        public void run() {
            try {
                Thread.sleep(100);
                HistoryActivity.this.mediaPlayer.setVolume(1.0f, 1.0f);
                Log.e(HistoryActivity.TAG, "run: ");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    };
    private ExecutorService correctThread;
    private HistoryDao dao;
    private ArrayList<FHRData> fhrData;
    private SimpleDateFormat formatRecordingTime;
    private HistoryBean historyData;
    private boolean isPlayPause = true;
    private boolean isPlayStart = false;
    private boolean isTouchSB = false;
    ImageView iv_back;
    ImageView iv_play;
    private MediaPlayer mediaPlayer;
    FHRMonitorView monitorView;
    private long monitor_time;
    private int progress = 0;
    SeekBar sk_progress;
    TextView tv_FHR;
    TextView tv_allTime;
    TextView tv_monitor_time;
    TextView tv_nowTime;
    TextView tv_quickening_num;
    TextView tv_time;
    TextView tv_title;
    private ActivityHistoryBinding historyActivityBinding;

    /* access modifiers changed from: protected */
    @Override
    // android.support.v4.app.SupportActivity, android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity, com.laijiayiliao.myapplication.ui.BaseActivity
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);
        historyActivityBinding = DataBindingUtil.setContentView(this, R.layout.activity_history);
        historyActivityBinding.setLifecycleOwner(this);
        initView();
        initData();
        initAudio();
        setListeners();
    }

    private void initView() {
        iv_back = historyActivityBinding.ivBack;
        iv_play = historyActivityBinding.ivPlay;
        monitorView = historyActivityBinding.monitorView;
        sk_progress = historyActivityBinding.skProgress;
        tv_FHR = historyActivityBinding.tvFHR;
        tv_allTime = historyActivityBinding.tvAllTime;
        tv_monitor_time = historyActivityBinding.tvMonitorTime;
        tv_nowTime = historyActivityBinding.tvNowTime;
        tv_quickening_num = historyActivityBinding.tvQuickeningNum;
        tv_time = historyActivityBinding.tvTime;
        tv_title = historyActivityBinding.tvTitle;
    }

    /* access modifiers changed from: protected */
    @Override
    // android.support.v4.app.FragmentActivity, com.laijiayiliao.myapplication.ui.BaseActivity
    public void onResume() {
        super.onResume();
        this.monitorView.startHistoryMode();
        this.monitorView.setPause(false);
        this.monitorView.postDelayed(new Runnable() {
            /* class com.laijiayiliao.myapplication.ui.activity.HistoryActivity.AnonymousClass1 */

            public void run() {
                HistoryActivity.this.monitorView.setPause(true);
            }
        }, 50);
    }

    /* access modifiers changed from: protected */
    @Override // android.support.v4.app.FragmentActivity
    public void onPause() {
        playStop();
        this.monitorView.closeHistoryMode();
        super.onPause();
    }

    /* access modifiers changed from: protected */
    @Override // android.support.v7.app.AppCompatActivity, android.support.v4.app.FragmentActivity
    public void onDestroy() {
        this.mediaPlayer.release();
        this.mediaPlayer = null;
        super.onDestroy();
    }

    @Override // android.support.v4.app.FragmentActivity
    public void onBackPressed() {
        playStop();
        super.onBackPressed();
    }

    @Override // com.laijiayiliao.myapplication.ui.monitorView.OnFhrListener
    public void getFHR(int i, int i2) {
        int i3 = i * 500;
        this.tv_nowTime.setText(this.formatRecordingTime.format(Integer.valueOf(i3)));
        if (this.isPlayStart && ((long) this.progress) >= this.monitor_time) {
            this.monitorView.initial();
            this.progress = 0;
            playStop();
            this.sk_progress.setProgress(this.progress);
            Log.e(TAG, "playStop_progress: " + this.progress);
        } else if (!this.isTouchSB) {
            this.progress = i3;
            this.sk_progress.setProgress(this.progress);
        }
        Log.e(TAG, "setProgress: " + this.progress);
        if (i2 > 30) {
            TextView textView = this.tv_FHR;
            textView.setText("" + i2);
            return;
        }
        this.tv_FHR.setText("- - -");
    }

    private void initData() {
        this.dao = new HistoryDao(this);
        this.formatRecordingTime = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
        this.formatRecordingTime.applyLocalizedPattern("mm:ss");
        int id = getIntent().getIntExtra(Constant.HISTORY_INTENT, 0);
        this.historyData = this.dao.queryById(id);
        LogUtils.d("get id:" + id + "data:" + this.historyData);
        TextView textView = this.tv_time;
        textView.setText("录制时间：" + this.historyData.getTime());
        this.fhrData = (ArrayList) GsonUtil.getJsonData(this.historyData.getFhr_json(), new TypeToken<ArrayList<FHRData>>() {
            /* class com.laijiayiliao.myapplication.ui.activity.HistoryActivity.AnonymousClass2 */
        }.getType());
        this.monitorView.post(new Runnable() {
            /* class com.laijiayiliao.myapplication.ui.activity.HistoryActivity.AnonymousClass3 */

            public void run() {
                HistoryActivity.this.monitorView.addData(HistoryActivity.this.fhrData);
                HistoryActivity.this.monitor_time = HistoryActivity.this.monitorView.getRecordingTime(0);
                Log.e(HistoryActivity.TAG, "monitor_time: " + HistoryActivity.this.monitor_time);
                HistoryActivity.this.sk_progress.setMax((int) HistoryActivity.this.monitor_time);
                HistoryActivity.this.tv_monitor_time.setText(HistoryActivity.this.formatRecordingTime.format(Long.valueOf(HistoryActivity.this.monitor_time)));
                HistoryActivity.this.tv_allTime.setText(HistoryActivity.this.formatRecordingTime.format(Long.valueOf(HistoryActivity.this.monitor_time)));
                TextView textView = HistoryActivity.this.tv_quickening_num;
                textView.setText(HistoryActivity.this.monitorView.getQuickeningNum() + "");
            }
        });
    }

    private void initAudio() {
        this.correctThread = Executors.newSingleThreadExecutor();
        String fhrAudio = this.historyData.getFhrAudio();
        if (!new File(fhrAudio).exists()) {
            Toast.makeText(this, getResources().getString(R.string.audio_file_failed), Toast.LENGTH_LONG).show();
            Log.e(TAG, "initAudio:  audio_file:null");
            return;
        }
        this.mediaPlayer = new MediaPlayer();
        this.mediaPlayer.reset();
        try {
            this.mediaPlayer.setDataSource(fhrAudio);
            this.mediaPlayer.setAudioStreamType(2);
            this.mediaPlayer.prepare();
            this.audio_progress_max = this.mediaPlayer.getDuration();
            Log.e(TAG, "initAudio: " + this.audio_progress_max);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setListeners() {
        this.monitorView.setFhrListener(this);
        this.monitorView.setOnMonitorTouchListener(new OnMonitorTouchListener() {
            /* class com.laijiayiliao.myapplication.ui.activity.HistoryActivity.AnonymousClass4 */

            @Override // com.laijiayiliao.myapplication.ui.monitorView.OnMonitorTouchListener
            public void isTouchDown() {
                HistoryActivity.this.touchStart();
                HistoryActivity.this.setSBMove(false);
            }

            @Override // com.laijiayiliao.myapplication.ui.monitorView.OnMonitorTouchListener
            public void isTouchUp() {
                HistoryActivity.this.touchStop();
                HistoryActivity.this.setSBMove(true);
            }
        });
        this.sk_progress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            /* class com.laijiayiliao.myapplication.ui.activity.HistoryActivity.AnonymousClass5 */

            public void onProgressChanged(SeekBar seekBar, int i, boolean z) {
                if (HistoryActivity.this.isTouchSB) {
                    HistoryActivity.this.monitorView.setIndex(i / 500);
                    Log.e(HistoryActivity.TAG, "onProgressChanged: " + i);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                HistoryActivity.this.touchStart();
                HistoryActivity.this.isTouchSB = true;
                HistoryActivity.this.monitorView.setCanTouch(false);
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                HistoryActivity.this.touchStop();
                HistoryActivity.this.isTouchSB = false;
                HistoryActivity.this.monitorView.setCanTouch(true);
                Log.e(HistoryActivity.TAG, "onStopTrackingTouch: " + seekBar.getProgress());
            }
        });
        iv_back.setOnClickListener(this);
        iv_play.setOnClickListener(this);
    }

    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.iv_back) {
            playStop();
            finish();
        } else if (id == R.id.iv_play) {
            if (this.mediaPlayer == null) {
                Toast.makeText(this, getString(R.string.audio_file_failed), Toast.LENGTH_LONG).show();
                return;
            }
            Log.e(TAG, "onClick: , " + this.fhrData.size());
            if (!this.isPlayStart) {
                playStart();
                this.isPlayStart = true;
                return;
            }
            playStop();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void setSBMove(boolean z) {
        this.sk_progress.setFocusable(z);
        this.sk_progress.setClickable(z);
        this.sk_progress.setEnabled(z);
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void touchStart() {
        this.iv_play.setEnabled(false);
        if (!this.isPlayPause) {
            playPause();
        }
    }

    /* access modifiers changed from: private */
    /* access modifiers changed from: public */
    private void touchStop() {
        this.iv_play.setEnabled(true);
        this.monitorView.setPause(true);
        if (this.progress > 0) {
            this.mediaPlayer.seekTo(this.progress);
        }
        if (this.isPlayStart) {
            playStart();
        }
    }

    private void playStart() {
        Log.e(this.progress + " , " + this.monitor_time, new Object[0].toString());
        this.isPlayPause = false;
        this.monitorView.setPause(false);
        if (((long) this.progress) < this.monitor_time) {
            this.monitorView.setHistoryMode(true);
        }
        this.mediaPlayer.start();
        correct();
        this.iv_play.setImageResource(R.drawable.ic_stop);
        Log.e(TAG, "playStart: ");
    }

    private void playPause() {
        this.isPlayPause = true;
        this.monitorView.setHistoryMode(false);
        this.monitorView.postDelayed(new Runnable() {
            /* class com.laijiayiliao.myapplication.ui.activity.HistoryActivity.AnonymousClass6 */

            public void run() {
                HistoryActivity.this.monitorView.setPause(true);
            }
        }, 20);
        this.mediaPlayer.pause();
        this.iv_play.setImageResource(R.drawable.ic_play);
        Log.e(TAG, "playPause: ");
    }

    private void playStop() {
        if (this.isPlayStart) {
            this.isPlayStart = false;
            try {
                playPause();
                this.mediaPlayer.stop();
                this.mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "playStop: ");
        }
    }

    private void correct() {
        this.mediaPlayer.setVolume(0.0f, 0.0f);
        this.correctThread.execute(this.correctRunnable);
    }
}