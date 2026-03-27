package com.example.moneyhelper.service;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.moneyhelper.DatabaseHelper;
import com.example.moneyhelper.DataTypes.Category;
import com.example.moneyhelper.DataTypes.Expense;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Сервис для работы с категориями
 */
public class CategoryService {

    private static final String TAG = "CategoryService";

    private final DatabaseHelper dbHelper;
    private final SimpleDateFormat dateFormat;
    private final Locale currentLocale;

    public CategoryService(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        this.currentLocale = Locale.getDefault();
    }

    /**
     * Получить имя категории в зависимости от текущей локали
     */
    private String getLocalizedName(Cursor cursor, String nameColumn, String nameEnColumn) {
        // Если текущий язык английский и есть name_en, используем его
        if (currentLocale.getLanguage().equals("en")) {
            int nameEnIndex = cursor.getColumnIndex(nameEnColumn);
            if (nameEnIndex >= 0) {
                String englishName = cursor.getString(nameEnIndex);
                if (englishName != null && !englishName.isEmpty()) {
                    return englishName;
                }
            }
        }

        // Иначе возвращаем русское имя
        int nameIndex = cursor.getColumnIndex(nameColumn);
        if (nameIndex >= 0) {
            return cursor.getString(nameIndex);
        }

        // Если ни один столбец не найден, возвращаем пустую строку
        Log.e(TAG, "Columns " + nameColumn + " and " + nameEnColumn + " not found in cursor");
        return "";
    }

    /**
     * Получить все категории текущего пользователя за текущий месяц
     */
    public List<Category> getAllCategories() {
        return getCategoriesForMonth(new Date());
    }

    /**
     * Получить все категории пользователя (без фильтрации по месяцу)
     * Используется для выбора категории при добавлении расхода
     */
    public List<Category> getAllUserCategories() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query =
                "SELECT " +
                        "    uc.id as user_cat_id, " +
                        "    uc.cat_id, " +
                        "    uc.name, " +
                        "    c.name, " +
                        "    c.name_en, " +
                        "    c.icon, " +
                        "    uc.fixed " +
                        "FROM user_categories uc " +
                        "JOIN categories c ON uc.cat_id = c.id " +
                        "WHERE uc.user_id = ? " +
                        "ORDER BY uc.name";

        try (Cursor cursor = db.rawQuery(query,
                new String[]{String.valueOf(getCurrentUserId())})) {

            while (cursor.moveToNext()) {
                long userCatId = cursor.getLong(0);
                long catId = cursor.getLong(1);
                String localizedName = getLocalizedName(cursor, "name", "name_en");
                String icon = cursor.getString(5);
                boolean isFixed = cursor.getInt(6) == 1;

                Category category = new Category(userCatId, catId, localizedName, icon,
                        isFixed, 0, 0);
                categories.add(category);
            }

            Log.d(TAG, String.format("Загружено %d категорий пользователя", categories.size()));

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке категорий пользователя", e);
        }

        return categories;
    }

    public List<Category> getCategoriesForMonthForPrediction(Date month) {
        List<Category> categories = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.setTime(month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String monthStr = dateFormat.format(cal.getTime());

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query =
                "SELECT " +
                        "    uc.id as user_cat_id, " +
                        "    uc.cat_id, " +
                        "    uc.name, " +
                        "    c.name, " +
                        "    c.name_en, " +
                        "    c.icon, " +
                        "    uc.fixed, " +
                        "    COALESCE(ex.sum_expenses, 0) as current_expense, " +
                        "    COALESCE(p.predict, 0) as budget " +
                        "FROM user_categories uc " +
                        "JOIN categories c ON uc.cat_id = c.id " +
                        "LEFT JOIN predict p ON p.user_cat_id = uc.id " +
                        "LEFT JOIN (" +
                        "    SELECT me.user_cat_id, SUM(me.expenses) as sum_expenses " +
                        "    FROM monthly_expenses me " +
                        "    JOIN dates d ON me.date_id = d.id " +
                        "    WHERE d.date = ? AND (me.is_income = 0 OR me.is_income IS NULL) " +
                        "    GROUP BY me.user_cat_id " +
                        ") ex ON uc.id = ex.user_cat_id " +
                        "WHERE uc.user_id = ? " +
                        "ORDER BY current_expense DESC";

        try (Cursor cursor = db.rawQuery(query, new String[]{monthStr, String.valueOf(getCurrentUserId())})) {
            double totalExpense = 0;
            List<Category> tempList = new ArrayList<>();

            while (cursor.moveToNext()) {
                long userCatId = cursor.getLong(0);
                long catId = cursor.getLong(1);
                String localizedName = getLocalizedName(cursor, "name", "name_en");
                String icon = cursor.getString(5);
                boolean isFixed = cursor.getInt(6) == 1;
                double currentExpense = cursor.getDouble(7);
                double budget = cursor.getDouble(8);

                Category category = new Category(userCatId, catId, localizedName, icon, isFixed, currentExpense, budget);
                category.setMonthDate(cal.getTime());

                tempList.add(category);
                totalExpense += currentExpense;
            }

            for (Category category : tempList) {
                if (totalExpense > 0) {
                    category.setPercentage((int) ((category.getCurrentExpense() / totalExpense) * 100));
                }
                categories.add(category);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке категорий", e);
        }

        return categories;
    }

    /**
     * Получить категории за определенный месяц
     */
    public List<Category> getCategoriesForMonth(Date month) {
        List<Category> categories = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.setTime(month);
        cal.set(Calendar.DAY_OF_MONTH, 1);

        String monthStr = dateFormat.format(cal.getTime());

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query =
                "SELECT " +
                        "    uc.id as user_cat_id, " +
                        "    uc.cat_id, " +
                        "    uc.name, " +
                        "    c.name, " +
                        "    c.name_en, " +
                        "    c.icon, " +
                        "    uc.fixed, " +
                        "    COALESCE(SUM(me.expenses), 0) as current_expense, " +
                        "    COALESCE(p.predict, 0) as budget " +
                        "FROM user_categories uc " +
                        "JOIN categories c ON uc.cat_id = c.id " +
                        "LEFT JOIN monthly_expenses me ON me.user_cat_id = uc.id " +
                        "    AND (me.is_income = 0 OR me.is_income IS NULL) " +
                        "LEFT JOIN dates d ON me.date_id = d.id " +
                        "LEFT JOIN predict p ON p.user_cat_id = uc.id " +
                        "WHERE uc.user_id = ? AND d.date = ?" +
                        "GROUP BY uc.id " +
                        "ORDER BY current_expense DESC";

        try (Cursor cursor = db.rawQuery(query,
                new String[]{ String.valueOf(getCurrentUserId()), monthStr})) {

            double totalExpense = 0;
            List<Category> tempList = new ArrayList<>();

            while (cursor.moveToNext()) {
                long userCatId = cursor.getLong(0);
                long catId = cursor.getLong(1);
                String localizedName = getLocalizedName(cursor, "name", "name_en");
                String icon = cursor.getString(5);
                boolean isFixed = cursor.getInt(6) == 1;
                double currentExpense = cursor.getDouble(7);
                double budget = cursor.getDouble(8);

                Category category = new Category(userCatId, catId, localizedName, icon,
                        isFixed, currentExpense, budget);
                category.setMonthDate(cal.getTime());

                tempList.add(category);
                totalExpense += currentExpense;
            }

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
                        "    uc.id, uc.cat_id, uc.name, " +
                        "    c.name, c.name_en, c.icon, uc.fixed " +
                        "FROM user_categories uc " +
                        "JOIN categories c ON uc.cat_id = c.id " +
                        "WHERE uc.id = ?";

        try (Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(userCategoryId)})) {
            if (cursor.moveToFirst()) {
                String localizedName = getLocalizedName(cursor, "name", "name_en");
                return new Category(
                        cursor.getLong(0),
                        cursor.getLong(1),
                        localizedName,
                        cursor.getString(5),
                        cursor.getInt(6) == 1,
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
    public long createCategory(String name, String nameEn, String icon, boolean isFixed) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        db.beginTransaction();
        try {
            // 1. Создаем запись в categories (если такой еще нет)
            long categoryId = getOrCreateGlobalCategory(name, nameEn, icon);

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
    public boolean updateCategory(long userCategoryId, String name, String nameEn, String icon, boolean isFixed) {
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

            // Обновляем иконку и английское имя в categories
            if (icon != null || nameEn != null) {
                StringBuilder sql = new StringBuilder("UPDATE categories SET ");
                List<Object> args = new ArrayList<>();

                if (icon != null) {
                    sql.append("icon = ?");
                    args.add(icon);
                }
                if (nameEn != null) {
                    if (icon != null) sql.append(", ");
                    sql.append("name_en = ?");
                    args.add(nameEn);
                }

                sql.append(" WHERE id = (SELECT cat_id FROM user_categories WHERE id = ?)");
                args.add(userCategoryId);

                db.execSQL(sql.toString(), args.toArray());
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
     * Добавить расход для категории на указанный месяц
     */
    public boolean addExpense(long userCategoryId, double amount, Date month) {
        if (amount <= 0) {
            Log.e(TAG, "Сумма расхода должна быть больше 0");
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            db.beginTransaction();

            Calendar cal = Calendar.getInstance();
            if (month != null) {
                cal.setTime(month);
            }
            cal.set(Calendar.DAY_OF_MONTH, 1);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            String monthStr = dateFormat.format(cal.getTime());

            long dateId;
            try (Cursor cursor = db.query(
                    "dates",
                    new String[]{"id"},
                    "date = ?",
                    new String[]{monthStr},
                    null, null, null)) {

                if (cursor.moveToFirst()) {
                    dateId = cursor.getLong(0);
                } else {
                    ContentValues dateValues = new ContentValues();
                    dateValues.put("date", monthStr);
                    dateId = db.insert("dates", null, dateValues);
                }
            }

            String transactionId = UUID.randomUUID().toString();

            ContentValues values = new ContentValues();
            values.put("user_cat_id", userCategoryId);
            values.put("expenses", amount);
            values.put("date_id", dateId);
            values.put("is_income", 0);
            values.put("transaction_id", transactionId);

            long result = db.insert("monthly_expenses", null, values);

            db.setTransactionSuccessful();

            if (result > 0) {
                Log.d(TAG, String.format("Добавлен расход %.2f для категории %d", amount, userCategoryId));
                return true;
            } else {
                Log.e(TAG, "Ошибка добавления расхода");
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при добавлении расхода", e);
            return false;
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Добавить расход для категории на текущий месяц
     */
    public boolean addExpense(long userCategoryId, double amount) {
        return addExpense(userCategoryId, amount, null);
    }

    /**
     * Получить все расходы за указанный месяц
     */
    public List<Expense> getExpensesForMonth(Date month) {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Calendar cal = Calendar.getInstance();
        if (month != null) {
            cal.setTime(month);
        }
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        String monthStr = dateFormat.format(cal.getTime());

        String query =
                "SELECT " +
                        "    me.id, " +
                        "    me.transaction_id, " +
                        "    me.user_cat_id, " +
                        "    uc.name as category_name, " +
                        "    c.name, " +
                        "    c.name_en, " +
                        "    c.icon as category_icon, " +
                        "    me.expenses, " +
                        "    COALESCE(me.is_income, 0) as is_income, " +
                        "    d.date " +
                        "FROM monthly_expenses me " +
                        "JOIN user_categories uc ON me.user_cat_id = uc.id " +
                        "JOIN categories c ON uc.cat_id = c.id " +
                        "JOIN dates d ON me.date_id = d.id " +
                        "WHERE uc.user_id = ? AND d.date = ? " +
                        "ORDER BY me.id DESC";

        try (Cursor cursor = db.rawQuery(query,
                new String[]{String.valueOf(getCurrentUserId()), monthStr})) {

            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String transactionId = cursor.getString(1);
                long userCatId = cursor.getLong(2);
                String localizedName = getLocalizedName(cursor, "name", "name_en");
                String categoryIcon = cursor.getString(6);
                double amount = cursor.getDouble(7);
                boolean isIncome = cursor.getInt(8) == 1;
                String dateStr = cursor.getString(9);

                Date expenseDate;
                try {
                    expenseDate = dateFormat.parse(dateStr);
                } catch (Exception e) {
                    expenseDate = cal.getTime();
                }

                Expense expense = new Expense(id, transactionId, userCatId,
                        localizedName, categoryIcon, amount, isIncome, expenseDate);
                expenses.add(expense);
            }

            Log.d(TAG, String.format("Загружено %d расходов за %s", expenses.size(), monthStr));

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке расходов", e);
        }

        return expenses;
    }

    /**
     * Обновить расход
     */
    public boolean updateExpense(long expenseId, long userCategoryId, double amount) {
        if (amount <= 0) {
            Log.e(TAG, "Сумма расхода должна быть больше 0");
            return false;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put("user_cat_id", userCategoryId);
            values.put("expenses", amount);

            int rows = db.update("monthly_expenses", values,
                    "id = ?",
                    new String[]{String.valueOf(expenseId)});

            Log.d(TAG, String.format("Обновлен расход id=%d, изменено строк: %d", expenseId, rows));

            return rows > 0;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обновлении расхода", e);
            return false;
        }
    }

    /**
     * Удалить расход
     */
    public boolean deleteExpense(long expenseId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            int rows = db.delete("monthly_expenses",
                    "id = ?",
                    new String[]{String.valueOf(expenseId)});

            Log.d(TAG, String.format("Удален расход id=%d, удалено строк: %d", expenseId, rows));

            return rows > 0;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при удалении расхода", e);
            return false;
        }
    }

    /**
     * Получить все транзакции по категории за указанный месяц
     */
    public List<Expense> getExpensesByCategory(long userCategoryId, Date month) {
        List<Expense> expenses = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Calendar cal = Calendar.getInstance();
        if (month != null) {
            cal.setTime(month);
        }
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        String monthStr = dateFormat.format(cal.getTime());

        String query =
                "SELECT " +
                        "    me.id, " +
                        "    me.transaction_id, " +
                        "    me.user_cat_id, " +
                        "    uc.name as category_name, " +
                        "    c.name, " +
                        "    c.name_en, " +
                        "    c.icon as category_icon, " +
                        "    me.expenses, " +
                        "    COALESCE(me.is_income, 0) as is_income, " +
                        "    d.date " +
                        "FROM monthly_expenses me " +
                        "JOIN user_categories uc ON me.user_cat_id = uc.id " +
                        "JOIN categories c ON uc.cat_id = c.id " +
                        "JOIN dates d ON me.date_id = d.id " +
                        "WHERE me.user_cat_id = ? AND d.date = ? " +
                        "ORDER BY me.id DESC";

        try (Cursor cursor = db.rawQuery(query,
                new String[]{String.valueOf(userCategoryId), monthStr})) {

            while (cursor.moveToNext()) {
                long id = cursor.getLong(0);
                String transactionId = cursor.getString(1);
                long userCatId = cursor.getLong(2);
                String localizedName = getLocalizedName(cursor, "name", "name_en");
                String categoryIcon = cursor.getString(6);
                double amount = cursor.getDouble(7);
                boolean isIncome = cursor.getInt(8) == 1;
                String dateStr = cursor.getString(9);

                Date expenseDate;
                try {
                    expenseDate = dateFormat.parse(dateStr);
                } catch (Exception e) {
                    expenseDate = cal.getTime();
                }

                Expense expense = new Expense(id, transactionId, userCatId,
                        localizedName, categoryIcon, amount, isIncome, expenseDate);
                expenses.add(expense);
            }

            Log.d(TAG, String.format("Загружено %d транзакций для категории %d за %s",
                    expenses.size(), userCategoryId, monthStr));

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке транзакций по категории", e);
        }

        return expenses;
    }

    /**
     * Получить или создать глобальную категорию
     */
    private long getOrCreateGlobalCategory(String name, String nameEn, String icon) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // Ищем существующую категорию по русскому имени или английскому
        try (Cursor cursor = db.query("categories",
                new String[]{"id"},
                "name = ? OR name_en = ?",
                new String[]{name, nameEn != null ? nameEn : name},
                null, null, null)) {

            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }

        // Создаем новую
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("name_en", nameEn != null ? nameEn : name);
        values.put("icon", icon != null ? icon : "📦");

        return db.insert("categories", null, values);
    }

    /**
     * Получить ID текущего пользователя
     */
    private long getCurrentUserId() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query("users", new String[]{"id"},
                null, null, null, null, null, "1")) {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        }
        return 1;
    }

    /**
     * Получить топ N категорий расходов за месяц
     */
    public List<Category> getTopCategories(Date month, int limit) {
        List<Category> allCategories = getCategoriesForMonth(month);

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
     * Получить категории с прогнозами на следующий месяц из таблицы predict
     */
    public List<Category> getCategoriesWithPredictions() {
        List<Category> categories = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String query =
                "SELECT " +
                        "    uc.id as user_cat_id, " +
                        "    uc.cat_id, " +
                        "    uc.name, " +
                        "    c.name, " +
                        "    c.name_en, " +
                        "    c.icon, " +
                        "    uc.fixed, " +
                        "    COALESCE(p.predict, 0) as prediction " +
                        "FROM predict p " +
                        "JOIN user_categories uc ON p.user_cat_id = uc.id " +
                        "JOIN categories c ON uc.cat_id = c.id " +
                        "WHERE uc.user_id = ? " +
                        "ORDER BY p.predict DESC";

        try (Cursor cursor = db.rawQuery(query,
                new String[]{String.valueOf(getCurrentUserId())})) {

            double totalPrediction = 0;
            List<Category> tempList = new ArrayList<>();

            while (cursor.moveToNext()) {
                long userCatId = cursor.getLong(0);
                long catId = cursor.getLong(1);
                String localizedName = getLocalizedName(cursor, "name", "name_en");
                String icon = cursor.getString(5);
                boolean isFixed = cursor.getInt(6) == 1;
                double prediction = cursor.getDouble(7);

                Category category = new Category(userCatId, catId, localizedName, icon,
                        isFixed, 0, prediction);

                tempList.add(category);
                totalPrediction += prediction;
            }

            for (Category category : tempList) {
                if (totalPrediction > 0) {
                    int percentage = (int) ((category.getBudget() / totalPrediction) * 100);
                    category.setPercentage(percentage);
                }
                categories.add(category);
            }

            Log.d(TAG, String.format("Загружено %d категорий с прогнозами, общая сумма прогнозов: %.2f",
                    categories.size(), totalPrediction));

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при загрузке категорий с прогнозами", e);
        }

        return categories;
    }

    /**
     * Получить общий доход за месяц
     */
    public double getTotalIncome(Date month) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Calendar cal = Calendar.getInstance();
        cal.setTime(month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        String monthStr = dateFormat.format(cal.getTime());

        try {
            String query = "SELECT money from users where id = ?";
            try(Cursor cursor= db.rawQuery(query, new String[]{String.valueOf(getCurrentUserId())})) {
                if (cursor.moveToFirst()){
                    double income = cursor.getDouble(0);
                    Log.d(TAG, String.format("Доход за %s: %.2f", monthStr, income));
                    return income;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении дохода", e);
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