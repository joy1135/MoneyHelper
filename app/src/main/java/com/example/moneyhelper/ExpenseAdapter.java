package com.example.moneyhelper;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
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

        private ImageView ivCategoryIcon;
        public ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvFixed = itemView.findViewById(R.id.tvFixed);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
        }

        public void bind(Expense expense, ExpenseClickListener listener) {
            // Убираем иконку из текста — только название категории и сумма
            String categoryText = expense.getCategoryName();
            categoryText += String.format(Locale.getDefault(), " - %.0f ₽", expense.getAmount());
            tvName.setText(categoryText);

            // Загружаем иконку в ImageView
            String iconName = expense.getCategoryIcon();
            if (ivCategoryIcon != null && iconName != null && !iconName.isEmpty()) {
                int resId = itemView.getContext().getResources().getIdentifier(
                        iconName, "drawable",
                        itemView.getContext().getPackageName()
                );
                if (resId != 0) {
                    ivCategoryIcon.setImageResource(resId);
                }
            }

            // тип дохода/расхода
            if (expense.isIncome()) {
                tvFixed.setText(itemView.getContext().getString(R.string.expense_type_income));
                tvFixed.setTextColor(itemView.getContext()
                        .getResources().getColor(android.R.color.holo_green_dark));
            } else {
                tvFixed.setText(itemView.getContext().getString(R.string.expense_type_expense));
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