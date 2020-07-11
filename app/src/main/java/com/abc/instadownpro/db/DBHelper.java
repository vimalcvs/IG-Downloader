package com.abc.instadownpro.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.abc.instadownpro.base.MainApplication;

public class DBHelper {


    private static final String DATABASE_NAME = "downloader.db";// 数据库名
    SQLiteDatabase db;
    Context context;

    private static volatile DBHelper sDefault;


    public static DBHelper getDefault() {
        if (sDefault == null) {
            sDefault = new DBHelper(MainApplication.getInstance().getApplicationContext());
        }

        return sDefault;
    }

    public DBHelper(Context _context) {
        context = _context;
        db = context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null);
        createTable();
    }

    public void createTable() {
        try {
            db.execSQL("CREATE TABLE IF NOT EXISTS downloading_table (" +
                    "_ID INTEGER PRIMARY KEY autoincrement,"
                    + "video_title TEXT, page_url varchar(512),thumbnail_url varchar(512),video_url varchar(512),app_page_url varchar(512),video_path varchar(512),video_status int default 0"
                    + ");");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
