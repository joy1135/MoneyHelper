package com.example.moneyhelper.DataTypes;

import java.util.Date;

public class Expense {
    private long id;  // ID из таблицы monthly_expenses
    private String transactionId;  // transaction_id из БД
    private long userCategoryId;  // ID категории пользователя
    private String categoryName;  // Название категории
    private String categoryIcon;  // Иконка категории
    private double amount;  // Сумма расхода
    private boolean isIncome;  // Доход или расход
    private Date date;  // Дата расхода

    // Конструктор для создания из БД
    public Expense(long id, String transactionId, long userCategoryId, 
                   String categoryName, String categoryIcon, double amount, 
                   boolean isIncome, Date date) {
        this.id = id;
        this.transactionId = transactionId;
        this.userCategoryId = userCategoryId;
        this.categoryName = categoryName;
        this.categoryIcon = categoryIcon;
        this.amount = amount;
        this.isIncome = isIncome;
        this.date = date;
    }

    // Упрощенный конструктор для обратной совместимости
    public Expense(String category, int amount, boolean isIncome) {
        this.categoryName = category;
        this.amount = amount;
        this.isIncome = isIncome;
        this.id = -1;
        this.transactionId = null;
        this.userCategoryId = -1;
        this.categoryIcon = "";
        this.date = new Date();
    }

    // Геттеры
    public long getId() { return id; }
    public String getTransactionId() { return transactionId; }
    public long getUserCategoryId() { return userCategoryId; }
    public String getCategory() { return categoryName; }
    public String getCategoryName() { return categoryName; }
    public String getCategoryIcon() { return categoryIcon; }
    public double getAmount() { return amount; }
    public boolean isIncome() { return isIncome; }
    public Date getDate() { return date; }

    // Сеттеры
    public void setAmount(double amount) { this.amount = amount; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public void setUserCategoryId(long userCategoryId) { this.userCategoryId = userCategoryId; }
    public void setDate(Date date) { this.date = date; }
}