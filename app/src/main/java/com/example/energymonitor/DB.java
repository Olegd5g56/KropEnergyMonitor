package com.example.energymonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

class DB extends SQLiteOpenHelper {
    private static final String LOG_TAG = "EnergyMonitor_DB_LOG";
    public static final String dbName = "ignis.db";
    private static final int dbVersion = 4;
    private static final String tableName = "MAIN_TABLE";
    private final SQLiteDatabase db;
    Context context;

    public DB(Context context){
        super(context, dbName, null, dbVersion);
        this.context=context;
        db=this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Log.d(LOG_TAG, "--- onCreate database ---");
        db.execSQL("create table "+tableName+" ("
                + "date text PRIMARY KEY,"
                + "H01 INTEGER,"
                + "H02 INTEGER,"
                + "H03 INTEGER,"
                + "H04 INTEGER,"
                + "H05 INTEGER,"
                + "H06 INTEGER,"
                + "H07 INTEGER,"
                + "H08 INTEGER,"
                + "H09 INTEGER,"
                + "H10 INTEGER,"
                + "H11 INTEGER,"
                + "H12 INTEGER,"
                + "H13 INTEGER,"
                + "H14 INTEGER,"
                + "H15 INTEGER,"
                + "H16 INTEGER,"
                + "H17 INTEGER,"
                + "H18 INTEGER,"
                + "H19 INTEGER,"
                + "H20 INTEGER,"
                + "H21 INTEGER,"
                + "H22 INTEGER,"
                + "H23 INTEGER,"
                + "H24 INTEGER" + ");");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "--- onUpgrade database --- OLD: "+oldVersion+" NEW: "+newVersion);
        clearDatabase(db);
    }

    private String format(int n){
        return n > 9 ? "H"+n : "H0"+n;
    }
    public void loadJSON(JSONObject data) throws JSONException {
        clean();
        JSONArray days_arr = data.getJSONArray("data");

        for (int i = 0; i < days_arr.length(); i++) {
            JSONArray shedule_arr = days_arr.getJSONObject(i).getJSONArray("Shedule");
            for (int j = 0; j < shedule_arr.length(); j++) {
                ContentValues cv = new ContentValues();
                JSONObject e = shedule_arr.getJSONObject(j);
                cv.put("date", e.getString("DayName").split(", ")[1]);
                for (int q = 1; q <= 24; q++) cv.put(format(q), e.getString(format(q)));
                db.insert(tableName, null, cv);
            }
        }
        Log.d(LOG_TAG, "added to bd");
    }

    public BlackoutShedule get(String date){
        BlackoutShedule rez;
        Cursor c = db.rawQuery("SELECT * FROM "+tableName+" WHERE date='"+date+"'", null);
        c.moveToFirst();
        if(c.getCount() > 0) {
            int[] hours = new int[24];
            for(int j = 0; j < 24; j++){
                hours[j] = c.getInt(j+1);
            }
            rez = new BlackoutShedule(date,hours);
            c.close();
            return rez;
        }else{
            return null;
        }
    }

    public String[] getDates(){
        Log.d(LOG_TAG, "getDates()");

        Cursor c = db.rawQuery("SELECT * FROM "+tableName, null);
        String[] rez = new String[c.getCount()];

        if (c.moveToFirst()) {
            do {
                rez[c.getPosition()] = c.getString(0);
            } while (c.moveToNext());
        }else{
            Log.d(LOG_TAG, "0 rows");
        }

        c.close();
        return rez;
    }

    public void clean(){
        db.execSQL("DELETE FROM "+tableName);
    }

    public void clearDatabase(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS "+tableName);
        onCreate(db);
    }

}