package com.example.moneyhelper;
import com.example.moneyhelper.DataTypes.Category;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder> {

    private List<Category> categories;

    public CategoryAdapter(List<Category> categories) {
        this.categories = categories;
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
        holder.bind(category);
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void updateCategories(List<Category> newCategories) {
        this.categories = newCategories;
        notifyDataSetChanged();
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        private TextView categoryNameTextView;
        private TextView expenseTextView;
        private TextView percentageTextView;
        private TextView amountTextView;

        public CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryNameTextView = itemView.findViewById(R.id.categoryNameTextView);
            expenseTextView = itemView.findViewById(R.id.expenseTextView);
            percentageTextView = itemView.findViewById(R.id.percentageTextView);

            amountTextView = itemView.findViewById(R.id.amountTextView);
        }

        public void bind(Category category) {
            categoryNameTextView.setText(category.getName());
            expenseTextView.setText(category.getExpense() + "");
            percentageTextView.setText(category.getPercentage() + "%");

            amountTextView.setText(category.getBudget() + " ₽");
        }
    }
}