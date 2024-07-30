package com.example.energymonitor;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class FetchJSON extends Thread{
    public interface Fetchable {
        void onFetched(int result, JSONObject data);
    }

    private static final String LOG_TAG = "EnergyMonitor_FetchJSON_LOG";
    Fetchable fetchable;
    private final Request request;
    private final static OkHttpClient client = new OkHttpClient();
    public static final short OK = 0;
    public static final short NETWORK_ERROR = 1;
    public static final short PARSE_ERROR = 1;


    FetchJSON(String URL,Fetchable fetchable){
        this.fetchable=fetchable;
        request = new Request.Builder()
                .url(URL)
                .build();
    }

    @Override
    public void run() {
        try {
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && response.body() != null) {
                fetchable.onFetched(OK, new JSONObject(response.body().string()));
            } else {
                fetchable.onFetched(NETWORK_ERROR, null);
            }
            response.close();
        } catch (IOException e) {
            String msg = e.getLocalizedMessage();
            Log.e(LOG_TAG, msg != null ? msg : "Unknown Error!");
            fetchable.onFetched(NETWORK_ERROR,null);
        } catch (JSONException e) {
            fetchable.onFetched(PARSE_ERROR,null);
        }
    }
}
