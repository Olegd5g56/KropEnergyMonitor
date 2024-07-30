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

import org.json.JSONException;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = "EnergyMonitor_Main_LOG";
    SharedPreferences sp;
    DB db;
    TextView textView;
    Spinner date_spinner;

    public static final String SP_NAME = "SP";
    public static final String HOUSE_KEY = "HOUSE_KEY";
    private static final int REQUEST_NOTIFICATION_PERMISSION = 42;
    private static final int REQUEST_CODE = 42;

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


        textView = findViewById(R.id.tv1);
        date_spinner = findViewById(R.id.date_spinner);


        date_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected_date = date_spinner.getSelectedItem().toString();
                BlackoutShedule shedule = db.get(selected_date);
                if (shedule != null){
                    String shedule_str = shedule.toString();
                    if(shedule_str.equals(BlackoutShedule.noShedule)) {
                        textView.setTextSize(24);
                        textView.setText(getString(R.string.no_blackout));
                    }else{
                        textView.setTextSize(60);
                        textView.setText(getString(R.string.no_blackout));
                    }
                }

            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        findViewById(R.id.settings_button).setOnClickListener((view) -> startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class),REQUEST_CODE));

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

        if(sp.getInt(HOUSE_KEY,-1) == -1) startActivityForResult(new Intent(MainActivity.this, SettingsActivity.class),REQUEST_CODE);
        FetchShedule();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                int house_id = data.getIntExtra(HOUSE_KEY,-1);
                if(house_id != -1) {
                    Log.d(LOG_TAG,"onActivityResult: "+house_id);
                    sp.edit()
                            .putInt(HOUSE_KEY, house_id)
                            .apply();
                    FetchShedule();
                }
            }else if (sp.getInt(HOUSE_KEY,-1) == -1){
                textView.setText(R.string.address_alert);
            }
        }
    }

    protected void FetchShedule(){
        int house_id = sp.getInt(HOUSE_KEY,-1);
        if(house_id != -1) {
            new FetchJSON("https://kiroe.com.ua/electricity-blackout/websearch/"+house_id+"?ajax=1", (result, data) -> {
                Log.d(LOG_TAG,"OnFetch code: "+result);
                if(result == FetchJSON.OK){
                    try {
                        db.loadJSON(data);
                    } catch (JSONException e) {
                        this.runOnUiThread(() -> alert(getString(R.string.json_error)));
                        Log.d(LOG_TAG,"DB could not recognize JSON!");
                    }
                }
                else this.runOnUiThread(() -> alert(getString(R.string.network_error)));
                this.runOnUiThread(this::update_date_spinner);
            }).start();
        }
    }

    protected void update_date_spinner(){
        Log.d(LOG_TAG,"update_date_spinner");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this,android.R.layout.simple_spinner_dropdown_item);
        for(String e: db.getDates()){
            adapter.add(e);
        }
        date_spinner.setAdapter(adapter);

        int current_date_position = adapter.getPosition( new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date()));
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

}