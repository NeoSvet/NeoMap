package ru.neosvet.neomap.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by NeoSvet on 22.01.2017.
 */

public class DataBase extends SQLiteOpenHelper {
    public static final String TABLE = "MARKER",
            NAME = "name", LAT = "lat", LNG = "lng",
            DESCRIPTION = "des";

    public DataBase(Context context) {
        super(context, TABLE, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE + " ("
                + NAME + " text primary key,"
                + DESCRIPTION + " text,"
                + LAT + " real,"
                + LNG + " real);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}
