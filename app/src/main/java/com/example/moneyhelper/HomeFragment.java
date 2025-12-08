package com.example.moneyhelper;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DatabaseHelper;
import com.example.moneyhelper.predict.ExpensePredictor;
import com.example.moneyhelper.predict.PredictionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private TextView balanceTextView;
    private RecyclerView expensesRecyclerView;
    private ExpenseAdapter expenseAdapter;
    private Button predictionButton;
    private ExecutorService executorService;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Инициализация кнопки из layout
        predictionButton = view.findViewById(R.id.predictionButton);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupPredictionButton();
        loadData();

        // Создаем пул потоков
        executorService = Executors.newSingleThreadExecutor();
    }

    private void initViews(View view) {
        balanceTextView = view.findViewById(R.id.balanceTextView);
        expensesRecyclerView = view.findViewById(R.id.expensesRecyclerView);
    }

    private void setupRecyclerView() {
        expensesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Используем тот же тип данных, что ожидает ExpenseAdapter
        List<com.example.moneyhelper.DataTypes.Expense> emptyList = new ArrayList<>();
        expenseAdapter = new ExpenseAdapter(emptyList);
        expensesRecyclerView.setAdapter(expenseAdapter);
    }

    private void loadData() {
        balanceTextView.setText("Бал. доход-расход: 12000₽");

        List<com.example.moneyhelper.DataTypes.Expense> expenses = getSampleExpenses();
        expenseAdapter.updateExpenses(expenses);
    }

    private List<com.example.moneyhelper.DataTypes.Expense> getSampleExpenses() {
        List<com.example.moneyhelper.DataTypes.Expense> expenses = new ArrayList<>();

        // Создаем объекты Expense с правильным пакетом
        expenses.add(new com.example.moneyhelper.DataTypes.Expense("ЖКХ", 2000, false));
        expenses.add(new com.example.moneyhelper.DataTypes.Expense("ЖКХ", 2000, true));
        return expenses;
    }

    private void setupPredictionButton() {
        if (predictionButton == null) {
            return;
        }

        predictionButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Запуск предсказания расходов...", Toast.LENGTH_SHORT).show();

            if (executorService == null || executorService.isShutdown()) {
                executorService = Executors.newSingleThreadExecutor();
            }

            executorService.execute(() -> {
                try {
                    DatabaseHelper dbHelper = DatabaseHelper.getInstance(requireContext());
                    ExpensePredictor predictor = new ExpensePredictor(dbHelper.getWritableDatabase());

                    List<PredictionResult> results = predictor.predictAllCategories();

                    requireActivity().runOnUiThread(() -> {
                        int successfulCount = 0;
                        for (PredictionResult result : results) {
                            if (result.hasEnoughData()) {
                                successfulCount++;
                            }
                        }

                        if (successfulCount > 0) {
                            String message = String.format("Предсказание завершено! Успешно: %d из %d категорий",
                                    successfulCount, results.size());
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();

                            showPredictionResults(results);
                        } else {
                            Toast.makeText(getContext(),
                                    "Недостаточно данных для предсказания. Нужны данные минимум за 2 месяца.",
                                    Toast.LENGTH_LONG).show();
                        }
                    });

                } catch (Exception e) {
                    requireActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(),
                                "Ошибка при выполнении предсказания: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
                    e.printStackTrace();
                }
            });
        });
    }

    private void showPredictionResults(List<PredictionResult> results) {
        StringBuilder message = new StringBuilder("Результаты предсказаний:\n\n");

        for (PredictionResult result : results) {
            if (result.hasEnoughData()) {
                message.append(String.format("• %s: %.2f руб.\n",
                        result.getCategoryName(), result.getPredictedAmount()));
            } else {
                message.append(String.format("• %s: %s\n",
                        result.getCategoryName(), result.getErrorMessage()));
            }
        }

        new android.app.AlertDialog.Builder(requireContext())
                .setTitle("Предсказание расходов на следующий месяц")
                .setMessage(message.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}