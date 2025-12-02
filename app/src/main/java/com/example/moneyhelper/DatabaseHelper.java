package com.example.moneyhelper;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "money_helper.db";
    private static final int DATABASE_VERSION = 2;
    private final Context context;
    private SQLiteDatabase database;
    private String databasePath;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        this.databasePath = context.getDatabasePath(DATABASE_NAME).getPath();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Не создаем таблицы, так как используем готовую БД
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Логика обновления БД при необходимости
    }

    // Создание базы данных
    public void createDatabase() throws IOException {
        boolean dbExist = checkDatabase();

        if (!dbExist) {
            this.getReadableDatabase();
            copyDatabase();
        }
    }

    // Проверка существования БД
    private boolean checkDatabase() {
        SQLiteDatabase checkDB = null;
        try {
            checkDB = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READONLY);
        } catch (Exception e) {
            // База данных не существует
        }

        if (checkDB != null) {
            checkDB.close();
        }

        return checkDB != null;
    }

    // Копирование БД из assets
    private void copyDatabase() throws IOException {
        // Открываем локальную БД как поток ввода
        InputStream myInput = context.getAssets().open("databases/" + DATABASE_NAME);

        // Путь к созданной БД
        String outFileName = databasePath;

        // Открываем пустую БД как поток вывода
        OutputStream myOutput = new FileOutputStream(outFileName);

        // Копируем байт за байтом
        byte[] buffer = new byte[1024];
        int length;
        while ((length = myInput.read(buffer)) > 0) {
            myOutput.write(buffer, 0, length);
        }

        // Закрываем потоки
        myOutput.flush();
        myOutput.close();
        myInput.close();
    }

    // Открытие БД
    public void openDatabase() throws SQLException {
        database = SQLiteDatabase.openDatabase(databasePath, null, SQLiteDatabase.OPEN_READWRITE);
    }

    @Override
    public synchronized void close() {
        if (database != null) {
            database.close();
        }
        super.close();
    }

    // Выполнение запроса SELECT
    public Cursor query(String query, String[] selectionArgs) {
        return database.rawQuery(query, selectionArgs);
    }

    // Выполнение других SQL запросов
    public void execSQL(String sql) throws SQLException {
        database.execSQL(sql);
    }
}