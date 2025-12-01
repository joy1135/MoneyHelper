package com.example.moneyhelper;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Expense;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private TextView balanceTextView;
    private RecyclerView expensesRecyclerView;
    private ExpenseAdapter expenseAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        loadData();
    }

    private void initViews(View view) {
        balanceTextView = view.findViewById(R.id.balanceTextView);
        expensesRecyclerView = view.findViewById(R.id.expensesRecyclerView);
    }

    private void setupRecyclerView() {
        expensesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        expenseAdapter = new ExpenseAdapter(new ArrayList<>());
        expensesRecyclerView.setAdapter(expenseAdapter);
    }

    private void loadData() {
        balanceTextView.setText("Бал. доход-расход: 12000₽");

        List<Expense> expenses = getSampleExpenses();
        expenseAdapter.updateExpenses(expenses);
    }

    private List<Expense> getSampleExpenses() {
        List<Expense> expenses = new ArrayList<>();
        expenses.add(new Expense("ЖКХ", 2000, false));
        expenses.add(new Expense("ЖКХ", 2000, true));
        return expenses;
    }
}