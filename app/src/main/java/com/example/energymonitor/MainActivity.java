package com.example.energymonitor;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements FetchBlackoutRounds.FetchResult {
    private static final String LOG_TAG = "EnergyMonitor_Main_LOG";
    SharedPreferences sp;
    DB db;
    TextView textView;
    Spinner spinner, date_spinner;

    public static final String SP_NAME = "SP";
    public static final String SELECTED_KEY = "selected";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 42;
    public static SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE);

        db = new DB(this);
        try {
            db.clean();
        } catch (Exception e) {
            alert(getString(R.string.db_corrupt));
            db.clearDatabase();
        }

        textView = findViewById(R.id.tv1);
        spinner = findViewById(R.id.spinner);
        date_spinner = findViewById(R.id.date_spinner);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Object selected_date = date_spinner.getSelectedItem();
                if(selected_date != null) {
                    textView.setText(db.get(selected_date.toString())[position]);
                    sp.edit()
                            .putInt(SELECTED_KEY, position)
                            .apply();
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        date_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                getLayoutInflater();
                Object selected_date = date_spinner.getSelectedItem();
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_dropdown_item);
                for(int i = 1; i <= db.get(selected_date.toString()).length; i++){
                    adapter.add(i+" "+getString(R.string.round));
                }

                spinner.setAdapter(adapter);
                int latest_position = sp.getInt(SELECTED_KEY,0);
                spinner.setSelection(latest_position < adapter.getCount() ? latest_position : 0);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        new FetchBlackoutRounds(this).start();

        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        long interval = 10*60*1000;
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + interval,
                interval,
                pendingIntent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATION_PERMISSION);
            }
        }

    }

    protected void update_date_spinner(){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_spinner_dropdown_item);
        for(String e: db.getDates()){
            adapter.add(e);
        }
        date_spinner.setAdapter(adapter);

        int current_date_position = adapter.getPosition(dateFormat.format(new Date()));
        date_spinner.setSelection(current_date_position != -1 ? current_date_position : 0);

        findViewById(R.id.progressBar).setVisibility(View.INVISIBLE);
    }

    protected void alert(String msg){
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                (dialog, which) -> dialog.dismiss());
        alertDialog.show();
    }

    @Override
    public void onFetched(int result, String date, String raw, String[] rounds) {
        Log.d(LOG_TAG,"Fetched: '"+raw+"'");

        switch (result){
            case FetchBlackoutRounds.OkResult:
                if (rounds.length == 0) {
                    this.runOnUiThread(() -> alert(raw));
                } else {
                    if (date != null) {
                        db.add(date, rounds);
                    } else {
                        this.runOnUiThread(() -> alert(getString(R.string.date_error)));
                    }
                }
                break;
            case FetchBlackoutRounds.BadResult:
                this.runOnUiThread(() -> alert(getString(R.string.not_found)));
                break;
            case FetchBlackoutRounds.NetworkError:
                this.runOnUiThread(() -> alert(getString(R.string.network_error)));
                break;
        }

        this.runOnUiThread(this::update_date_spinner);

    }

}