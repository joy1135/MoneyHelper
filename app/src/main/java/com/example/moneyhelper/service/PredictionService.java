package com.example.moneyhelper.service;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.moneyhelper.DatabaseHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Сервис для создания прогнозов расходов
 */
public class PredictionService {
    private static final String TAG = "PredictionService";

    private final Context context;
    private final DatabaseHelper dbHelper;

    public PredictionService(Context context) {
        this.context = context;
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * Создает прогнозы на следующий месяц
     * @return количество созданных прогнозов
     */
    public int createMonthlyPredictions() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int predictionsCreated = 0;

        try {
            db.beginTransaction();

            // Получаем ID следующего месяца
            long nextMonthDateId = getNextMonthDateId(db);

            // Получаем все пользовательские категории
            List<UserCategory> userCategories = getUserCategories(db);

            for (UserCategory userCategory : userCategories) {
                // Проверяем, нет ли уже прогноза для этой категории на следующий месяц
                if (predictionExists(db, userCategory.id, nextMonthDateId)) {
                    Log.d(TAG, "Прогноз уже существует для категории: " + userCategory.name);
                    continue;
                }

                // Рассчитываем прогноз
                double prediction = calculatePrediction(db, userCategory.id);

                if (prediction > 0) {
                    // Сохраняем прогноз
                    ContentValues values = new ContentValues();
                    values.put("user_cat_id", userCategory.id);
                    values.put("predict", prediction);

                    long id = db.insert("predict", null, values);

                    if (id > 0) {
                        predictionsCreated++;
                        Log.d(TAG, String.format(Locale.getDefault(),
                                "Создан прогноз для '%s': %.2f руб.",
                                userCategory.name, prediction));
                    }
                }
            }

            db.setTransactionSuccessful();

        } catch (Exception e) {
            Log.e(TAG, "Ошибка создания прогнозов", e);
        } finally {
            db.endTransaction();
        }

        return predictionsCreated;
    }

    /**
     * Рассчитывает прогноз для категории на основе исторических данных
     */
    private double calculatePrediction(SQLiteDatabase db, long userCatId) {
        // Получаем расходы за последние 3 месяца (или меньше, если данных нет)
        Cursor cursor = db.rawQuery(
                "SELECT me.expenses, d.date " +
                        "FROM monthly_expenses me " +
                        "JOIN dates d ON me.date_id = d.id " +
                        "WHERE me.user_cat_id = ? " +
                        "ORDER BY d.date DESC " +
                        "LIMIT 3",
                new String[]{String.valueOf(userCatId)}
        );

        List<Double> expenses = new ArrayList<>();
        while (cursor.moveToNext()) {
            expenses.add(cursor.getDouble(0));
        }
        cursor.close();

        if (expenses.isEmpty()) {
            return 0;
        }

        // Используем разные методы в зависимости от количества данных
        if (expenses.size() == 1) {
            // Если данные только за 1 месяц - используем их как прогноз
            return expenses.get(0);
        } else if (expenses.size() == 2) {
            // Если данные за 2 месяца - среднее арифметическое
            return (expenses.get(0) + expenses.get(1)) / 2.0;
        } else {
            // Если данные за 3+ месяца - взвешенное среднее
            // Даем больший вес последнему месяцу
            return calculateWeightedAverage(expenses);
        }
    }

    /**
     * Рассчитывает взвешенное среднее (больший вес последним месяцам)
     */
    private double calculateWeightedAverage(List<Double> expenses) {
        if (expenses.size() < 3) {
            return expenses.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        }

        // Веса: последний месяц - 0.5, предпоследний - 0.3, остальные - 0.2
        double[] weights = {0.5, 0.3, 0.2};
        double weightedSum = 0;

        for (int i = 0; i < Math.min(3, expenses.size()); i++) {
            weightedSum += expenses.get(i) * weights[i];
        }

        return weightedSum;
    }

    /**
     * Проверяет, существует ли прогноз для категории на указанный месяц
     */
    private boolean predictionExists(SQLiteDatabase db, long userCatId, long dateId) {
        Cursor cursor = db.query(
                "predict",
                new String[]{"id"},
                "user_cat_id = ?",
                new String[]{String.valueOf(userCatId)},
                null, null, null
        );

        boolean exists = cursor.getCount() > 0;
        cursor.close();

        return exists;
    }

    /**
     * Получает date_id для следующего месяца
     */
    private long getNextMonthDateId(SQLiteDatabase db) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 1);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
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
     * Получает все пользовательские категории
     */
    private List<UserCategory> getUserCategories(SQLiteDatabase db) {
        List<UserCategory> categories = new ArrayList<>();

        Cursor cursor = db.query(
                "user_categories",
                new String[]{"id", "name", "fixed"},
                null, null, null, null, null
        );

        while (cursor.moveToNext()) {
            UserCategory category = new UserCategory();
            category.id = cursor.getLong(0);
            category.name = cursor.getString(1);
            category.fixed = cursor.getInt(2) == 1;
            categories.add(category);
        }
        cursor.close();

        return categories;
    }

    /**
     * Вспомогательный класс для категорий
     */
    private static class UserCategory {
        long id;
        String name;
        boolean fixed;
    }

    /**
     * Получает прогноз для категории
     */
    public Double getPrediction(long userCatId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                "predict",
                new String[]{"predict"},
                "user_cat_id = ?",
                new String[]{String.valueOf(userCatId)},
                null, null, null
        );

        Double prediction = null;
        if (cursor.moveToFirst()) {
            prediction = cursor.getDouble(0);
        }
        cursor.close();

        return prediction;
    }

    /**
     * Получает все прогнозы
     */
    public Map<Long, Double> getAllPredictions() {
        Map<Long, Double> predictions = new HashMap<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query(
                "predict",
                new String[]{"user_cat_id", "predict"},
                null, null, null, null, null
        );

        while (cursor.moveToNext()) {
            predictions.put(cursor.getLong(0), cursor.getDouble(1));
        }
        cursor.close();

        return predictions;
    }
}