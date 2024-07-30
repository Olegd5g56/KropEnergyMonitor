package com.example.energymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;


public class AlarmReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "EnergyMonitor_AlarmReceiver_LOG";
    private DB db;
    private NotificationHelper notificationHelper;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(db==null) db = new DB(context);
        if(notificationHelper==null) notificationHelper = new NotificationHelper(context);
        SharedPreferences sp = context.getSharedPreferences(MainActivity.SP_NAME, Context.MODE_PRIVATE);

        int house_id = sp.getInt(MainActivity.HOUSE_KEY,-1);
        if(house_id != -1) {
            new FetchJSON("https://kiroe.com.ua/electricity-blackout/websearch/"+house_id+"?ajax=1", (result, data) -> {
                Log.d(LOG_TAG,"OnFetch code: "+result);
                if(result == FetchJSON.OK){
                    try {
                        String current_date = new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date());
                        BlackoutShedule old_shedule = db.get(current_date);
                        db.loadJSON(data);
                        BlackoutShedule new_shedule = db.get(current_date);
                        if(!Arrays.equals(old_shedule.hours, new_shedule.hours)){
                            new NotificationHelper(context).sendNotification(context.getString(R.string.attention),
                                    context.getString(R.string.change_shedule));
                        }
                        Log.d(LOG_TAG,"DB updated!");
                    } catch (JSONException e) {
                        Log.d(LOG_TAG,"DB could not recognize JSON!");
                    }
                }
                else Log.d(LOG_TAG,"Network Error");
            }).start();
        }

    }

}