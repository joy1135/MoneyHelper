package com.example.moneyhelper;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.UpcomingExpense;

import java.util.List;

public class UpcomingExpenseAdapter extends RecyclerView.Adapter<UpcomingExpenseAdapter.UpcomingExpenseViewHolder> {

    private List<UpcomingExpense> expenses;

    public UpcomingExpenseAdapter(List<UpcomingExpense> expenses) {
        this.expenses = expenses;
    }

    @NonNull
    @Override
    public UpcomingExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_upcoming_expense, parent, false);
        return new UpcomingExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UpcomingExpenseViewHolder holder, int position) {
        UpcomingExpense expense = expenses.get(position);
        holder.bind(expense);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    public void updateExpenses(List<UpcomingExpense> newExpenses) {
        this.expenses = newExpenses;
        notifyDataSetChanged();
    }

    static class UpcomingExpenseViewHolder extends RecyclerView.ViewHolder {
        private TextView titleTextView;
        private TextView amountTextView;
        private TextView dateTextView;
        private TextView noteTextView;

        public UpcomingExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            noteTextView = itemView.findViewById(R.id.noteTextView);
        }

        public void bind(UpcomingExpense expense) {
            titleTextView.setText(expense.getTitle());
            amountTextView.setText(expense.getAmount() + " ₽");
            dateTextView.setText(expense.getDate());

            if (expense.getNote() != null && !expense.getNote().isEmpty()) {
                noteTextView.setVisibility(View.VISIBLE);
                noteTextView.setText(expense.getNote());

                // Меняем цвет в зависимости от того, прогноз это или реальный расход
                if (expense.getNote().contains("Прогноз")) {
                    noteTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray, null));
                    amountTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.darker_gray, null));
                } else {
                    noteTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.holo_green_dark, null));
                    amountTextView.setTextColor(itemView.getContext().getResources().getColor(android.R.color.black, null));
                }
            } else {
                noteTextView.setVisibility(View.GONE);
            }
        }
    }
}