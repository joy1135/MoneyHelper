package com.example.moneyhelper.DataTypes;

public class Expense {
    private String category;
    private int amount;
    private boolean isIncome;

    public Expense(String category, int amount, boolean isIncome) {
        this.category = category;
        this.amount = amount;
        this.isIncome = isIncome;
    }

    public String getCategory() { return category; }
    public int getAmount() { return amount; }
    public boolean isIncome() { return isIncome; }
}