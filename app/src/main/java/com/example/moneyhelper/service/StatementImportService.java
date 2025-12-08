package com.example.moneyhelper.service;
import static android.webkit.ConsoleMessage.MessageLevel.LOG;

import com.example.moneyhelper.DatabaseHelper;
import com.example.moneyhelper.parser.SberbankStatementParser;




import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class StatementImportService {
    private static final String TAG = "StatementImportService";

    private final Context context;
    private final DatabaseHelper dbHelper;
    private final SberbankStatementParser parser;
    private final PredictionService predictionService;

    public StatementImportService(Context context) {
        this.context = context;
        this.dbHelper =  DatabaseHelper.getInstance(context);
        this.parser = new SberbankStatementParser(context);
        this.predictionService = new PredictionService(context);
    }

    /**
     * Импортирует выписку из PDF
     */
    public ImportResult importStatement(Uri pdfUri) {
        ImportResult result = new ImportResult();
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            // 1. Парсим PDF
            List<SberbankStatementParser.Transaction> transactions =
                    parser.parseStatement(pdfUri);

            result.totalTransactions = transactions.size();

            // 2. Получаем или создаем категории пользователя
            Map<String, Long> categoryMap = getCategoryMap(db);

            // 3. Импортируем транзакции
            for (SberbankStatementParser.Transaction transaction : transactions) {
                // Пропускаем доходы
                if (transaction.isIncome) {
                    result.skippedTransactions++;
                    continue;
                }

                // Проверяем, не импортирована ли уже эта транзакция
                if (isDuplicate(db, transaction)) {
                    result.duplicateTransactions++;
                    continue;
                }

                Long categoryId = categoryMap.get(transaction.category);
                if (categoryId == null) {
                    // Создаем новую категорию
                    Log.d(TAG,"Create category: " + transaction.category + "Tx: " + transaction.toString());
                    categoryId = createCategory(db, transaction.category);
                    categoryMap.put(transaction.category, categoryId);
                }

                // Получаем user_cat_id
                long userCatId = getUserCategoryId(db, categoryId);

                // Добавляем расход
                long expenseId = insertExpense(db, userCatId, transaction);

                if (expenseId > 0) {
                    result.importedTransactions++;
                }
            }

            db.setTransactionSuccessful();

            // 4. Проверяем, нужно ли создавать прогнозы
//            if (shouldCreatePredictions(db)) {
//                result.predictionsCreated = predictionService.createMonthlyPredictions();
//            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка импорта выписки", e);
            result.error = e.getMessage();
        } finally {
            db.endTransaction();
        }

        return result;
    }

    /**
     * Получает маппинг категорий
     */
    private Map<String, Long> getCategoryMap(SQLiteDatabase db) {
        Map<String, Long> map = new HashMap<>();

        Cursor cursor = db.query(
                "categories",
                new String[]{"id", "name"},
                null, null, null, null, null
        );

        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            map.put(name, id);
        }
        cursor.close();

        return map;
    }

    /**
     * Создает новую категорию
     */
    private long createCategory(SQLiteDatabase db, String categoryName) {
        ContentValues values = new ContentValues();
        values.put("name", categoryName);
        Log.d(TAG, "Create category: " + categoryName);
        values.put("icon", getDefaultIcon(categoryName));

        long categoryId = db.insert("categories", null, values);

        // Создаем запись в user_categories для текущего пользователя
        long userId = getCurrentUserId(db);

        ContentValues userCatValues = new ContentValues();
        userCatValues.put("user_id", userId);
        userCatValues.put("cat_id", categoryId);
        userCatValues.put("name", categoryName);
        userCatValues.put("fixed", 0);

        db.insert("user_categories", null, userCatValues);

        return categoryId;
    }

    /**
     * Получает user_category_id
     */
    private long getUserCategoryId(SQLiteDatabase db, long categoryId) {
        long userId = getCurrentUserId(db);

        Cursor cursor = db.query(
                "user_categories",
                new String[]{"id"},
                "user_id = ? AND cat_id = ?",
                new String[]{String.valueOf(userId), String.valueOf(categoryId)},
                null, null, null
        );

        long userCatId = -1;
        if (cursor.moveToFirst()) {
            userCatId = cursor.getLong(0);
        }
        cursor.close();

        return userCatId;
    }

    /**
     * Добавляет расход в БД
     */
    private long insertExpense(SQLiteDatabase db, long userCatId,
                               SberbankStatementParser.Transaction transaction) {

        // Получаем date_id для месяца транзакции
        long dateId = getOrCreateDateId(db, transaction.date);

        ContentValues values = new ContentValues();
        values.put("user_cat_id", userCatId);
        values.put("expenses", transaction.amount);
        values.put("date_id", dateId);

        return db.insert("monthly_expenses", null, values);
    }

    /**
     * Проверяет, является ли транзакция дубликатом
     */
    private boolean isDuplicate(SQLiteDatabase db,
                                SberbankStatementParser.Transaction transaction) {
//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String dateStr = dateFormat.format(transaction.date);

        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM monthly_expenses me " +
                        "JOIN dates d ON me.date_id = d.id " +
                        "WHERE d.date = ? AND me.expenses = ?",
                new String[]{dateStr, String.valueOf(transaction.amount)}
        );

        boolean isDuplicate = false;
        if (cursor.moveToFirst()) {
            isDuplicate = cursor.getInt(0) > 0;
        }
        cursor.close();

        return isDuplicate;
    }

    /**
     * Получает или создает date_id для месяца
     */
    private long getOrCreateDateId(SQLiteDatabase db, Date transactionDate) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(transactionDate);

        // Устанавливаем 1 число месяца
        cal.set(Calendar.DAY_OF_MONTH, 1);

//        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        String dateStr = dateFormat.format(cal.getTime());

        // Ищем существующую запись
        Cursor cursor = db.query(
                "dates",
                new String[]{"id"},
                "date = ?",
                new String[]{dateStr},
                null, null, null
        );

        long dateId;
        if (cursor.moveToFirst()) {
            dateId = cursor.getLong(0);
        } else {
            // Создаем новую запись
            ContentValues values = new ContentValues();
            values.put("date", dateStr);
            dateId = db.insert("dates", null, values);
        }
        cursor.close();

        return dateId;
    }

    /**
     * Проверяет, нужно ли создавать прогнозы
     * Прогнозы создаются если:
     * 1. Есть данные за более чем 1 месяц
     * 2. Сегодня 1 число месяца ИЛИ идет импорт выписки
     */
    private boolean shouldCreatePredictions(SQLiteDatabase db) {
        // Проверяем количество месяцев с данными
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(DISTINCT date_id) FROM monthly_expenses",
                null
        );

        int monthCount = 0;
        if (cursor.moveToFirst()) {
            monthCount = cursor.getInt(0);
        }
        cursor.close();

        return monthCount > 1;
    }

    /**
     * Получает ID текущего пользователя
     */
    private long getCurrentUserId(SQLiteDatabase db) {
        // В вашей реализации может быть SharedPreferences или другой механизм
        // Пока возвращаем первого пользователя
        Cursor cursor = db.query("users", new String[]{"id"}, null, null, null, null, null, "1");
        long userId = 1;
        if (cursor.moveToFirst()) {
            userId = cursor.getLong(0);
        }
        cursor.close();
        return userId;
    }

    /**
     * Возвращает иконку по умолчанию для категории
     */
    private String getDefaultIcon(String categoryName) {
        if (categoryName == null){
            return "error";
        }
        switch (categoryName) {
            case "Продукты": return "🛒";
            case "Транспорт": return "🚗";
            case "Кафе и рестораны": return "🍽️";
            case "Переводы": return "💸";
            default: return "📦";
        }
    }



    /**
     * Результат импорта
     */
    public static class ImportResult {
        public int totalTransactions;
        public int importedTransactions;
        public int duplicateTransactions;
        public int skippedTransactions;
        public int predictionsCreated;
        public String error;

        public boolean isSuccess() {
            return error == null;
        }

        public String getMessage() {
            if (!isSuccess()) {
                return "Ошибка: " + error;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Импорт завершен!\n");
            sb.append("Всего транзакций: ").append(totalTransactions).append("\n");
            sb.append("Импортировано: ").append(importedTransactions).append("\n");

            if (duplicateTransactions > 0) {
                sb.append("Пропущено дубликатов: ").append(duplicateTransactions).append("\n");
            }

            if (skippedTransactions > 0) {
                sb.append("Пропущено (доходы): ").append(skippedTransactions).append("\n");
            }

            if (predictionsCreated > 0) {
                sb.append("Создано прогнозов: ").append(predictionsCreated);
            }

            return sb.toString();
        }
    }
}
