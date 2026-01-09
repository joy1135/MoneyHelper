package com.example.moneyhelper.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.moneyhelper.DatabaseHelper;
import com.example.moneyhelper.DataTypes.Category;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Сервис для работы с категориями
 */
public class CategoryService {

    private static final String TAG = "CategoryService";

    private final DatabaseHelper dbHelper;
    private final SimpleDateFormat dateFormat;

    public CategoryService(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.dateFormat = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
    }

    /**
     * Получить все категории текущего пользователя за текущий месяц
     */
    public List<Category> getAllCategories() {
        return getCategoriesForMonth(new Date());
    }

    /**
     * Получить категории за определенный месяц
     */
    public List<Category> getCategoriesForMonth(Date month) {
        List<Category> categories = new ArrayList<>();

        // Получаем первое число месяца
        Calendar cal = Calendar.getInstance();
        cal.setTime(month);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        String monthStr = dateFormat.format(cal.getTime());

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // SQL запрос для получения категорий с расходами и прогнозами
        String query =
                "SELECT " +
                        "    uc.id as user_cat_id, " +
                        "    uc.cat_id, " +
                        "    uc.name, " +
                        "    c.icon, " +
                        "    uc.fixed, " +
                        "    COALESCE(SUM(me.expenses), 0) as current_expense, " +
                        "    COALESCE(p.predict, 0) as budget " +
                        "FROM user_categories uc " +
                        "JOIN categories c ON uc.cat_id = c.id " +
                        "LEFT JOIN monthly_expenses me ON me.user_cat_id = uc.id " +
                        "LEFT JOIN dates d ON me.date_id = d.id " +
                        "LEFT JOIN predict p ON p.user_cat_id = uc.id " +
                        "WHERE uc.user_id = ? AND d.date = ?" +
                        "GROUP BY uc.id " +
                        "ORDER BY current_expense DESC";

        try (Cursor cursor = db.rawQuery(query,
                new String[]{ String.valueOf(getCurrentUserId()), monthStr})) {

//            Log.d(TAG, "SQL: " + cursor.);

            double totalExpense = 0;
            List<Category> tempList = new ArrayList<>();

            // Первый проход - собираем категории и считаем общую сумму
            while (cursor.moveToNext()) {
                long userCatId = cursor.getLong(0);
                long catId = cursor.getLong(1);
                String name = cursor.getString(2);
                String icon = cursor.getString(3);
                boolean isFixed = cursor.getInt(4) == 1;
                double currentExpense = cursor.getDouble(5);
                double budget = cursor.getDouble(6);

                Category category = new Category(userCatId, catId, name, icon,
                        isFixed, currentExpense, budget);
                category.setMonthDate(cal.getTime());

                tempList.add(category);
                totalExpense += currentExpense;
            }

            // Второй проход - вычисляем проценты
            for (Category category : tempList) {
                if (totalExpense > 0) {
                    int percentage = (int) ((category.getCurrentExpense() / totalExpense) * 100);
                    category.setPercentage(percentage);
                }
                categories.add(category);
            }

            Log.d(TAG, String.format("Загружено %d категорий за %s, общая сумма: %.2f",
                    categories.size(), monthStr, totalExpense));

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке категорий", e);
        }

        return categories;
    }

    /**
     * Получить категорию по ID
     */
    public Category getCategoryById(long userCategoryId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query =
                "SELECT " +
                        "    uc.id, uc.cat_id, uc.name, c.icon, uc.fixed " +
                        "FROM user_categories uc " +
                        "JOIN categories c ON uc.cat_id = c.id " +
                        "WHERE uc.id = ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userCategoryId)})) {
            if (cursor.moveToFirst()) {
                return new Category(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getInt(4) == 1,
                        0,
                        0
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении категории", e);
        }

        return null;
    }

    /**
     * Создать новую категорию
     */
    public long createCategory(String name, String icon, boolean isFixed) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            // 1. Создаем запись в categories (если такой еще нет)
            long categoryId = getOrCreateGlobalCategory(name, icon);

            // 2. Создаем запись в user_categories
            ContentValues values = new ContentValues();
            values.put("user_id", getCurrentUserId());
            values.put("cat_id", categoryId);
            values.put("name", name);
            values.put("fixed", isFixed ? 1 : 0);

            long userCategoryId = db.insert("user_categories", null, values);

            db.setTransactionSuccessful();

            Log.d(TAG, String.format("Создана категория: %s (id=%d)", name, userCategoryId));

            return userCategoryId;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании категории", e);
            return -1;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Обновить категорию
     */
    public boolean updateCategory(long userCategoryId, String name, String icon, boolean isFixed) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            // Обновляем user_categories
            ContentValues values = new ContentValues();
            values.put("name", name);
            values.put("fixed", isFixed ? 1 : 0);

            int rows = db.update("user_categories", values,
                    "id = ?",
                    new String[]{String.valueOf(userCategoryId)});

            // Обновляем иконку в categories (если категория принадлежит пользователю)
            if (icon != null) {
                db.execSQL(
                        "UPDATE categories SET icon = ? " +
                                "WHERE id = (SELECT cat_id FROM user_categories WHERE id = ?)",
                        new Object[]{icon, userCategoryId}
                );
            }

            db.setTransactionSuccessful();

            Log.d(TAG, String.format("Обновлена категория id=%d, изменено строк: %d",
                    userCategoryId, rows));

            return rows > 0;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении категории", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Удалить категорию
     */
    public boolean deleteCategory(long userCategoryId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            // Благодаря ON DELETE CASCADE в схеме БД,
            // связанные записи в monthly_expenses и predict удалятся автоматически
            int rows = db.delete("user_categories",
                    "id = ?",
                    new String[]{String.valueOf(userCategoryId)});

            Log.d(TAG, String.format("Удалена категория id=%d, удалено строк: %d",
                    userCategoryId, rows));

            return rows > 0;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при удалении категории", e);
            return false;
        }
    }

    /**
     * Получить или создать глобальную категорию
     */
    private long getOrCreateGlobalCategory(String name, String icon) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Ищем существующую категорию
        try (Cursor cursor = db.query("categories",
                new String[]{"id"},
                "name = ?",
                new String[]{name},
                null, null, null)) {

            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }

        // Создаем новую
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("icon", icon != null ? icon : "📦");

        return db.insert("categories", null, values);
    }

    /**
     * Получить ID текущего пользователя
     * TODO: Заменить на реальную логику получения текущего пользователя
     */
    private long getCurrentUserId() {
        // Пока возвращаем первого пользователя
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query("users", new String[]{"id"},
                null, null, null, null, null, "1")) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }
        return 1; // Fallback
    }

    /**
     * Получить топ N категорий расходов за месяц
     */
    public List<Category> getTopCategories(Date month, int limit) {
        List<Category> allCategories = getCategoriesForMonth(month);
        
        // Фильтруем категории с расходами > 0 и берем топ N
        List<Category> topCategories = new ArrayList<>();
        for (Category category : allCategories) {
            if (category.getCurrentExpense() > 0) {
                topCategories.add(category);
                if (topCategories.size() >= limit) {
                    break;
                }
            }
        }
        
        return topCategories;
    }
    
    /**
     * Получить общий доход за месяц
     * TODO: Реализовать получение доходов из БД, если есть таблица доходов
     */
    public double getTotalIncome(Date month) {
        // Пока возвращаем 0, так как доходы не хранятся в БД
        // В будущем можно добавить таблицу incomes или использовать другую логику
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String monthStr = dateFormat.format(cal.getTime());
        
        // Проверяем, есть ли таблица incomes
        try {
            String query = "SELECT COALESCE(SUM(amount), 0) FROM incomes " +
                          "WHERE user_id = ? AND date = ?";
            try (Cursor cursor = db.rawQuery(query, 
                    new String[]{String.valueOf(getCurrentUserId()), monthStr})) {
                if (cursor.moveToFirst()) {
                    return cursor.getDouble(0);
                }
            }
        } catch (Exception e) {
            // Таблицы incomes нет, возвращаем 0
            Log.d(TAG, "Таблица incomes не найдена, доходы не учитываются");
        }
        
        return 0.0;
    }
    
    /**
     * Получить общий расход за месяц
     */
    public double getTotalExpense(Date month) {
        CategoryStats stats = getCategoryStats(month);
        return stats.totalExpense;
    }
    
    /**
     * Получить баланс (доход - расход) за месяц
     */
    public double getBalance(Date month) {
        double income = getTotalIncome(month);
        double expense = getTotalExpense(month);
        return income - expense;
    }

    /**
     * Получить статистику по категориям
     */
    public CategoryStats getCategoryStats(Date month) {
        List<Category> categories = getCategoriesForMonth(month);

        double totalExpense = 0;
        double totalBudget = 0;
        int overBudgetCount = 0;

        for (Category category : categories) {
            totalExpense += category.getCurrentExpense();
            totalBudget += category.getBudget();
            if (category.isOverBudget()) {
                overBudgetCount++;
            }
        }

        return new CategoryStats(
                categories.size(),
                totalExpense,
                totalBudget,
                overBudgetCount
        );
    }

    /**
     * Класс для статистики категорий
     */
    public static class CategoryStats {
        public final int totalCategories;
        public final double totalExpense;
        public final double totalBudget;
        public final int overBudgetCount;

        public CategoryStats(int totalCategories, double totalExpense,
                             double totalBudget, int overBudgetCount) {
            this.totalCategories = totalCategories;
            this.totalExpense = totalExpense;
            this.totalBudget = totalBudget;
            this.overBudgetCount = overBudgetCount;
        }

        public double getRemainingBudget() {
            return Math.max(0, totalBudget - totalExpense);
        }

        public int getBudgetFulfillment() {
            if (totalBudget == 0) return 0;
            return (int) ((totalExpense / totalBudget) * 100);
        }
    }
}