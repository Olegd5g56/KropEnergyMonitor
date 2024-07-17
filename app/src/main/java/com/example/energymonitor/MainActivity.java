package com.example.energymonitor;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements FetchBlackoutRounds.FetchResult {
    SharedPreferences bd;
    TextView textView;
    Spinner spinner;
    ArrayList<String> rounds = new ArrayList<String>();

    public static final String BD = "bd";
    public static final String SELECTED_KEY = "selected";
    public static final String ROUNDS_KEY = "rounds";

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

        bd = getSharedPreferences(BD, Context.MODE_PRIVATE);
        textView = (TextView) findViewById(R.id.tv1);
        spinner = findViewById(R.id.spinner);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                textView.setText(rounds.get(position).replace(",","\n"));
                bd.edit()
                        .putInt(SELECTED_KEY,position)
                        .apply();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        new FetchBlackoutRounds(this,new Handler(Looper.getMainLooper())).start();

    }

    protected void alert(String msg){
        AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage(msg);
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
        alertDialog.show();
    }

    @Override
    public void onFetched(String result) {
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item);

        if(result != null) {
            int i = 1;
            for (String e : result.split("<br>")) {
                if (e.contains("черга:")) {
                    adapter.add(i + " черга");
                    rounds.add(e.split("черга:")[1].replace(" ", ""));
                    i++;
                }
            }
        }else{
            alert("Network error!!!");
        }

        if(rounds.isEmpty()){
            String data = bd.getString(ROUNDS_KEY,"none");
            if(result != null) alert(result.replace("<br>",""));
            if(!data.equals("none")){
                int i = 0;
                for(String e : data.split("#")){
                    adapter.add(i + " черга");
                    rounds.add(e);
                    i++;
                }
            }
        }else{
            bd.edit()
                    .putString(ROUNDS_KEY,String.join("#",rounds))
                    .apply();
        }

        if(!rounds.isEmpty()){
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);
            int last_select = bd.getInt(SELECTED_KEY, -1);
            if (last_select >= 0) spinner.setSelection(last_select);
        }

    }


}