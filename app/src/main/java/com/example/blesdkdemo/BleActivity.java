package com.example.blesdkdemo;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class BleActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {
    public static final int MY_PERMISSION_REQUEST_CODE = 1000;
    private static final String TAG = "MainActivity_TAG";
    public static ImageView iv_connect;
    DrawerLayout drawer_layout;
    //private HistoryListFragment historyFragment;
    LinearLayout layout_error;
    /* access modifiers changed from: private */
    public MonitorBLEFragment mainBLEFragment;
    /* access modifiers changed from: private */
    //public MonitorLineFragment mainLineFragment;
    /* access modifiers changed from: private */
    public int mode;
    private String[] my_permission = {"android.permission.ACCESS_COARSE_LOCATION", "android.permission.RECORD_AUDIO"};
    NavigationView nav_view;
    RelativeLayout rl_fragment;

    Toolbar toolbar;
    TextView tv_error;
    TextView tv_title;

    /* access modifiers changed from: protected */
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_txy_ble);
        initView();
        requestPermission();
        initViews();
        initFragments();
    }

    private void initView() {
        iv_connect = findViewById(R.id.iv_connect);
        drawer_layout = findViewById(R.id.drawer_layout);
        layout_error = findViewById(R.id.layout_error);
        nav_view = findViewById(R.id.nav_view);
        rl_fragment = findViewById(R.id.rl_fragment);

        toolbar = findViewById(R.id.toolbar);
        tv_error = findViewById(R.id.tv_error);
        tv_title = findViewById(R.id.tv_title);
    }

    public void onBackPressed() {
        if (this.drawer_layout.isDrawerOpen((int) GravityCompat.START)) {
            this.drawer_layout.closeDrawer((int) GravityCompat.START);
            return;
        }
        if (!this.tv_title.getText().toString().equals(getResources().getString(R.string.monitor))) {
            showMainFragment();
        } else {
            exitApp();
        }
    }

    private void exitApp() {
        if (BaseMonitorFragment.isRecording) {
            Toast.makeText(this, getResources().getString(R.string.exit_if_recording), Toast.LENGTH_SHORT).show();
        } else {
            new AlertDialog.Builder(this).setCancelable(false).setMessage((CharSequence) getResources().getString(R.string.exit_app)).setPositiveButton((CharSequence) getResources().getString(R.string.confirm), (DialogInterface.OnClickListener) new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialogInterface, int i) {
                    System.exit(0);
                }
            }).setNegativeButton((CharSequence) getResources().getString(R.string.cancel), (DialogInterface.OnClickListener) null).show();
        }
    }

    private void requestPermission() {
        String[] strArr = this.my_permission;
        int length = strArr.length;
        boolean z = false;
        int i = 0;
        while (true) {
            if (i >= length) {
                break;
            } else if (ContextCompat.checkSelfPermission(this, strArr[i]) != 0) {
                z = true;
                break;
            } else {
                i++;
            }
        }
        if (z) {
            ActivityCompat.requestPermissions(this, this.my_permission, 1000);
        }
    }

    private void initViews() {
        this.nav_view.setNavigationItemSelectedListener(this);
        this.tv_title.setText(R.string.monitor);
        ActionBarDrawerToggle actionBarDrawerToggle = new ActionBarDrawerToggle(this, this.drawer_layout, this.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        this.drawer_layout.addDrawerListener(actionBarDrawerToggle);
        actionBarDrawerToggle.syncState();
        this.toolbar.setNavigationIcon((int) R.drawable.ic_toolbar_left_menu_24dp);
    }

    public void setWarning(boolean z, @NonNull String str) {
        if (this.mode != 1 || !z) {
            this.layout_error.setVisibility(View.GONE);
            return;
        }
        this.layout_error.setVisibility(View.VISIBLE);
        this.tv_error.setText(str);
    }

    public void setWarningOnClickListener(View.OnClickListener onClickListener) {
        this.layout_error.setOnClickListener(onClickListener);
    }

    public static void setConnectIcon(@DrawableRes int i) {
        iv_connect.setImageResource(i);
    }

    private void initFragments() {
        if (this.mainBLEFragment == null || !this.mainBLEFragment.isAdded()) {
            this.mainBLEFragment = new MonitorBLEFragment();
            //this.mainLineFragment = new MonitorLineFragment();
            //this.historyFragment = new HistoryListFragment();

            this.mode = getSharedPreferences(Constant.MODE, 0).getInt(Constant.MODE, 0);
            getSupportFragmentManager().beginTransaction().add((int) R.id.rl_fragment, (Fragment) this.mainBLEFragment).commit();
//            if (this.mode == 1) {
//            } else {
//                getSupportFragmentManager().beginTransaction().add((int) R.id.rl_fragment, (Fragment) this.mainLineFragment).add((int) R.id.rl_fragment, (Fragment) this.historyFragment).hide(this.historyFragment).add((int) R.id.rl_fragment, (Fragment) this.aboutFragment).hide(this.aboutFragment).add((int) R.id.rl_fragment, (Fragment) this.setFragment).hide(this.setFragment).commit();
//            }
        }
    }

    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
//            case R.id.nav_history:
//                showHistoryFragment();
//                break;
            case R.id.nav_main:
                showMainFragment();
                break;
        }
        this.drawer_layout.closeDrawer((int) GravityCompat.START);
        return true;
    }
    /*private void showMainFragment() {
        if (this.mode == 1) {
            getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).show(this.mainBLEFragment).hide(this.historyFragment).hide(this.setFragment).hide(this.aboutFragment).commit();
        }
        if (this.mode == 2) {
            getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).show(this.mainLineFragment).hide(this.historyFragment).hide(this.setFragment).hide(this.aboutFragment).commit();
        }
        this.tv_title.setText(getResources().getString(R.string.monitor));
    }*/

    private void showMainFragment() {
//        if (this.mode == 1) {
//            /*hide(this.historyFragment).hide(this.setFragment).hide(this.aboutFragment)*/
//            getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).show(this.mainBLEFragment).commit();
//        }
//        if (this.mode == 2) {
//            //hide(this.historyFragment).hide(this.setFragment).hide(this.aboutFragment)
//            getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).show(this.mainBLEFragment).commit();
//        }
        getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).show(this.mainBLEFragment).commit();
        this.tv_title.setText(getResources().getString(R.string.monitor));
    }

//    private void showHistoryFragment() {
//        String string = getResources().getString(R.string.history);
//        if (!this.tv_title.getText().toString().equals(string)) {
//            if (this.mode == 1) {
//                getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).show(this.historyFragment).hide(this.mainBLEFragment).hide(this.setFragment).hide(this.aboutFragment).commit();
//            }
//            if (this.mode == 2) {
//                getSupportFragmentManager().beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).show(this.historyFragment).hide(this.mainLineFragment).hide(this.setFragment).hide(this.aboutFragment).commit();
//            }
//            this.tv_title.setText(string);
//        }
//    }


}