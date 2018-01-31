package jp.co.casio.exilim_sample_application;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by Daisuke Ohtani on 2017/05/24.
 * 2018/01/30 POSTをクエリ入力に修正
 */

public class PluginControl {

    // ログ用TAG
    private static final String TAG = "ExilimPluginCtrl";
    // EXILIMプラグイン名
    private static final String EXILIM_PLUGIN_NAME = "Exilim";
    // プラグインAPI ID保持
    private String mApiId = null;
    // URL保持用 変数
    private String sStr_UrlText = null;

    // EXILIMプラグインセットアップAPI
    public boolean sendExilimPluginSetUp(String vstr_url){
        String url, str;
        JSONObject service;
        JSONArray services;
        boolean vb_Ret = false;

        if (vstr_url == null) {
            return vb_Ret;
        }
        // URL文字列のコピー
        sStr_UrlText = vstr_url;

        url = "http://"+ sStr_UrlText +":4035/gotapi/servicediscovery";
        str = sendHttpWithResponse(url, "GET");

        if(str != null) {
            try {
                //JSON解析ser
                JSONObject root = new JSONObject(str);
                if (root != null) {
                    services = root.getJSONArray("services");
                    if (services != null) {
                        for (int ii = 0; ii < services.length(); ii++) {
                            service = services.getJSONObject(ii);
                            // Exilim Pluginの検索
                            if (service.getString("name").equals(EXILIM_PLUGIN_NAME)) {
                                mApiId = service.getString("id");
                                Log.i(TAG, "SET:" + EXILIM_PLUGIN_NAME);
                                break;
                            }
                        }
                    }
                }
                vb_Ret = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return vb_Ret;
    }

    // EXILIM LIVE VIEW 開始API
    public String sendExilimLiveViewStart(){
        if( mApiId != null && sStr_UrlText != null){
            String url,str;

            Log.i(TAG, "onClickConnect");
            url = "http://" + sStr_UrlText + ":4035/gotapi/mediaStreamRecording/preview?serviceId=" + mApiId;
            str = sendHttpWithResponse(url, "PUT");

            return str;
        }
        else {
            return null;
        }
    }

    // EXILIM LIVE VIEW 停止API
    public void sendExilimLiveViewEnd(){

        if( mApiId != null && sStr_UrlText != null){
            String url,str;

            Log.i(TAG, "onClickConnect");
            url = "http://" + sStr_UrlText + ":4035/gotapi/mediaStreamRecording/preview?serviceId=" + mApiId;
            str = sendHttpWithResponse(url, "DELETE");

            if( str == null ) {
                Log.e(TAG, "sendLiveViewEnd Http_ERR");
            }
        }
    }

    // EXILIM 写真撮影API
    public String sendExilimTakePhoto(){
        if( mApiId != null && sStr_UrlText != null){
            String url,str, path;

            // 2017/10/24　クエリ文字列入力に修正
            url = "http://" + sStr_UrlText + ":4035/gotapi/mediaStreamRecording/takephoto?serviceId=" + mApiId;
            str = sendHttpWithResponse(url, "POST");

            if (str != null) {
                try {
                    //JSON解析ser
                    JSONObject root = new JSONObject(str);
                    if (root != null) {
                        path = root.getString("path");
                    }
                    else {
                        path = null;
                    }
                } catch (Exception e) {
                    path = null;
                    e.printStackTrace();
                }
            }
            else {
                path = null;
                Log.e(TAG, "sendLiveViewEnd Http_ERR");
            }

            return path;
        }
        else {
            return null;
        }
    }

    // EXILIM 動画撮影開始API
    public String sendExilimMovieStart(){
        if( mApiId != null && sStr_UrlText != null){
            String url, str, path;

            Log.v(TAG, "sendMovieStart");

            // 2017/10/24　クエリ文字列入力に修正
            url = "http://" + sStr_UrlText + ":4035/gotapi/mediaStreamRecording/Record?serviceId=" + mApiId;
            str = sendHttpWithResponse(url, "POST");

            if (str != null) {
                try {
                    //JSON解析ser
                    JSONObject root = new JSONObject(str);
                    if (root != null) {
                        path = root.getString("path");
                    }
                    else {
                        path = null;
                    }
                } catch (Exception e) {
                    path = null;
                    e.printStackTrace();
                }
            }
            else {
                path = null;
                Log.e(TAG, "sendLiveViewEnd Http_ERR");
            }

            Log.v(TAG, "sendMovieStart_END");

            return path;
        }
        else {
            return null;
        }
    }

    // EXILIM 動画撮影状態通知 登録API
    public void sendExilimMovieStateChangeEventRegister() {
        if( mApiId != null && sStr_UrlText != null){
            String url,str;

            Log.v(TAG, "sendMovieStateChangeEventRegister");

            url = "http://" + sStr_UrlText + ":4035/gotapi/mediaStreamRecording/onRecordingChange?serviceId=" + mApiId;

            str = sendHttpWithResponse(url, "PUT");

            if( str == null ) {
                Log.e(TAG, "sendMovieStart Http_ERR");
            }

            Log.v(TAG, "sendMovieStateChangeEventRegister_END");
        }
        return;
    }

    // EXILIM 動画撮影状態通知 解除API
    public void sendExilimMovieStateChangeEventUnregister() {
        if( mApiId != null && sStr_UrlText != null){
            String url,str;

            Log.v(TAG, "sendMovieStateChangeEventUnregister");

            url = "http://" + sStr_UrlText + ":4035/gotapi/mediaStreamRecording/onRecordingChange?serviceId=" + mApiId;

            str = sendHttpWithResponse(url, "DELETE");

            if( str == null ) {
                Log.e(TAG, "sendMovieStart Http_ERR");
            }

            Log.v(TAG, "sendMovieStateChangeEventUnregister_END");
        }
        return;
    }

    // EXILIM 動画撮影状態通知 URL取得API
    public String getExilimMovieStateChangeEventURL() {
        return "http://" + sStr_UrlText + ":4035/gotapi/mediaStreamRecording/onRecordingChange?serviceId=" + mApiId;
    }

    // EXILIM 動画撮影停止API
    public void sendExilimMovieStop(){
        if( mApiId != null && sStr_UrlText != null){
            String url,str;

            url = "http://" + sStr_UrlText + ":4035/gotapi/mediaStreamRecording/Stop?serviceId=" + mApiId;
            str = sendHttpWithResponse(url, "PUT");

            if( str == null ) {
                Log.e(TAG, "sendMovieStop Http_ERR");
            }
        }
    }

    // EXILIM サムネイル画像取得API
    public String getExilimThumbnailImage(String FilePath){
        if( mApiId != null && sStr_UrlText != null){
            String url,str, uri;
            url = "http://" + sStr_UrlText + ":4035/gotapi/thumbnail?serviceId=" + mApiId + "&path=" + FilePath;

            str = sendHttpWithResponse(url, "GET");

            if (str != null) {
                try {
                    //JSON解析ser
                    JSONObject root = new JSONObject(str);
                    if (root != null) {
                        uri = root.getString("uri");
                    }
                    else {
                        uri = null;
                    }
                } catch (Exception e) {
                    uri = null;
                    e.printStackTrace();
                }
            }
            else {
                uri = null;
                Log.e(TAG, "sendLiveViewEnd Http_ERR");
            }

            return uri;
        }
        else {
            return null;
        }
    }

    // EXILIM 撮影画像取得API
    public String getExilimCaptureFile(String FilePath){
        if( mApiId != null && sStr_UrlText != null){
            String url,str, uri;
            url = "http://" + sStr_UrlText + ":4035/gotapi/file?serviceId=" + mApiId + "&path=" + FilePath;

            str = sendHttpWithResponse(url, "GET");

            if (str != null) {
                try {
                    //JSON解析ser
                    JSONObject root = new JSONObject(str);
                    if (root != null) {
                        uri = root.getString("uri");
                    }
                    else {
                        uri = null;
                    }
                } catch (Exception e) {
                    uri = null;
                    e.printStackTrace();
                }
            }
            else {
                uri = null;
                Log.e(TAG, "sendLiveViewEnd Http_ERR");
            }

            return uri;
        }
        else {
            return null;
        }
    }

    // EXILIM ズームインAPI
    public void sendExilimZoomIn(){
        if( mApiId != null && sStr_UrlText != null){
            String url,str;

            url = "http://" + sStr_UrlText +":4035/gotapi/camera/zoom?serviceId="+mApiId+"&direction=in&movement=max";
            str = sendHttpWithResponse(url, "PUT");

            if( str == null ) {
                Log.e(TAG, "sendLiveViewEnd Http_ERR");
            }
        }
    }

    // EXILIM ズームアウトAPI
    public void sendExilimZoomOut(){
        if( mApiId != null && sStr_UrlText != null){
            String url,str;

            url = "http://" + sStr_UrlText +":4035/gotapi/camera/zoom?serviceId="+mApiId+"&direction=out&movement=max";
            str = sendHttpWithResponse(url, "PUT");

            if( str == null ) {
                Log.e(TAG, "sendLiveViewEnd Http_ERR");
            }
        }
    }

    // HTTPコマンド送信 (応答付き)
    private String sendHttpWithResponse( String path, String method) {
        byte[] buf;

        buf = sendHttp(path, method);

        if (buf != null){
            return new String(buf);
        }
        else {
            return null;
        }
    }

    // HTTPコマンド送信基本API
    private byte[] sendHttp( String path, String method ) {
        byte[] w = new byte[1024];
        HttpURLConnection c = null;
        InputStream in = null;
        ByteArrayOutputStream out = null;
        String str = null;
        int resp_code;

        try{
            URL url = new URL(path);

            c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod(method);
            c.connect();

            resp_code = c.getResponseCode();

            if( resp_code == 200 ) {
                in = c.getInputStream();
                out = new ByteArrayOutputStream();
                while (true) {
                    int size = in.read(w);
                    if (size <= 0) break;
                    out.write(w, 0, size);
                }
                out.close();
                in.close();

                if( out.toByteArray()[0] == '{' ) {
                    str = new String(out.toByteArray());
                }
            }
            c.disconnect();

            return out.toByteArray();
        }catch( Exception e ) {
            try{
                if( c!= null )  c.disconnect();
                if( in!=null ) in.close();
                if( out!=null) out.close();
                e.printStackTrace();
            }catch(Exception e2){

            }
            e.printStackTrace();
            return null;
        }
    }

}
