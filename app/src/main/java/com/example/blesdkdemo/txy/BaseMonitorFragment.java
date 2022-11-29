package com.example.blesdkdemo.txy;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.blesdkdemo.AppInfo;
import com.example.blesdkdemo.Constant;
import com.example.blesdkdemo.R;
import com.example.blesdkdemo.databinding.FragmentMonitorBinding;
import com.example.blesdkdemo.txy.db.HistoryBean;
import com.example.blesdkdemo.txy.db.HistoryDao;
import com.example.blesdkdemo.txy.ui.FHRData;
import com.example.blesdkdemo.txy.ui.FHRMonitorView;
import com.example.blesdkdemo.txy.ui.FhrAllData;
import com.example.blesdkdemo.util.GsonUtil;
import com.example.blesdkdemo.util.Util;
import com.ikangtai.bluetoothsdk.info.TxyRecordInfo;
import com.ikangtai.bluetoothsdk.model.TxyRecordModel;
import com.ikangtai.bluetoothsdk.util.LogUtils;
import com.ikangtai.bluetoothsdk.util.PcmToWavUtil;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.UUID;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

/**
 * Fetal Heart Monitoring
 *
 * @author xiongyl 2020/9/24 0:55
 */
public class BaseMonitorFragment extends BaseFragment {
    private static final String TAG = "BaseMainFragment_TAG";
    public static boolean isRecording = false;
    protected String audio_path_name;
    protected View bottom_view;
    protected Button btn_quickening;
    protected Button btn_switch;
    private long click_quickening_old;
    protected int fhrEndIndex;
    protected int fhrStartIndex;
    protected SimpleDateFormat formatRecordingTime;
    private SimpleDateFormat formatSaveAudioNameTime;
    private SimpleDateFormat formatSaveTime;
    protected boolean isConnected = false;
    protected boolean isCreated = true;
    private boolean isFragmentHidden = false;
    protected boolean isRunning = true;
    protected ImageView iv_fhr_icon;
    protected int mFHR;
    protected int heartIconFlag;
    protected BleActivity mainActivity;
    protected FHRMonitorView monitorView;
    protected boolean needPermission = false;
    private int quickening_num = 0;
    protected String save_time;
    protected TextView tv_FHR;
    protected TextView tv_monitor_time;
    protected TextView tv_quickening_num;
    protected HistoryDao historyDao;
    protected FragmentMonitorBinding fragmentMainBinding;

    @Nullable
    public View onCreateView(@NonNull LayoutInflater layoutInflater, @Nullable ViewGroup viewGroup, @Nullable Bundle bundle) {
        fragmentMainBinding = FragmentMonitorBinding.inflate(layoutInflater, viewGroup, false);
        fragmentMainBinding.setLifecycleOwner(this);
        this.mView = fragmentMainBinding.getRoot();
        return this.mView;
    }

    public void initView() {
        bottom_view = fragmentMainBinding.bottomView;
        btn_quickening = fragmentMainBinding.btnQuickening;
        btn_switch = fragmentMainBinding.btnSwitch;
        iv_fhr_icon = fragmentMainBinding.ivFhrIcon;
        monitorView = fragmentMainBinding.monitorView;
        tv_FHR = fragmentMainBinding.tvFHR;
        tv_monitor_time = fragmentMainBinding.tvMonitorTime;
        tv_quickening_num = fragmentMainBinding.tvQuickeningNum;
    }

    public void onActivityCreated(@Nullable Bundle bundle) {
        super.onActivityCreated(bundle);
        initView();
        initDB();
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        this.mainActivity = (BleActivity) this.mActivity;
    }

    public void onPause() {
        super.onPause();
        this.isRunning = false;
        this.monitorView.setConnect(false);
    }

    public void onStop() {
        super.onStop();
        this.monitorView.setBreakType(1);
    }

    public void onResume() {
        super.onResume();
        this.isRunning = true;
        this.mFHR = 0;
        this.heartIconFlag = 0;
        changeSize();
        this.monitorView.setConnect(this.isConnected);
        if (!this.isFragmentHidden) {
            this.monitorView.postDelayed(new Runnable() {
                public void run() {
                    BaseMonitorFragment.this.monitorView.setPause(false);
                }
            }, 200);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        this.isCreated = false;
        if (isRecording) {
            File file = new File(this.audio_path_name);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);
        this.isFragmentHidden = hidden;
        if (hidden) {
            this.monitorView.setPause(true);
        } else {
            this.monitorView.setPause(false);
        }
    }

    /**
     * heart icon flash ui
     */
    protected class IconFlashTH extends Thread {
        protected IconFlashTH() {
        }

        public void run() {
            super.run();
            while (BaseMonitorFragment.this.isCreated) {
                try {
                    if (!BaseMonitorFragment.this.isRunning) {
                        sleep(20);
                    } else if (BaseMonitorFragment.this.mFHR <= 0) {
                        sleep(20);
                    } else {
                        final int time = 30000 / BaseMonitorFragment.this.mFHR;
                        if (BaseMonitorFragment.this.heartIconFlag == 1) {
                            BaseMonitorFragment.this.iv_fhr_icon.postDelayed(new Runnable() {
                                public void run() {
                                    if (BaseMonitorFragment.this.iv_fhr_icon != null) {
                                        BaseMonitorFragment.this.iv_fhr_icon.setVisibility(View.VISIBLE);
                                        BaseMonitorFragment.this.iv_fhr_icon.postDelayed(new Runnable() {
                                            public void run() {
                                                if (BaseMonitorFragment.this.iv_fhr_icon != null) {
                                                    BaseMonitorFragment.this.iv_fhr_icon.setVisibility(View.INVISIBLE);
                                                }
                                            }
                                        }, (long) time);
                                    }
                                }
                            }, (long) time);
                        }
                        sleep((long) (time * 2));
                    }
                } catch (Exception e) {
                    Log.e(BaseMonitorFragment.TAG, "Exception: ", e);
                }
            }
        }
    }

    /**
     * set quicken num
     */
    public void setQuickening() {
        if (this.click_quickening_old == 0) {
            this.click_quickening_old = System.currentTimeMillis();
        } else {
            long currentTimeMillis = System.currentTimeMillis();
            if (currentTimeMillis - this.click_quickening_old < 5000) {
                Toast.makeText(this.mContext, getResources().getString(R.string.quickening_prompt), Toast.LENGTH_SHORT).show();
                return;
            }
            this.click_quickening_old = currentTimeMillis;
        }
        this.monitorView.setQuickening(true);
        this.quickening_num++;
        this.tv_quickening_num.setText(this.quickening_num + "");
    }

    public void recordStart() {
        isRecording = true;
        long currentTimeMillis = System.currentTimeMillis();
        this.save_time = this.formatSaveTime.format(Long.valueOf(currentTimeMillis));
        String format = this.formatSaveAudioNameTime.format(Long.valueOf(currentTimeMillis));
        this.audio_path_name = getAudioPath(this.mContext) + "/audio_" + format + ".pcm";
        this.fhrStartIndex = this.monitorView.getFHRIndex();
        setMonitorTime();
        this.btn_switch.setText(R.string.recordStop);
        Toast.makeText(this.mContext, getResources().getString(R.string.recordStart), Toast.LENGTH_SHORT).show();
    }

    public static String getAudioPath(Context context) {
        String storagePath;
        File contextExternalFilesDir = context.getExternalFilesDir(null);
        if (contextExternalFilesDir == null) {
            contextExternalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC);
            storagePath = contextExternalFilesDir.getAbsolutePath();
        } else {
            storagePath = contextExternalFilesDir.getAbsolutePath() + "/audio";
        }
        File file = new File(storagePath);
        if (!file.exists()) {
            file.mkdir();
        }
        return storagePath;
    }


    public void recordEnd(String str) {
        isRecording = false;
        this.fhrEndIndex = this.monitorView.getFHRIndex();
        this.tv_monitor_time.setText("00:00");
        this.btn_switch.setText(R.string.record);
        showSaveDataDialog(str);
    }

    /**
     * update recording time
     */
    public void setMonitorTime() {
        new Thread() {
            public void run() {
                setPriority(1);
                while (BaseMonitorFragment.isRecording) {
                    try {
                        final String format = BaseMonitorFragment.this.formatRecordingTime.format(Long.valueOf(BaseMonitorFragment.this.monitorView.getRecordingTime(BaseMonitorFragment.this.fhrStartIndex)));
                        BaseMonitorFragment.this.mActivity.runOnUiThread(new Runnable() {
                            public void run() {
                                BaseMonitorFragment.this.tv_monitor_time.setText(format);
                            }
                        });
                        sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return;
                    }
                }
            }
        }.start();
    }

    private void showSaveDataDialog(String str) {
        new AlertDialog.Builder(this.mContext).setCancelable(false).setMessage((CharSequence) str).setPositiveButton((CharSequence) getResources().getString(R.string.save), (DialogInterface.OnClickListener) new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                BaseMonitorFragment.this.saveData();
            }
        }).setNegativeButton((CharSequence) getResources().getString(R.string.cancel), (DialogInterface.OnClickListener) new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                File file = new File(BaseMonitorFragment.this.audio_path_name);
                if (file.exists()) {
                    file.delete();
                }
                Toast.makeText(BaseMonitorFragment.this.mContext, BaseMonitorFragment.this.getResources().getString(R.string.isCancel), Toast.LENGTH_SHORT).show();
            }
        }).setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialogInterface) {
                BaseMonitorFragment.this.tv_monitor_time.setText("00:00");
            }
        }).show();
    }

    /**
     * save record data
     */
    public void saveData() {
        new Thread() {
            public void run() {
                final ArrayList<FHRData> saveFHR = BaseMonitorFragment.this.monitorView.getSaveFHR(BaseMonitorFragment.this.fhrStartIndex, BaseMonitorFragment.this.fhrEndIndex);
                String audioFilePath = BaseMonitorFragment.this.audio_path_name.replace(".pcm", ".wav");
                final boolean pcmToWav = new PcmToWavUtil().pcmToWav(BaseMonitorFragment.this.audio_path_name, audioFilePath);
                final HistoryBean historyBean = new HistoryBean(BaseMonitorFragment.this.save_time, GsonUtil.toJson(saveFHR), audioFilePath);
                final int addHistory = pcmToWav ? BaseMonitorFragment.this.historyDao.addHistory(historyBean) : 0;

                final TxyRecordInfo recordInfo = new TxyRecordInfo();
                recordInfo.setRecordId(UUID.randomUUID().toString());
                recordInfo.setSdkVersion(AppInfo.getInstance().getVersion());
                recordInfo.setPhoneModel(AppInfo.getPhoneProducer() + " " + AppInfo.getPhoneModel());
                recordInfo.setAudioFile(new File(audioFilePath));
                recordInfo.setFileExtension("wav");

                FhrAllData fhrAllData = BaseMonitorFragment.this.monitorView.getSaveAllFHR(BaseMonitorFragment.this.fhrStartIndex, BaseMonitorFragment.this.fhrEndIndex);
                recordInfo.setHistory(GsonUtil.toJson(fhrAllData));
                recordInfo.setDuration((int) BaseMonitorFragment.this.monitorView.getRecordingTime(BaseMonitorFragment.this.fhrStartIndex) / 1000);
                recordInfo.setQuickening(BaseMonitorFragment.this.monitorView.getQuickeningNum());
                recordInfo.setAverageFhr(BaseMonitorFragment.this.monitorView.getAvaFHR());
                recordInfo.setRecordCreateTime(System.currentTimeMillis() / 1000);

                //胎心记录保存SAAS服务器
                TxyRecordModel.saveTxyRecordInfo(recordInfo);
                BaseMonitorFragment.this.mActivity.runOnUiThread(new Runnable() {
                    public void run() {
                        if (!pcmToWav) {
                            Toast.makeText(BaseMonitorFragment.this.mContext, BaseMonitorFragment.this.getResources().getString(R.string.save_failed), Toast.LENGTH_SHORT).show();
                        } else if (addHistory > 0) {
                            LogUtils.d("save id:" + historyBean.getId() + "data:" + GsonUtil.toJson(saveFHR));
                            Toast.makeText(BaseMonitorFragment.this.mContext, BaseMonitorFragment.this.getResources().getString(R.string.save_succeed), Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getContext(), HistoryActivity.class);
                            intent.putExtra(Constant.HISTORY_INTENT, historyBean.getId());
                            startActivity(intent);
                        } else {
                            Toast.makeText(BaseMonitorFragment.this.mContext, BaseMonitorFragment.this.getResources().getString(R.string.save_failed), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }.start();
    }

    private void initDB() {
        this.historyDao = new HistoryDao(this.mContext);
        this.formatSaveTime = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
        this.formatSaveTime.applyLocalizedPattern("yyyy-MM-dd HH:mm:ss");
        this.formatSaveAudioNameTime = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
        this.formatSaveAudioNameTime.applyLocalizedPattern("yyyy-MM-dd HH_mm_ss");
        this.formatRecordingTime = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
        this.formatRecordingTime.applyLocalizedPattern("mm:ss");
    }

    private void changeSize() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        this.mActivity.getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        Log.e(TAG, "changeSize: W " + displayMetrics.widthPixels + " h " + displayMetrics.heightPixels + ", " + (((float) displayMetrics.heightPixels) / ((float) displayMetrics.widthPixels)) + ", " + 1.7777778f);
        if (((float) displayMetrics.heightPixels) / ((float) displayMetrics.widthPixels) > 1.7777778f) {
            LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) this.monitorView.getLayoutParams();
            layoutParams.weight = 0.0f;
            layoutParams.height = (int) Util.dp2px(440);
            this.monitorView.setLayoutParams(layoutParams);
        }
        this.bottom_view = this.mView.findViewById(R.id.bottom_view);
        if (Util.getNavigationBarHeight(this.mActivity) > 0) {
            this.bottom_view.setVisibility(View.VISIBLE);
        } else {
            this.bottom_view.setVisibility(View.GONE);
        }
    }
}