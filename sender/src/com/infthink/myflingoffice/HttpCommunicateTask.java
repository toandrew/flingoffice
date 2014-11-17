package com.infthink.myflingoffice;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class HttpCommunicateTask extends AsyncTask<String, Integer, Integer> {
    private static final String TAG = "HttpCommunicateTask";

    private String httpUrl;

    private List<NameValuePair> nameValues;

    private Handler handler;

    private Context context;

    private HttpClient httpclient;

    public HttpCommunicateTask(Context context, String httpUrl,
            List<NameValuePair> nameValues, Handler handler) {
        this.httpUrl = httpUrl;
        this.nameValues = nameValues;
        this.handler = handler;
        this.context = context;
    }

    public void showdown() {
        if (httpclient != null) {
            try {
                //httpclient.getConnectionManager().shutdown();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected Integer doInBackground(String... params) {
        // 取得默认的HttpClient
        httpclient = new DefaultHttpClient();
        try {
            // 设置字符集
            HttpEntity httpentity = null;
            httpentity = new UrlEncodedFormEntity(nameValues, Utils.HTTP_ENCODE);
            // HttpPost连接对象
            HttpPost httpRequest = new HttpPost(httpUrl);
            // 请求httpRequest
            httpRequest.setEntity(httpentity);

            LogUtils.d(TAG,
                    "Send HTTP request!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");

            // 取得HttpResponse
            HttpResponse httpResponse = httpclient.execute(httpRequest);
            // HttpStatus.SC_OK表示连接成功
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                // 取得返回的字符串
                handleResult(EntityUtils.toString(httpResponse.getEntity()));
            } else {
                sendMessage(Utils.HANDLE_CONNECTION_FAIL, null, handler);
            }
        } catch (ClientProtocolException e) {
            Log.e(TAG, "", e);
            sendMessage(Utils.HANDLE_CONNECTION_FAIL, null, handler);
        } catch (IOException e) {
            Log.e(TAG, "", e);
            sendMessage(Utils.HANDLE_CONNECTION_FAIL, null, handler);
        } catch (Exception e) {
            Log.e(TAG, "", e);
            sendMessage(Utils.HANDLE_CONNECTION_FAIL, null, handler);
        }

        LogUtils.d(TAG, "quit task!");

        return null;
    }

    public void handleResult(String jsonStr) {
        try {
            final int result = new JSONObject(jsonStr)
                    .getInt(Utils.RESPONSE_JSON_RESULT_KEY);
            switch (result) {
            case Utils.RESULT_SUCCESS:
                sendMessage(Utils.HANDLE_COMMUNICATION_RESPONSE_BODY_ENTITY,
                        jsonStr, handler);
                break;
            default:
                handler.post(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(context,
                                String.format("result: %d", result),
                                Toast.LENGTH_LONG).show();
                    }
                });
                sendMessage(Utils.HANDLE_COMMUNICATION_RESPONSE_ERROR, result,
                        handler);
                break;
            }
        } catch (JSONException e) {
            Log.e(TAG, "", e);
            sendMessage(Utils.HANDLE_COMMUNICATION_RESPONSE_ERROR, null,
                    handler);
        }
    }

    private void sendMessage(int what, Object object, Handler handler) {
        if (handler == null) {
            return;
        }
        LogUtils.d(TAG, "received !!!!!!!!what = " + what);
        if (object != null) {
            LogUtils.d(TAG, " obj = " + object.toString());
        }
        handler.removeMessages(what);

        Message msg = handler.obtainMessage();
        msg.what = what;
        msg.obj = object;
        handler.sendMessage(msg);
    }
}
