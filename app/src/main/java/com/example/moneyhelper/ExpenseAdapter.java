package com.example.moneyhelper;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Expense;

import java.util.List;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenses;

    public ExpenseAdapter(List<Expense> expenses) {
        this.expenses = expenses;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.bind(expense);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    public void updateExpenses(List<Expense> newExpenses) {
        this.expenses = newExpenses;
        notifyDataSetChanged();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private TextView categoryTextView;
        private TextView amountTextView;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
        }

        public void bind(Expense expense) {
            categoryTextView.setText(expense.getCategory());

            String amountText = expense.getAmount() + "₽";
            amountTextView.setText(amountText);

            // Цвет в зависимости от типа (доход/расход)
            if (expense.isIncome()) {
                amountTextView.setTextColor(itemView.getContext()
                        .getResources().getColor(android.R.color.holo_green_dark));
            } else {
                amountTextView.setTextColor(itemView.getContext()
                        .getResources().getColor(android.R.color.holo_red_dark));
            }
        }
    }
}