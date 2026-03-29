package com.example.moneyhelper;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Category;

import java.util.List;

public class SimpleCategoryAdapter extends RecyclerView.Adapter<SimpleCategoryAdapter.ViewHolder> {

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
        ImageView ivCategoryIcon;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvName);
            tvFixed = itemView.findViewById(R.id.tvFixed);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            ivCategoryIcon = itemView.findViewById(R.id.ivCategoryIcon);
        }

        public void bind(Category category, CategoryClickListener listener) {
            tvName.setText(category.getName());
            tvFixed.setText(category.isFixed()
                    ? itemView.getContext().getString(R.string.category_fixed)
                    : "");

            String iconName = category.getIcon();
            if (ivCategoryIcon != null && iconName != null && !iconName.isEmpty()) {
                int resId = itemView.getContext().getResources().getIdentifier(
                        iconName, "drawable",
                        itemView.getContext().getPackageName()
                );
                if (resId != 0) {
                    ivCategoryIcon.setImageResource(resId);
                }
            }

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(category);
            });
            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(category);
            });
        }
    }
}
