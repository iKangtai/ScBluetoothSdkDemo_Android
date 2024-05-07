package com.example.blesdkdemo.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.blesdkdemo.BuildConfig;
import com.example.blesdkdemo.R;
import com.example.blesdkdemo.view.TopBar;
import com.ikangtai.bluetoothsdk.util.FileUtil;

/**
 * desc
 *
 * @author xiongyl 2021/4/1 22:24
 */
public class ChatActivity extends AppCompatActivity {
    private TopBar topBar;
    private WebView chatWebView;
    private PaxWebChromeClient chromeClient;
    private ViewGroup mViewParent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStatusBarIconColor();
        setContentView(R.layout.activity_chat_layout);
        topBar = findViewById(R.id.aliTopBar);
        mViewParent = findViewById(R.id.chatWebViewParent);
        chatWebView = new WebView(this, null);
        mViewParent.addView(chatWebView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        topBar.setOnTopBarClickListener(new TopBar.OnTopBarClickListener() {
            @Override
            public void leftClick() {
                onBackPressed();
            }

            @Override
            public void midLeftClick() {

            }

            @Override
            public void midRightClick() {

            }

            @Override
            public void rightClick() {

            }
        });

        if (chatWebView != null) {
            chatWebView.setVisibility(View.VISIBLE);
        }
        loadChatData();
    }

    private void loadChatData() {
        String url = getIntent().getStringExtra("url");

        chatWebView.getSettings().setJavaScriptEnabled(true);
        //chatWebView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        chatWebView.getSettings().setBlockNetworkImage(false);
        chatWebView.getSettings().setDomStorageEnabled(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            chatWebView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        chatWebView.getSettings().setUserAgentString(initUserAgent());
        //启用数据库
        chatWebView.getSettings().setDatabaseEnabled(true);
        //开启DomStorage缓存
        chatWebView.getSettings().setDomStorageEnabled(true);
        chatWebView.getSettings().setAllowContentAccess(true);
        chatWebView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        chromeClient = new PaxWebChromeClient(this, new PaxWebChromeClient.IEvent() {
            @Override
            public void receivedTitle(String title) {

            }
        });
        chatWebView.setWebChromeClient(chromeClient);
        chatWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }
        });
        chatWebView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                final WebView.HitTestResult hitTestResult = chatWebView.getHitTestResult();
                // 如果是图片类型或者是带有图片链接的类型
                if (hitTestResult.getType() == WebView.HitTestResult.IMAGE_TYPE ||
                        hitTestResult.getType() == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    // 弹出保存图片的对话框
                    AlertDialog.Builder builder = new AlertDialog.Builder(ChatActivity.this);
                    builder.setTitle(getString(R.string.tips));
                    builder.setMessage(getString(R.string.save_image_to_local));
                    builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String picUrl = hitTestResult.getExtra();//获取图片链接
                            //保存图片到相册
                            FileUtil.downloadImageFile(ChatActivity.this, picUrl);
                        }
                    });
                    builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        // 自动dismiss
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                    return true;
                }
                return false;
            }
        });
        chatWebView.loadUrl(url);
    }

    private String initUserAgent() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" BleSDKDemo");
        stringBuilder.append("/");
        stringBuilder.append(BuildConfig.VERSION_NAME);
        stringBuilder.append(" (");
        stringBuilder.append(Build.MODEL);
        stringBuilder.append(";");
        stringBuilder.append(" Android ");
        stringBuilder.append(Build.VERSION.RELEASE);
        stringBuilder.append(")");

        return stringBuilder.toString();
    }

    @Override
    public void onBackPressed() {
        if (chatWebView != null && chatWebView.canGoBack()) {
            chatWebView.goBack();
        } else {
            finish();
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (chromeClient != null) {
            chromeClient.onActivityResult(requestCode, resultCode, data);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void setStatusBarIconColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }


    static class PaxWebChromeClient extends WebChromeClient {
        private static final int CHOOSE_REQUEST_CODE = 0x9001;
        private Activity mActivity;
        private ValueCallback<Uri> uploadFile;
        private ValueCallback<Uri[]> uploadFiles;
        private IEvent event;


        public PaxWebChromeClient(@NonNull Activity mActivity, IEvent event) {
            this.mActivity = mActivity;
            this.event = event;
        }


        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public void onPermissionRequest(PermissionRequest request) {
            request.grant(request.getResources());
        }

        /**
         * For Android 3.0+
         *
         * @param uploadMsg
         * @param acceptType
         */
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            this.uploadFile = uploadFile;
            openFileChooseProcess();
        }

        /**
         * For Android < 3.0
         *
         * @param uploadMsgs
         */
        public void openFileChooser(ValueCallback<Uri> uploadMsgs) {
            this.uploadFile = uploadFile;
            openFileChooseProcess();
        }

        /**
         * For Android  > 4.1.1
         *
         * @param uploadMsg
         * @param acceptType
         * @param capture
         */
        //@Override
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
            this.uploadFile = uploadFile;
            openFileChooseProcess();
        }

        /**
         * For Android  >= 5.0
         *
         * @param webView
         * @param filePathCallback
         * @param fileChooserParams
         * @return
         */
        @Override
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> filePathCallback,
                                         FileChooserParams fileChooserParams) {
            this.uploadFiles = filePathCallback;
            openFileChooseProcess();
            return true;
        }

        private void openFileChooseProcess() {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("*/*");
            mActivity.startActivityForResult(Intent.createChooser(i, "Choose"),
                    CHOOSE_REQUEST_CODE);
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode == Activity.RESULT_OK) {
                switch (requestCode) {
                    case CHOOSE_REQUEST_CODE:
                        if (null != uploadFile) {
                            Uri result = data == null || resultCode != Activity.RESULT_OK ? null
                                    : data.getData();
                            uploadFile.onReceiveValue(result);
                            uploadFile = null;
                        }
                        if (null != uploadFiles) {
                            Uri result = data == null || resultCode != Activity.RESULT_OK ? null
                                    : data.getData();
                            uploadFiles.onReceiveValue(new Uri[]{result});
                            uploadFiles = null;
                        }
                        break;
                    default:
                        break;
                }
            } else if (resultCode == Activity.RESULT_CANCELED) {
                if (null != uploadFile) {
                    uploadFile.onReceiveValue(null);
                    uploadFile = null;
                }
                if (null != uploadFiles) {
                    uploadFiles.onReceiveValue(null);
                    uploadFiles = null;
                }
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (event != null) {
                event.receivedTitle(title);
            }
        }

        public interface IEvent {
            void receivedTitle(String title);
        }
    }

}
