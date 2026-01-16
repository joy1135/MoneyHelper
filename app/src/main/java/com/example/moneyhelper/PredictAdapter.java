package com.example.moneyhelper;
import com.example.moneyhelper.DataTypes.Category;
import com.example.moneyhelper.CategoryAdapter;
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

public class PredictAdapter extends RecyclerView.Adapter<PredictAdapter.PredictViewHolder> {

    private List<Category> categories;
    private CategoryClickListener clickListener;

    public PredictAdapter(List<Category> categories, CategoryClickListener clickListener) {
        this.categories = categories;
        this.clickListener = clickListener;
    }

    @NonNull
    @Override
    public PredictViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new PredictViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PredictViewHolder holder, int position) {
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

    static class PredictViewHolder extends RecyclerView.ViewHolder {

        private TextView categoryNameTextView;
        private TextView expenseTextView;
//        private TextView percentageTextView;
//        private TextView budgetTextView;
//        private TextView differenceTextView;
//        private ProgressBar budgetProgressBar;

        public PredictViewHolder(@NonNull View itemView) {
            super(itemView);

            categoryNameTextView = itemView.findViewById(R.id.categoryTextView);
            expenseTextView = itemView.findViewById(R.id.amountTextView);

        }

        public void bind(Category category, CategoryClickListener listener) {


            // Название
            categoryNameTextView.setText(category.getName());

            // Текущие расходы
            expenseTextView.setText(String.format(Locale.getDefault(),
                    "%.0f ₽", category.getBudget()));


            // Обработка кликов
//            if (listener != null) {
//                itemView.setOnClickListener(v -> listener.onCategoryClick(category));
//                itemView.setOnLongClickListener(v -> {
//                    listener.onCategoryLongClick(category);
//                    return true;
//                });
//            }
        }
    }


}