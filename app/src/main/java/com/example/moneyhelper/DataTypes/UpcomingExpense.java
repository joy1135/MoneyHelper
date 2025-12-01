package com.example.moneyhelper.DataTypes;

public class UpcomingExpense {
    private String title;
    private int amount;
    private String date;
    private String note;

    public UpcomingExpense(String title, int amount, String date, String note) {
        this.title = title;
        this.amount = amount;
        this.date = date;
        this.note = note;
    }

    public String getTitle() { return title; }
    public int getAmount() { return amount; }
    public String getDate() { return date; }
    public String getNote() { return note; }
}