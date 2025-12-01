package com.example.moneyhelper;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Income;
import com.example.moneyhelper.DataTypes.UpcomingExpense;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private CalendarView calendarView;
    private RecyclerView upcomingExpensesRecyclerView;
    private RecyclerView incomesRecyclerView;
    private UpcomingExpenseAdapter upcomingExpenseAdapter;
    private IncomeAdapter incomeAdapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerViews();
        setupCalendar();
        loadData();
    }

    private void initViews(View view) {
        calendarView = view.findViewById(R.id.calendarView);
        upcomingExpensesRecyclerView = view.findViewById(R.id.upcomingExpensesRecyclerView);
        incomesRecyclerView = view.findViewById(R.id.incomesRecyclerView);
    }

    private void setupRecyclerViews() {
        upcomingExpensesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        upcomingExpenseAdapter = new UpcomingExpenseAdapter(new ArrayList<>());
        upcomingExpensesRecyclerView.setAdapter(upcomingExpenseAdapter);

        incomesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        incomeAdapter = new IncomeAdapter(new ArrayList<>());
        incomesRecyclerView.setAdapter(incomeAdapter);
    }

    private void setupCalendar() {
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            // Обработка выбора даты
        });
    }

    private void loadData() {
        List<UpcomingExpense> expenses = getSampleUpcomingExpenses();
        upcomingExpenseAdapter.updateExpenses(expenses);

        List<Income> incomes = getSampleIncomes();
        incomeAdapter.updateIncomes(incomes);
    }

    private List<UpcomingExpense> getSampleUpcomingExpenses() {
        List<UpcomingExpense> expenses = new ArrayList<>();
        expenses.add(new UpcomingExpense("Подписка Yandex", 400, "Завтра", "✕ Прогноз"));
        expenses.add(new UpcomingExpense("Аренда квартиры", 10000, "28 ноября", ""));
        return expenses;
    }

    private List<Income> getSampleIncomes() {
        List<Income> incomes = new ArrayList<>();
        incomes.add(new Income("Зарплата", 8000));
        incomes.add(new Income("Поступления", 4000));
        return incomes;
    }
}