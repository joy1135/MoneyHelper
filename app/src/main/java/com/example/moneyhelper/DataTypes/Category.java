package com.example.moneyhelper.DataTypes;


import java.util.Date;

/**
 * Модель категории
 * Соответствует таблицам: categories, user_categories, monthly_expenses
 */
public class Category {
    // ID из таблицы user_categories
    private long userCategoryId;

    // ID из таблицы categories
    private long categoryId;

    // Данные категории
    private String name;
    private String icon;

    // Данные пользовательской категории
    private boolean isFixed;  // Фиксированная категория или нет

    // Расчетные данные (из monthly_expenses)
    private double currentExpense;  // Текущие расходы за месяц
    private double budget;          // Прогноз/бюджет (из predict)
    private int percentage;         // Процент от общих расходов

    // Данные для отображения
    private Date monthDate;         // За какой месяц данные

    // Конструктор для создания из БД
    public Category(long userCategoryId, long categoryId, String name, String icon,
                    boolean isFixed, double currentExpense, double budget) {
        this.userCategoryId = userCategoryId;
        this.categoryId = categoryId;
        this.name = name;
        this.icon = icon;
        this.isFixed = isFixed;
        this.currentExpense = currentExpense;
        this.budget = budget;
        this.percentage = 0;
    }

    // Упрощенный конструктор
    public Category(String name, String icon) {
        this.name = name;
        this.icon = icon;
        this.isFixed = false;
        this.currentExpense = 0;
        this.budget = 0;
        this.percentage = 0;
    }

    // Геттеры
    public long getUserCategoryId() {
        return userCategoryId;
    }

    public long getCategoryId() {
        return categoryId;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon == null ? "" : icon;
    }

    public boolean isFixed() {
        return isFixed;
    }

    public double getCurrentExpense() {
        return currentExpense;
    }

    public double getBudget() {
        return budget;
    }

    public int getPercentage() {
        return percentage;
    }

    public Date getMonthDate() {
        return monthDate;
    }
    public long getCatId() { return categoryId; }

    // Сеттеры
    public void setUserCategoryId(long userCategoryId) {
        this.userCategoryId = userCategoryId;
    }

    public void setCategoryId(long categoryId) {
        this.categoryId = categoryId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public void setFixed(boolean fixed) {
        isFixed = fixed;
    }

    public void setCurrentExpense(double currentExpense) {
        this.currentExpense = currentExpense;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public void setPercentage(int percentage) {
        this.percentage = percentage;
    }

    public void setMonthDate(Date monthDate) {
        this.monthDate = monthDate;
    }

    // Вычисляемые поля

    /**
     * Получить разницу между расходами и бюджетом
     * @return положительное значение = перерасход, отрицательное = экономия
     */
    public double getDifference() {
        return currentExpense - budget;
    }

    /**
     * Процент выполнения бюджета
     * @return 0-100+ (может быть больше 100 при перерасходе)
     */
    public int getBudgetFulfillment() {
        if (budget == 0) return 0;
        return (int) ((currentExpense / budget) * 100);
    }

    /**
     * Есть ли перерасход
     */
    public boolean isOverBudget() {
        return currentExpense > budget && budget > 0;
    }

    /**
     * Остаток бюджета
     */
    public double getRemainingBudget() {
        return Math.max(0, budget - currentExpense);
    }

    @Override
    public String toString() {
        return String.format("Category{name='%s', expense=%.2f, budget=%.2f, percentage=%d%%}",
                name, currentExpense, budget, percentage);
    }

    public String getDisplayName() {
        return name;
    }
}