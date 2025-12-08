package com.example.moneyhelper.predict;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import com.example.moneyhelper.predict.ExpenseData;
import com.example.moneyhelper.predict.PredictionResult;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExpensePredictor {
    private static final String TAG = "ExpensePredictor";

    private final SQLiteDatabase database;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    private final SimpleDateFormat monthFormat = new SimpleDateFormat("MM-yyyy", Locale.getDefault());

    public ExpensePredictor(SQLiteDatabase database) {
        this.database = database;
    }

    /**
     * Основной метод для выполнения предсказаний для всех категорий
     */
    public List<PredictionResult> predictAllCategories() {
        Log.d(TAG, "Начало предсказаний для всех категорий");
        List<PredictionResult> results = new ArrayList<>();

        try {
            // Очищаем предыдущие предсказания
            clearOldPredictions();

            // Получаем все нефиксированные категории пользователя
            List<CategoryInfo> categories = getNonFixedCategories();
            Log.d(TAG, "Найдено категорий для предсказания: " + categories.size());

            for (CategoryInfo category : categories) {
                try {
                    PredictionResult result = predictForCategory(category);
                    results.add(result);

                    // Если предсказание успешно, сохраняем в БД
                    if (result.hasEnoughData() && result.getPredictedAmount() >= 0) {
                        savePrediction(category.userCatId, result.getPredictedAmount());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при предсказании для категории " + category.name, e);
                    results.add(new PredictionResult(category.userCatId, category.name,
                            "Ошибка: " + e.getMessage()));
                }
            }

            Log.d(TAG, "Предсказания завершены. Успешно: " +
                    results.stream().filter(PredictionResult::hasEnoughData).count());

        } catch (Exception e) {
            Log.e(TAG, "Критическая ошибка при выполнении предсказаний", e);
        }

        return results;
    }

    /**
     * Предсказание для конкретной категории
     */
    public PredictionResult predictForCategory(int userCatId) {
        CategoryInfo category = getCategoryInfo(userCatId);
        if (category == null) {
            return new PredictionResult(userCatId, "Неизвестная категория", "Категория не найдена");
        }

        return predictForCategory(category);
    }

    private PredictionResult predictForCategory(CategoryInfo category) {
        Log.d(TAG, "Предсказание для категории: " + category.name + " (ID: " + category.userCatId + ")");

        try {
            // Получаем данные о расходах за предыдущие месяцы
            List<ExpenseData> monthlyExpenses = getMonthlyExpensesForCategory(category.userCatId);

            if (monthlyExpenses.isEmpty()) {
                return new PredictionResult(category.userCatId, category.name,
                        "Нет данных о расходах");
            }

            if (monthlyExpenses.size() < 2) {
                return new PredictionResult(category.userCatId, category.name,
                        "Недостаточно данных. Нужны данные минимум за 2 месяца");
            }

            // Подготавливаем данные для линейной регрессии
            List<Double> xValues = new ArrayList<>();
            List<Double> yValues = new ArrayList<>();

            for (int i = 0; i < monthlyExpenses.size(); i++) {
                ExpenseData data = monthlyExpenses.get(i);
                xValues.add((double) (i + 1)); // X: порядковый номер месяца (1, 2, 3...)
                yValues.add(data.getTotalAmount()); // Y: сумма расходов

                Log.d(TAG, String.format("Месяц %d: сумма=%.2f", i + 1, data.getTotalAmount()));
            }

            // Вычисляем линейную регрессию
            LinearRegressionCalculator.RegressionResult regression =
                    LinearRegressionCalculator.calculateRegression(xValues, yValues);

            if (!regression.isValid) {
                return new PredictionResult(category.userCatId, category.name,
                        regression.errorMessage);
            }

            // Создаем результат предсказания
            PredictionResult result = new PredictionResult(
                    category.userCatId,
                    category.name,
                    regression.nextPrediction,
                    true
            );

            Log.d(TAG, String.format("Предсказание для '%s': %.2f",
                    category.name, regression.nextPrediction));

            return result;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при предсказании для категории " + category.name, e);
            return new PredictionResult(category.userCatId, category.name,
                    "Ошибка вычисления: " + e.getMessage());
        }
    }

    /**
     * Получает данные о расходах по категории, сгруппированные по месяцам
     */
    private List<ExpenseData> getMonthlyExpensesForCategory(int userCatId) {
        List<ExpenseData> result = new ArrayList<>();
        Map<String, Double> monthlyTotals = new HashMap<>();
        List<String> monthKeys = new ArrayList<>();

        String query = "SELECT me.expenses, d.date " +
                "FROM monthly_expenses me " +
                "JOIN dates d ON me.date_id = d.id " +
                "WHERE me.user_cat_id = ? " +
                "ORDER BY d.date";

        Cursor cursor = null;
        try {
            cursor = database.rawQuery(query, new String[]{String.valueOf(userCatId)});

            while (cursor.moveToNext()) {
                double expenses = cursor.getDouble(cursor.getColumnIndexOrThrow("expenses"));
                String dateStr = cursor.getString(cursor.getColumnIndexOrThrow("date"));

                try {
                    Date date = dateFormat.parse(dateStr);
                    String monthKey = monthFormat.format(date);

                    // Суммируем расходы за месяц
                    double currentTotal = monthlyTotals.getOrDefault(monthKey, 0.0);
                    monthlyTotals.put(monthKey, currentTotal + expenses);

                    // Сохраняем порядок месяцев
                    if (!monthKeys.contains(monthKey)) {
                        monthKeys.add(monthKey);
                    }

                } catch (ParseException e) {
                    Log.w(TAG, "Ошибка парсинга даты: " + dateStr, e);
                }
            }

            // Создаем список ExpenseData в правильном порядке
            for (int i = 0; i < monthKeys.size(); i++) {
                String monthKey = monthKeys.get(i);
                double total = monthlyTotals.get(monthKey);
                result.add(new ExpenseData(monthKey, total, i + 1));
            }

            Log.d(TAG, "Найдено " + result.size() + " месяцев данных для категории " + userCatId);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении расходов для категории " + userCatId, e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return result;
    }

    /**
     * Получает информацию о нефиксированных категориях
     */
    private List<CategoryInfo> getNonFixedCategories() {
        List<CategoryInfo> categories = new ArrayList<>();

        String query = "SELECT id, name, fixed FROM user_categories WHERE fixed = 0";
        Cursor cursor = null;

        try {
            cursor = database.rawQuery(query, null);

            while (cursor.moveToNext()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                categories.add(new CategoryInfo(id, name));
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении категорий", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return categories;
    }

    /**
     * Получает информацию о конкретной категории
     */
    private CategoryInfo getCategoryInfo(int userCatId) {
        String query = "SELECT id, name FROM user_categories WHERE id = ?";
        Cursor cursor = null;

        try {
            cursor = database.rawQuery(query, new String[]{String.valueOf(userCatId)});

            if (cursor.moveToFirst()) {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow("id"));
                String name = cursor.getString(cursor.getColumnIndexOrThrow("name"));
                return new CategoryInfo(id, name);
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении информации о категории", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return null;
    }

    /**
     * Сохраняет предсказание в БД
     */
    private void savePrediction(int userCatId, double predictedAmount) {
        try {
            // Удаляем старое предсказание если есть
            database.delete("predict", "user_cat_id = ?",
                    new String[]{String.valueOf(userCatId)});

            // Вставляем новое предсказание
            ContentValues values = new ContentValues();
            values.put("user_cat_id", userCatId);
            values.put("predict", predictedAmount);

            long result = database.insertWithOnConflict("predict", null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);

            if (result != -1) {
                Log.d(TAG, "Сохранено предсказание для категории " + userCatId +
                        ": " + predictedAmount);
            } else {
                Log.e(TAG, "Ошибка сохранения предсказания для категории " + userCatId);
            }

        } catch (SQLException e) {
            Log.e(TAG, "Ошибка БД при сохранении предсказания", e);
        }
    }

    /**
     * Очищает старые предсказания
     */
    private void clearOldPredictions() {
        try {
            int deleted = database.delete("predict", null, null);
            Log.d(TAG, "Удалено старых предсказаний: " + deleted);
        } catch (SQLException e) {
            Log.e(TAG, "Ошибка при очистке предсказаний", e);
        }
    }

    /**
     * Получает все сохраненные предсказания
     */
    public Map<Integer, Double> getAllPredictions() {
        Map<Integer, Double> predictions = new HashMap<>();

        String query = "SELECT p.user_cat_id, p.predict, uc.name " +
                "FROM predict p " +
                "LEFT JOIN user_categories uc ON p.user_cat_id = uc.id";

        Cursor cursor = null;
        try {
            cursor = database.rawQuery(query, null);

            while (cursor.moveToNext()) {
                int userCatId = cursor.getInt(cursor.getColumnIndexOrThrow("user_cat_id"));
                double predict = cursor.getDouble(cursor.getColumnIndexOrThrow("predict"));
                predictions.put(userCatId, predict);
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении предсказаний", e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return predictions;
    }

    /**
     * Вспомогательный класс для хранения информации о категории
     */
    private static class CategoryInfo {
        int userCatId;
        String name;

        CategoryInfo(int userCatId, String name) {
            this.userCatId = userCatId;
            this.name = name;
        }
    }
}