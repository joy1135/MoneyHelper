package com.example.moneyhelper;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Income;

import java.util.List;

public class IncomeAdapter extends RecyclerView.Adapter<IncomeAdapter.IncomeViewHolder> {

    private List<Income> incomes;

    public IncomeAdapter(List<Income> incomes) {
        this.incomes = incomes;
    }

    @NonNull
    @Override
    public IncomeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_income, parent, false);
        return new IncomeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IncomeViewHolder holder, int position) {
        Income income = incomes.get(position);
        holder.bind(income);
    }

    @Override
    public int getItemCount() {
        return incomes.size();
    }

    public void updateIncomes(List<Income> newIncomes) {
        this.incomes = newIncomes;
        notifyDataSetChanged();
    }

    static class IncomeViewHolder extends RecyclerView.ViewHolder {
        private TextView categoryTextView;
        private TextView amountTextView;

        public IncomeViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryTextView = itemView.findViewById(R.id.categoryTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
        }

        public void bind(Income income) {
            categoryTextView.setText(income.getCategory());
            amountTextView.setText(income.getAmount() + "");
        }
    }
}

