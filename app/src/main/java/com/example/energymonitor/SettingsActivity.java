package com.example.energymonitor;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class SettingsActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {
    private static final String LOG_TAG = "EnergyMonitor_Settings_LOG";
    private Spinner electrical_net_spinner,settlement_spinner,street_spinner,house_spinner;
    private ItemList electrical_net_list,settlement_list,street_list,house_list;
    private int electrical_net_id, settlement_id, street_id, house_id = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        electrical_net_spinner = findViewById(R.id.electrical_net_spinner);
        settlement_spinner = findViewById(R.id.settlement_spinner);
        street_spinner = findViewById(R.id.street_spinner);
        house_spinner = findViewById(R.id.house_spinner);

        electrical_net_spinner.setOnItemSelectedListener(this);
        settlement_spinner.setOnItemSelectedListener(this);
        street_spinner.setOnItemSelectedListener(this);
        house_spinner.setOnItemSelectedListener(this);

        new FetchJSON("https://kiroe.com.ua/electricity-blackout?ajax=1", (result,data) -> {
            electrical_net_list = new ItemList(data, "OfficeName", "OfficeId");
            this.runOnUiThread(() ->
                setSpinner(electrical_net_spinner,electrical_net_list)
            );
        }).start();

        findViewById(R.id.save_button).setOnClickListener((view) -> {
            if(house_id != -1){
                Intent resultIntent = new Intent();
                resultIntent.putExtra(MainActivity.HOUSE_KEY, house_id);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        findViewById(R.id.cancel_button).setOnClickListener((view) -> {
                Intent resultIntent = new Intent();
                setResult(RESULT_CANCELED, resultIntent);
                finish();
        });

    }

    private void setSpinner(Spinner spinner,ItemList list) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(SettingsActivity.this, android.R.layout.simple_spinner_dropdown_item);
        for (Item e : list) adapter.add(e.name);
        spinner.setAdapter(adapter);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if(position == 0) return;
        if(parent.getId() == R.id.electrical_net_spinner) {
            electrical_net_id = electrical_net_list.get(position).id;
            new FetchJSON("https://kiroe.com.ua/electricity-blackout/city/" + electrical_net_id + "?ajax=1", (result, data) -> {
                settlement_list = new ItemList(data, "CityName", "CityId");
                this.runOnUiThread(() ->
                        setSpinner(settlement_spinner,settlement_list)
                );
            }).start();
        }else if(parent.getId() == R.id.settlement_spinner) {
            settlement_id = settlement_list.get(position).id;
            new FetchJSON("https://kiroe.com.ua/electricity-blackout/street/" + settlement_id + "?ajax=1", (result, data) -> {
                street_list = new ItemList(data, "StreetName", "StreetId");
                this.runOnUiThread(() ->
                        setSpinner(street_spinner,street_list)
                );
            }).start();
        }else if(parent.getId() == R.id.street_spinner) {
            street_id = street_list.get(position).id;
            new FetchJSON("https://kiroe.com.ua/electricity-blackout/house/"+settlement_id+"/"+street_id+"?ajax=1", (result, data) -> {
                house_list = new ItemList(data, "HouseNo", "HouseId");
                this.runOnUiThread(() ->
                        setSpinner(house_spinner,house_list)
                );
            }).start();
        }else if(parent.getId() == R.id.house_spinner) {
            house_id = house_list.get(position).id;
            Log.e(LOG_TAG,house_id+"");
        }

    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {}


    private static class Item{
        public final String name;
        public final int id;
        Item(String name, int id){
            this.name=name;
            this.id=id;
        }
    }

    private static class ItemList extends ArrayList<Item>{
        ItemList(JSONObject data,String name_key,String id_key){
            this.add(new Item("",-1));
            try {
                JSONArray arr = data.getJSONArray("data");
                for(int i = 0; i < arr.length(); i++){
                    this.add(new Item(arr.getJSONObject(i).getString(name_key),
                            arr.getJSONObject(i).getInt(id_key)) );
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
