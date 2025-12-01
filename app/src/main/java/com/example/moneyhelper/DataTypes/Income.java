package com.example.moneyhelper.DataTypes;

public class Income {
    private String category;
    private int amount;

    public Income(String category, int amount) {
        this.category = category;
        this.amount = amount;
    }

    public String getCategory() { return category; }
    public int getAmount() { return amount; }
}