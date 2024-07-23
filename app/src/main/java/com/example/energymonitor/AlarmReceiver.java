package com.example.energymonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Arrays;

public class AlarmReceiver extends BroadcastReceiver implements FetchBlackoutRounds.FetchResult {
    private static final String LOG_TAG = "EnergyMonitor_AlarmReceiver_LOG";
    private DB db;
    private NotificationHelper notificationHelper;
    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        if(db==null) db = new DB(context);
        if(notificationHelper==null) notificationHelper = new NotificationHelper(context);
        this.context=context;
        new FetchBlackoutRounds(this).start();
    }

    @Override
    public void onFetched(int result, String date, String raw, String[] rounds) {
        if(db == null) return;
        switch (result){
            case FetchBlackoutRounds.OkResult:
                if (date != null && rounds.length != 0) {
                    if(!Arrays.equals(db.get(date), rounds)) notificationHelper.sendNotification("LOL",
                            context.getString(R.string.rounds_change)+" "+date);
                    db.add(date, rounds);
                    Log.d(LOG_TAG,"Saved to db");
                } else Log.d(LOG_TAG,"Data error!!!");
                break;
            case FetchBlackoutRounds.BadResult:
                Log.d(LOG_TAG,"Information about outage schedules not found");
                break;
            case FetchBlackoutRounds.NetworkError:
                Log.d(LOG_TAG,"Network error!!!");
                break;
        }
    }
}