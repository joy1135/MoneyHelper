package com.example.moneyhelper;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Singleton DatabaseHelper для предотвращения блокировок БД
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static final String DB_NAME = "money_helper.db";
    private static final int DB_VERSION = 6;

    private static DatabaseHelper instance;
    private static final Object instanceLock = new Object();

    private final Context context;
    private final String dbPath;
    private SQLiteDatabase database;

    // Приватный конструктор для Singleton
    private DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
        this.context = context.getApplicationContext();
        this.dbPath = context.getDatabasePath(DB_NAME).getPath();

        // Проверяем и копируем БД при создании
        checkAndCopyDatabase();
    }

    /**
     * Получить единственный экземпляр DatabaseHelper
     * ВСЕГДА используйте этот метод вместо new DatabaseHelper()
     */
    public static DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            synchronized (instanceLock) {
                if (instance == null) {
                    instance = new DatabaseHelper(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Не создаем таблицы, так как используем готовую БД
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // При обновлении версии перезаписываем БД
        try {
            copyDatabase();
        } catch (IOException e) {
            Log.e(TAG, "Error upgrading database", e);
            throw new RuntimeException("Error copying database", e);
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        // Включаем WAL режим для лучшей конкурентности
        if (!db.isReadOnly()) {
            db.enableWriteAheadLogging();
        }
        // Устанавливаем busy timeout
        db.setMaxSqlCacheSize(100);
    }

    /**
     * Проверяем и копируем БД если нужно
     */
    private void checkAndCopyDatabase() {
        if (!checkDatabaseExists()) {
            try {
                Log.d(TAG, "Database not found, copying from assets...");
                copyDatabase();
                Log.d(TAG, "Database copied successfully");
            } catch (IOException e) {
                Log.e(TAG, "Error copying database", e);
                throw new RuntimeException("Error copying database", e);
            }
        }
    }

    /**
     * Проверяем существует ли БД
     */
    private boolean checkDatabaseExists() {
        File dbFile = new File(dbPath);
        return dbFile.exists();
    }

    /**
     * Копируем БД из assets
     */
    private void copyDatabase() throws IOException {
        // Убедимся, что папка databases существует
        File dbFile = new File(dbPath);
        File dbDir = dbFile.getParentFile();
        if (dbDir != null && !dbDir.exists()) {
            dbDir.mkdirs();
        }

        // Получаем поток из assets
        InputStream inputStream = context.getAssets().open(DB_NAME);

        // Создаем поток для записи
        OutputStream outputStream = new FileOutputStream(dbPath);

        // Копируем файл
        byte[] buffer = new byte[8192]; // Увеличенный буфер для скорости
        int length;
        while ((length = inputStream.read(buffer)) > 0) {
            outputStream.write(buffer, 0, length);
        }

        // Закрываем потоки
        outputStream.flush();
        outputStream.close();
        inputStream.close();
    }

    /**
     * Получить БД для записи
     * Использует WAL режим для предотвращения блокировок
     */
    @Override
    public synchronized SQLiteDatabase getWritableDatabase() {
        if (database != null && database.isOpen()) {
            return database;
        }

        // Убедимся, что БД скопирована
        if (!checkDatabaseExists()) {
            try {
                copyDatabase();
            } catch (IOException e) {
                Log.e(TAG, "Error copying database", e);
                throw new RuntimeException("Error copying database", e);
            }
        }

        try {
            // Открываем БД с флагами для предотвращения блокировок
            database = SQLiteDatabase.openDatabase(
                    dbPath,
                    null,
                    SQLiteDatabase.OPEN_READWRITE |
                            SQLiteDatabase.CREATE_IF_NECESSARY |
                            SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING
            );

            // Настраиваем параметры для предотвращения блокировок
//            database.execSQL("PRAGMA busy_timeout = 3000"); // 3 секунды ожидания
//            database.execSQL("PRAGMA journal_mode = WAL"); // Write-Ahead Logging
//            database.execSQL("PRAGMA synchronous = NORMAL"); // Баланс скорости и надежности
            database.setMaxSqlCacheSize(100);

            Log.d(TAG, "Database opened in WAL mode");

        } catch (SQLException e) {
            Log.e(TAG, "Error opening database", e);
            throw e;
        }

        return database;
    }

    /**
     * Получить БД для чтения
     * В WAL режиме чтение не блокирует запись
     */
    @Override
    public synchronized SQLiteDatabase getReadableDatabase() {
        // В WAL режиме используем ту же БД для чтения и записи
        return getWritableDatabase();
    }

    /**
     * Закрываем БД
     * ВНИМАНИЕ: В Singleton обычно не нужно закрывать БД
     */
    @Override
    public synchronized void close() {
        if (database != null && database.isOpen()) {
            database.close();
            database = null;
            Log.d(TAG, "Database closed");
        }
        super.close();
    }

    /**
     * Проверка состояния БД
     */
    public boolean isDatabaseOpen() {
        return database != null && database.isOpen();
    }

    /**
     * Выполнить запрос с повторными попытками при блокировке
     */
    public synchronized void executeWithRetry(DatabaseOperation operation, int maxRetries) {
        int retries = 0;
        SQLException lastException = null;

        while (retries < maxRetries) {
            try {
                SQLiteDatabase db = getWritableDatabase();
                operation.execute(db);
                return; // Успешно выполнено
            } catch (SQLException e) {
                lastException = e;
                if (e.getMessage() != null && e.getMessage().contains("database is locked")) {
                    retries++;
                    Log.w(TAG, "Database locked, retry " + retries + "/" + maxRetries);
                    try {
                        Thread.sleep(100); // Ждем 100мс перед повтором
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                } else {
                    throw e; // Если это не блокировка, пробрасываем исключение
                }
            }
        }

        // Если все попытки исчерпаны
        if (lastException != null) {
            Log.e(TAG, "Failed after " + maxRetries + " retries", lastException);
            throw lastException;
        }
    }

    /**
     * Интерфейс для операций с БД
     */
    public interface DatabaseOperation {
        void execute(SQLiteDatabase db) throws SQLException;
    }
}