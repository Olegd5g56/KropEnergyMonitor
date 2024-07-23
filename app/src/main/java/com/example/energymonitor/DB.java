package com.example.energymonitor;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.icu.util.Calendar;
import android.util.Log;

import java.util.Date;

class DB extends SQLiteOpenHelper {
    private static final String LOG_TAG = "EnergyMonitor_DB_LOG";
    public static final String dbName = "ignis.db";
    private static final int dbVersion = 1;
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
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "date text,"
                + "rounds text" + ");");
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "--- onUpgrade database --- OLD: "+oldVersion+" NEW: "+newVersion);
        //clearDatabase();
    }

    public void add(String date,String[] rounds){
        Log.d(LOG_TAG, "'"+date+"' added to bd");
        del(date);

        ContentValues cv = new ContentValues();
        cv.put("date", date);
        cv.put("rounds", concat(rounds));
        db.insert(tableName, null, cv);
    }
    public void del(String date){
        db.execSQL("DELETE FROM "+tableName+" WHERE date='"+date+"'");
    }

    public String[] get(String date){
        String rez;
        Cursor c = db.rawQuery("SELECT * FROM "+tableName+" WHERE date='"+date+"'", null);
        c.moveToFirst();
        if(c.getCount() > 0) {
            c.getString(0);
            rez = c.getString(2);
            c.close();
            return split(rez);
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
                rez[c.getPosition()] = c.getString(1);
            } while (c.moveToNext());
        }else{
            Log.d(LOG_TAG, "0 rows");
        }

        c.close();
        return rez;
    }

    public void clean() throws Exception {
        Calendar two_days_ago_date = Calendar.getInstance();
        two_days_ago_date.add(Calendar.DAY_OF_YEAR, -2);

        for (String e : getDates()) {
            Date date = MainActivity.dateFormat.parse(e);
            if(date == null) throw new Exception("current date = null! What the hell?");
            if (date.before(two_days_ago_date.getTime())) del(e);
        }
    }

    public void clearDatabase() {
        db.execSQL("DROP TABLE IF EXISTS "+tableName);
        onCreate(db);
    }

    private String[] split(String data){
        return data.split("#@#");
    }
    private String concat(String[] data){
        return String.join("#@#",data);
    }

}