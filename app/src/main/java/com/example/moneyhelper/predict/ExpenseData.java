package com.example.moneyhelper.predict;

public class ExpenseData {
    private String monthKey;
    private double totalAmount;
    private int monthNumber;

    public ExpenseData(String monthKey, double totalAmount, int monthNumber) {
        this.monthKey = monthKey;
        this.totalAmount = totalAmount;
        this.monthNumber = monthNumber;
    }

    public String getMonthKey() { return monthKey; }
    public double getTotalAmount() { return totalAmount; }
    public int getMonthNumber() { return monthNumber; }

    public void setMonthNumber(int monthNumber) { this.monthNumber = monthNumber; }
}
