package com.example.moneyhelper;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Expense;

import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private List<Expense> expenses;
    private ExpenseClickListener clickListener;

    public ExpenseAdapter(List<Expense> expenses, ExpenseClickListener clickListener) {
        this.expenses = expenses;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category_simple, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.bind(expense, clickListener);
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    public void updateExpenses(List<Expense> newExpenses) {
        this.expenses = newExpenses;
        notifyDataSetChanged();
    }

    /**
     * Интерфейс для обработки кликов
     */
    public interface ExpenseClickListener {
        void onEditClick(Expense expense);
        void onDeleteClick(Expense expense);
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        private TextView tvName;
        private TextView tvFixed;
        private ImageButton btnEdit;
        private ImageButton btnDelete;

        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvFixed = itemView.findViewById(R.id.tvFixed);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Expense expense, ExpenseClickListener listener) {
            // Формируем текст: иконка + название категории + сумма
            String categoryText = "";
            if (expense.getCategoryIcon() != null && !expense.getCategoryIcon().isEmpty()) {
                categoryText = expense.getCategoryIcon() + " ";
            }
            categoryText += expense.getCategoryName();
            categoryText += String.format(Locale.getDefault(), " - %.0f ₽", expense.getAmount());
            
            tvName.setText(categoryText);
            
            // Показываем тип (доход/расход) вместо "Фиксированная"
            if (expense.isIncome()) {
                tvFixed.setText("Доход");
                tvFixed.setTextColor(itemView.getContext()
                        .getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvFixed.setText("Расход");
                tvFixed.setTextColor(itemView.getContext()
                        .getResources().getColor(android.R.color.holo_red_dark));
            }

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(expense);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(expense);
            });
        }
    }
}