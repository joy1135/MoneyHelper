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
import com.example.moneyhelper.DataTypes.Category;
import com.example.moneyhelper.predict.ExpensePredictor;
import com.example.moneyhelper.predict.PredictionResult;
import com.example.moneyhelper.service.CategoryService;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeFragment extends Fragment {

    private TextView balanceTextView;
    private RecyclerView expensesRecyclerView;
    private CategoryAdapter categoryAdapter;
    private Button predictionButton;
    private Button showButton;
    private ExecutorService executorService;
    private CategoryService categoryService;
    private TextView emptyTextView;
    
    private List<Category> allCategories;
    private List<Category> displayedCategories;
    private static final int INITIAL_COUNT = 3;
    private static final int LOAD_MORE_COUNT = 5;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // emptyTextView больше не кликабелен, так как отображаются прогнозы

        // Инициализация кнопки из layout
        predictionButton = view.findViewById(R.id.predictionButton);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        categoryService = new CategoryService(getContext());
        initViews(view);
        setupRecyclerView();
        setupPredictionButton();
        setupShowButton();
        loadData();

        // Создаем пул потоков
        executorService = Executors.newSingleThreadExecutor();
    }

    private void initViews(View view) {
        balanceTextView = view.findViewById(R.id.balanceTextView);
        expensesRecyclerView = view.findViewById(R.id.expensesRecyclerView);
        emptyTextView = view.findViewById(R.id.emptyTextView);
        showButton = view.findViewById(R.id.showButton);
    }

    private void setupRecyclerView() {
        expensesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Используем CategoryAdapter с item_category.xml
        List<Category> emptyList = new ArrayList<>();
        categoryAdapter = new CategoryAdapter(emptyList, new CategoryAdapter.CategoryClickListener() {
            @Override
            public void onCategoryClick(Category category) {
                // В HomeFragment клик по категории не требуется
            }

            @Override
            public void onCategoryLongClick(Category category) {
                // В HomeFragment длинный клик не требуется
            }
        });
        expensesRecyclerView.setAdapter(categoryAdapter);
    }

    private void loadData() {
        resetEmptyState();
        // Загружаем данные в фоновом потоке
        new Thread(() -> {
            try {
                Date currentMonth = new Date();
                
                // Получаем баланс
                double balance = categoryService.getBalance(currentMonth);
                
                // Получаем категории с текущими расходами за текущий месяц и прогнозами из predict
                allCategories = categoryService.getCategoriesForMonth(currentMonth);
                
                // Фильтруем категории с прогнозами > 0
                List<Category> categoriesWithPredictions = new ArrayList<>();
                for (Category category : allCategories) {
                    if (category.getBudget() > 0) {
                        categoriesWithPredictions.add(category);
                    }
                }
                allCategories = categoriesWithPredictions;
                
                // Сортируем по убыванию текущих расходов
                allCategories.sort((c1, c2) -> Double.compare(c2.getCurrentExpense(), c1.getCurrentExpense()));
                
                // Берем топ-3 для начального отображения
                displayedCategories = new ArrayList<>();
                int count = Math.min(INITIAL_COUNT, allCategories.size());
                for (int i = 0; i < count; i++) {
                    displayedCategories.add(allCategories.get(i));
                }
                
                // Обновляем UI в главном потоке
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        updateBalance(balance);
                        updateCategoriesList();
                        updateShowButton();
                        if (displayedCategories.isEmpty()){
                            showEmptyState();
                        }
                    });
                }
                
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(getContext(),
                                "Ошибка загрузки данных: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void showEmptyState() {
        expensesRecyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.VISIBLE);
        emptyTextView.setText("Нет прогнозов. Создайте прогнозы, нажав кнопку \"Спрогнозировать расходы\"");


    }

    private void resetEmptyState(){
        expensesRecyclerView.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
    }
    
    private void updateBalance(double balance) {
        DecimalFormat df = new DecimalFormat("#,###", new DecimalFormatSymbols(Locale.getDefault()));
        String balanceText = String.format(Locale.getDefault(), "%s₽", df.format(balance));
        balanceTextView.setText(balanceText);
        
        // Меняем цвет в зависимости от баланса
        if (balance >= 0) {
            balanceTextView.setTextColor(getResources().getColor(android.R.color.holo_green_dark, null));
        } else {
            balanceTextView.setTextColor(getResources().getColor(android.R.color.holo_red_dark, null));
        }
    }
    
    private void updateCategoriesList() {
        // Обновляем список категорий
        categoryAdapter.updateCategories(displayedCategories);
    }
    
    private void updateShowButton() {
        if (showButton != null) {
            if (displayedCategories.size() >= allCategories.size()) {
                // Все категории загружены, скрываем кнопку
                showButton.setVisibility(View.GONE);
            } else {
                // Есть еще категории, показываем кнопку
                showButton.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void setupShowButton() {
        if (showButton != null) {
            showButton.setOnClickListener(v -> loadMoreCategories());
        }
    }
    
    private void loadMoreCategories() {
        // Загружаем еще 5 категорий
        int currentCount = displayedCategories.size();
        int nextCount = Math.min(currentCount + LOAD_MORE_COUNT, allCategories.size());
        
        displayedCategories.clear();
        for (int i = 0; i < nextCount; i++) {
            displayedCategories.add(allCategories.get(i));
        }
        
        updateCategoriesList();
        updateShowButton();
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
    public void onResume() {
        super.onResume();
        // Перезагружаем данные при возвращении на экран
        loadData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}