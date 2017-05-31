package jp.co.casio.exilim_sample_application;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.deviceconnect.message.DConnectEventMessage;
import org.deviceconnect.message.DConnectMessage;
import org.deviceconnect.message.DConnectResponseMessage;
import org.deviceconnect.message.DConnectSDK;
import org.deviceconnect.message.DConnectSDKFactory;
import org.json.JSONObject;

import static android.view.MotionEvent.ACTION_DOWN;

public class MainActivity extends AppCompatActivity {

    // Toast用 エラー定義
    private static final int ERROR_PLUGIN_CAMERA_SETUP = 10;
    private static final int ERROR_PLUGIN_CAMERA_WIFI_CONNECT = 11;

    // EditText管理変数
    private EditText mTextUrl;
    // Plugin制御
    private PluginControl mPluginCtrl;
    // Plugin応答
    private boolean sb_PluginRet;

    // WebView管理変数(LiveView用)
    private WebView mWebViewLive;
    // WebView管理変数(Thumbnail用)
    private WebView mWebViewThumbnail;

    // Plugin Setup Button管理
    private Button mButtonPluginSetUp;
    // LiveView Start Button管理
    private Button mButtonLiveViewStart;
    // LiveView Stop Button管理
    private Button mButtonLiveViewStop;
    // Take Photo Button管理
    private Button mButtonTakePhoto;
    // Movie Button管理
    private Button mButtonMovie;

    // 状態ON/OFF管理フラグ
    private boolean sb_ExilimPluginON;
    private boolean sb_MovieCaptureON;
    private boolean sb_ZoomWideState;

    // 静止画/動画 撮影後のパス保持
    private String mCaptureFilePath = null;
    // プレビューURL保持FilePath
    private String mPreviewUri = null;
    // サムネイルURL保持
    private String mThumbnailUri = null;
    // SDK管理変数
    private static DConnectSDK mSDK_Ctrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 起動時にEDITテキストにフォーカスされないよう設定
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);

        // Plugin Control Class制御用
        mPluginCtrl = new PluginControl();
        // 編集テキスト用
        mTextUrl = (EditText) findViewById(R.id.text_url_p);

        // 画像表示用
        mWebViewLive = (WebView) findViewById(R.id.webViewForLive);
        mWebViewThumbnail = (WebView) findViewById(R.id.webViewForThumbnail);

        // 値の初期化
        mTextUrl.setText("localhost");
        sb_ExilimPluginON = false;
        sb_MovieCaptureON = false;
        sb_ZoomWideState = false;

        // 各ボタンや画像のイベントリスナー登録処理
        setPluginSetupButtonEventListener();
        setLiveViewStartButtonEventListener();
        setLiveViewStopButtonEventListener();
        setTakePhotoButtonEventListener();
        setMovieButtonEventListener();
        setLiveWebViewEventListener();
        setThumbnailEventListener();
    }

    //---------------------------------------------------------------------------------------------
    // ボタン制御処理
    //---------------------------------------------------------------------------------------------
    // "PluginSetUp"ボタン クリックイベントの登録
    private void setPluginSetupButtonEventListener() {

        mButtonPluginSetUp = (Button) findViewById(R.id.buttonPluginSetUp);
        // "PluginSetUp"ボタンのクリック処理対応
        mButtonPluginSetUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                (new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (sb_ExilimPluginON){
                            sb_ExilimPluginON = false;
                            sb_PluginRet = true;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mWebViewLive.loadUrl("about:blank");
                                    mWebViewThumbnail.loadUrl("about:blank");
                                }
                            });
                        }
                        else {
                            if (!mPluginCtrl.sendExilimPluginSetUp(mTextUrl.getText().toString())) {
                                sb_PluginRet = false;
                            }
                            else {
                                sb_ExilimPluginON = true;
                                sb_PluginRet = true;
                            }
                        }
                        if (!sb_PluginRet){
                            displayErrorToast(ERROR_PLUGIN_CAMERA_SETUP);
                        }
                    }
                })).start();
            }
        });
    }

    // "LiveViewStart"ボタン クリックイベントの登録
    private void setLiveViewStartButtonEventListener() {

        mButtonLiveViewStart = (Button) findViewById(R.id.buttonLiveViewStart);
        // "LiveViewStart"ボタンのクリック処理対応
        mButtonLiveViewStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sb_ExilimPluginON) {
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            sb_PluginRet = startPreview();
                            if (!sb_PluginRet) {
                                displayErrorToast(ERROR_PLUGIN_CAMERA_WIFI_CONNECT);
                            }
                        }
                    })).start();
                }
            }
        });
    }

    // "LiveViewStop"ボタン クリックイベントの登録
    private void setLiveViewStopButtonEventListener() {

        mButtonLiveViewStop = (Button) findViewById(R.id.buttonLiveViewStop);
        // "LiveViewStop"ボタンのクリック処理対応
        mButtonLiveViewStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sb_ExilimPluginON) {
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mPluginCtrl.sendExilimLiveViewEnd();
                        }
                    })).start();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWebViewLive.loadUrl("about:blank");
                            mWebViewThumbnail.loadUrl("about:blank");
                        }
                    });
                }
            }
        });
    }

    // "TakePhoto"ボタン クリックイベントの登録
    private void setTakePhotoButtonEventListener() {

        mButtonTakePhoto = (Button) findViewById(R.id.buttonTakePhoto);
        // "TakePhoto"ボタンのクリック処理対応
        mButtonTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sb_ExilimPluginON) {
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            takePhotoCommandSequence();
                        }
                    })).start();
                }
            }
        });
    }

    // "Movie"ボタン クリックイベントの登録
    private void setMovieButtonEventListener() {

        mButtonMovie = (Button) findViewById(R.id.buttonMovie);
        // "TakePhoto"ボタンのクリック処理対応
        mButtonMovie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sb_ExilimPluginON) {
                    (new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (sb_MovieCaptureON){
                                sendMovieStopWithOnChangeEvent();
                                sb_MovieCaptureON = false;
                            }
                            else {
                                // 動画撮影開始
                                sb_MovieCaptureON = true;
                                sendMovieRecordWithOnChangeEvent();
                            }
                        }
                    })).start();
                }
            }
        });
    }

    // LiveView表示用 WebView クリックイベントの登録
    private void setLiveWebViewEventListener() {
        mWebViewLive.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (sb_ExilimPluginON) {
                    if (event.getAction() == ACTION_DOWN) {
                        (new Thread(new Runnable() {
                            @Override
                            public void run() {
                                changeWebViewEnabled(false);
                                if (sb_ZoomWideState) {
                                    mPluginCtrl.sendExilimZoomIn();
                                    sb_ZoomWideState = false;
                                } else {
                                    mPluginCtrl.sendExilimZoomOut();
                                    sb_ZoomWideState = true;
                                }
                                sleep(2000);
                                changeWebViewEnabled(true);
                            }
                        })).start();
                    }
                }
                return true;
            }
        });
    }

    // LiveView表示用 WebView クリックイベントの登録
    private void setThumbnailEventListener() {
        mWebViewThumbnail.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (sb_ExilimPluginON) {
                    if (event.getAction() == ACTION_DOWN) {
                        (new Thread(new Runnable() {
                            @Override
                            public void run() {
                                getCaptureFileWithDownload();
                            }
                        })).start();
                    }
                }
                return true;
            }
        });
    }

    //-----------------------------------------------------
    // カメラ用 各イベント内のサブルーチン
    //-----------------------------------------------------
    // プレビュー開始
    private boolean startPreview() {
        String str;
        boolean vb_Ret;

        vb_Ret = false;

        str = mPluginCtrl.sendExilimLiveViewStart();

        if (str != null) {
            try {
                //JSON解析
                JSONObject root = new JSONObject(str);

                if (root != null) {
                    //WEBサーバからデータ取得
                    mPreviewUri = root.getString("uri");

                    this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mWebViewLive.setWebViewClient(new WebViewClient());

                            mWebViewLive.getSettings().setUseWideViewPort(true);
                            mWebViewLive.getSettings().setLoadWithOverviewMode(true);
                            Log.i("LiveView:", mPreviewUri);
                            mWebViewLive.loadUrl(mPreviewUri);
                        }
                    });
                    vb_Ret = true;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return vb_Ret;
    }

    // 写真撮影シーケンス処理 (サムネイル取得まで)
    private void takePhotoCommandSequence(){

        // 表示スレッド上で サムネイル表示WebViewのクリア
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebViewThumbnail.loadUrl("about:blank");
                mWebViewThumbnail.setEnabled(false);
            }
        });

        mCaptureFilePath = mPluginCtrl.sendExilimTakePhoto();

        if (mCaptureFilePath != null){
            mThumbnailUri = mPluginCtrl.getExilimThumbnailImage(mCaptureFilePath);
        }
        else {
            mThumbnailUri = null;
        }

        if (mThumbnailUri != null){
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWebViewThumbnail.setWebViewClient(new WebViewClient());
                    mWebViewThumbnail.getSettings().setUseWideViewPort(true);
                    mWebViewThumbnail.getSettings().setLoadWithOverviewMode(true);
                    Log.i("Thumb:", mThumbnailUri);
                    mWebViewThumbnail.loadUrl(mThumbnailUri.toLowerCase());
                    mWebViewThumbnail.setEnabled(true);
                }
            });
        }
    }

    // 動画撮影開始(撮影状態取得版)
    private void sendMovieRecordWithOnChangeEvent() {

        mPluginCtrl.sendExilimMovieStateChangeEventRegister();

        if (mSDK_Ctrl == null) {
            mSDK_Ctrl = DConnectSDKFactory.create(getApplicationContext(), DConnectSDKFactory.Type.HTTP);
        }

        mSDK_Ctrl.connectWebSocket(new DConnectSDK.OnWebSocketListener() {
            @Override
            public void onOpen() {
                Log.i("test:WebSocket", "Open");

                String url = mPluginCtrl.getExilimMovieStateChangeEventURL();
                mSDK_Ctrl.addEventListener(url, new DConnectSDK.OnEventListener() {
                    @Override
                    public void onResponse(DConnectResponseMessage dConnectResponseMessage) {
                        Log.i("test:WebSocketEvent R", dConnectResponseMessage.toString());
                    }

                    @Override
                    public void onMessage(DConnectEventMessage dConnectEventMessage) {
                        Log.i("test:WebSocketEvent M", dConnectEventMessage.toString());
                        // TODO : 状態変化による処理がある場合はここに追加
                    }
                });
            }
            @Override
            public void onClose() {
                Log.i("test:WebSocket", "Close");
            }
            @Override
            public void onError(Exception e) {
                Log.i("test:WebSocket", "Error");
            }
        });

        mCaptureFilePath = mPluginCtrl.sendExilimMovieStart();
        // 動画停止が完了するまでこの処理を抜けてこないはず。

        // 停止処理
        mPluginCtrl.sendExilimMovieStateChangeEventUnregister();
        // WebSocketの削除処理
        String url = mPluginCtrl.getExilimMovieStateChangeEventURL();
        mSDK_Ctrl.removeEventListener(url);
        mSDK_Ctrl.disconnectWebSocket();

        if (mCaptureFilePath != null){
            mThumbnailUri = mPluginCtrl.getExilimThumbnailImage(mCaptureFilePath);
        }
        else {
            mThumbnailUri = null;
        }

        if (mThumbnailUri != null){
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mWebViewThumbnail.setWebViewClient(new WebViewClient());
                    mWebViewThumbnail.getSettings().setUseWideViewPort(true);
                    mWebViewThumbnail.getSettings().setLoadWithOverviewMode(true);
                    Log.i("Thumb:", mThumbnailUri);
                    mWebViewThumbnail.loadUrl(mThumbnailUri.toLowerCase());
                    mWebViewThumbnail.setEnabled(true);
                }
            });
        }
    }

    // 動画撮影停止(撮影状態取得版)
    private void sendMovieStopWithOnChangeEvent() {
        // 動画撮影停止
        mPluginCtrl.sendExilimMovieStop();
    }

    // ファイル取得
    private void getCaptureFileWithDownload(){

        if (mCaptureFilePath != null){
            mThumbnailUri = mPluginCtrl.getExilimCaptureFile(mCaptureFilePath);
        }
        else {
            mThumbnailUri = null;
        }

        displayFileGetToast(mThumbnailUri);
    }

    // WebViewのクリック有効/無効設定
    private void changeWebViewEnabled(final boolean vb_flag){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mWebViewLive.setEnabled(vb_flag);
            }
        });
    }

    //=============================================================================================
    // トースト処理関数
    //=============================================================================================
    // トーストERROR表示処理
    private void displayErrorToast(final int ErrorID){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast;
                String dispErr;

                switch (ErrorID){
                    case ERROR_PLUGIN_CAMERA_SETUP:
                        dispErr = "HTTP ERROR:" + ErrorID +"\n" +
                                "Plugin Setting Failed\n" +
                                "Check DeviceWebAPI State\n" +
                                "@Exilim Plugin";
                        break;
                    case ERROR_PLUGIN_CAMERA_WIFI_CONNECT:
                        dispErr = "HTTP ERROR:" + ErrorID +"\n" +
                                "Command Request Failed\n" +
                                "Check Camera WiFi Connection";
                        break;
                    default:
                        dispErr = "UNKNOWN ERROR";
                        break;
                }

                toast = Toast.makeText(getApplicationContext(),
                        dispErr,
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CLIP_HORIZONTAL | Gravity.CLIP_VERTICAL, 0, 0);
                toast.show();
            }
        });
    }

    // トースト ファイル取得通知
    private void displayFileGetToast(final String vstr_file_path){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast toast;
                String dispString;

                if (vstr_file_path == null){
                    dispString = "File Not Found\n";
                }
                else {
                    dispString = "Get File Complete\n" + vstr_file_path;
                }

                toast = Toast.makeText(getApplicationContext(),
                        dispString,
                        Toast.LENGTH_LONG);
                toast.setGravity(Gravity.CLIP_HORIZONTAL | Gravity.CLIP_VERTICAL, 0, 0);
                toast.show();
            }
        });
    }

    //=============================================================================================
    // 基本ユーティリティ処理関数
    //=============================================================================================
    /**
     * sleep
     * @param msecs : スリープ時間
     * @return boolean : 処理結果
     */
    private static boolean sleep(long msecs)
    {
        try {
            Thread.sleep(msecs);
            return true;
        }
        catch(Exception e) {
            return false;
        }
    }

}
