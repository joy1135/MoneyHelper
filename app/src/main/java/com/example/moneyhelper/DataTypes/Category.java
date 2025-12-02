package com.example.moneyhelper.DataTypes;

public class Category {
    private String name;
    private int expense;
    private int percentage;
    private int budget;

    public Category(String name, int expense, int percentage, int budget) {
        this.name = name;
        this.expense = expense;
        this.percentage = percentage;
        this.budget = budget;
    }

    public String getName() { return name; }
    public int getExpense() { return expense; }
    public int getPercentage() { return percentage; }
    public int getBudget() { return budget; }
}