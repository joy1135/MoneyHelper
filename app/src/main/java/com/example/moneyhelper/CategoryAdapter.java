package com.example.moneyhelper;
import com.example.moneyhelper.DataTypes.Category;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.Locale;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categories;
    private CategoryClickListener clickListener;

    public CategoryAdapter(List<Category> categories, CategoryClickListener clickListener) {
        this.categories = categories;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public CategoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new CategoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CategoryViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.bind(category, clickListener);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateCategories(List<Category> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    /**
     * Интерфейс для обработки кликов
     */
    public interface CategoryClickListener {
        void onCategoryClick(Category category);
        void onCategoryLongClick(Category category);
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView iconTextView;
        private TextView categoryNameTextView;
        private TextView expenseTextView;
        private TextView percentageTextView;
        private TextView budgetTextView;
        private TextView differenceTextView;
        private ProgressBar budgetProgressBar;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            iconTextView = itemView.findViewById(R.id.iconTextView);
            categoryNameTextView = itemView.findViewById(R.id.categoryNameTextView);
            expenseTextView = itemView.findViewById(R.id.expenseTextView);
            percentageTextView = itemView.findViewById(R.id.percentageTextView);
            budgetTextView = itemView.findViewById(R.id.budgetTextView);
            differenceTextView = itemView.findViewById(R.id.differenceTextView);
            budgetProgressBar = itemView.findViewById(R.id.budgetProgressBar);
        }

        public void bind(Category category, CategoryClickListener listener) {
            // Иконка
            if (iconTextView != null) {
                iconTextView.setText(category.getIcon());
            }
            else {

            }

            // Название
            categoryNameTextView.setText(category.getName());

            // Текущие расходы
            expenseTextView.setText(String.format(Locale.getDefault(),
                    "%.0f ₽", category.getCurrentExpense()));

            // Процент от общих расходов
            percentageTextView.setText(category.getPercentage() + "%");

            // Бюджет
            if (budgetTextView != null) {
                if (category.getBudget() > 0) {
                    budgetTextView.setText(String.format(Locale.getDefault(),
                            "Бюджет: %.0f ₽", category.getBudget()));
                    budgetTextView.setVisibility(View.VISIBLE);
                } else {
                    budgetTextView.setVisibility(View.GONE);
                }
            }

            // Разница (перерасход/экономия)
            if (differenceTextView != null && category.getBudget() > 0) {
                double diff = category.getDifference();

                if (diff > 0) {
                    // Перерасход
                    differenceTextView.setText(String.format(Locale.getDefault(),
                            "+%.0f ₽", diff));
                    differenceTextView.setTextColor(Color.parseColor("#F44336")); // Красный
                } else if (diff < 0) {
                    // Экономия
                    differenceTextView.setText(String.format(Locale.getDefault(),
                            "%.0f ₽", diff));
                    differenceTextView.setTextColor(Color.parseColor("#4CAF50")); // Зеленый
                } else {
                    // В рамках бюджета
                    differenceTextView.setText("±0 ₽");
                    differenceTextView.setTextColor(Color.parseColor("#757575")); // Серый
                }

                differenceTextView.setVisibility(View.VISIBLE);
            } else if (differenceTextView != null) {
                differenceTextView.setVisibility(View.GONE);
            }

            // ProgressBar для бюджета
            if (budgetProgressBar != null && category.getBudget() > 0) {
                int progress = category.getBudgetFulfillment();
                budgetProgressBar.setProgress(Math.min(progress, 100));

                // Меняем цвет в зависимости от процента
                if (progress > 100) {
                    budgetProgressBar.setProgressTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor("#F44336"))); // Красный
                } else if (progress > 80) {
                    budgetProgressBar.setProgressTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor("#FF9800"))); // Оранжевый
                } else {
                    budgetProgressBar.setProgressTintList(
                            android.content.res.ColorStateList.valueOf(
                                    Color.parseColor("#4CAF50"))); // Зеленый
                }

                budgetProgressBar.setVisibility(View.VISIBLE);
            } else if (budgetProgressBar != null) {
                budgetProgressBar.setVisibility(View.GONE);
            }

            // Обработка кликов
            if (listener != null) {
                itemView.setOnClickListener(v -> listener.onCategoryClick(category));
                itemView.setOnLongClickListener(v -> {
                    listener.onCategoryLongClick(category);
                    return true;
                });
            }
        }
    }

    public static class SimpleCategoryAdapter extends RecyclerView.Adapter<SimpleCategoryAdapter.ViewHolder> {

        private List<Category> categories;
        private CategoryClickListener clickListener;

        public SimpleCategoryAdapter(List<Category> categories, CategoryClickListener clickListener) {
            this.categories = categories;
            this.clickListener = clickListener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category_simple, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Category category = categories.get(position);
            holder.bind(category, clickListener);
        }

        @Override
        public int getItemCount() {
            return categories.size();
        }

        public void updateCategories(List<Category> newCategories) {
            this.categories = newCategories;
            notifyDataSetChanged();
        }

        public interface CategoryClickListener {
            void onEditClick(Category category);
            void onDeleteClick(Category category);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvFixed;
            ImageButton btnEdit, btnDelete;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tvName);
                tvFixed = itemView.findViewById(R.id.tvFixed);
                btnEdit = itemView.findViewById(R.id.btnEdit);
                btnDelete = itemView.findViewById(R.id.btnDelete);
            }

            public void bind(Category category, CategoryClickListener listener) {
                tvName.setText(category.getName());
                tvFixed.setText(category.isFixed() ? "Фиксированная" : "");

                btnEdit.setOnClickListener(v -> {
                    if (listener != null) listener.onEditClick(category);
                });

                btnDelete.setOnClickListener(v -> {
                    if (listener != null) listener.onDeleteClick(category);
                });
            }
        }
    }
}