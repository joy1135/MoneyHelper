package com.example.moneyhelper;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Category;
import com.example.moneyhelper.service.CategoryService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CategoriesFragment extends Fragment {

    private RecyclerView categoriesRecyclerView;
    private CategoryAdapter categoryAdapter;
    private Button addButton;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private TextView statsTextView;
    private TextView monthTextView;
    private TextView monthPrevButton;
    private TextView monthNextButton;

    private CategoryService categoryService;
    private SimpleDateFormat monthFormat;
    private Calendar selectedMonth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_categories, container, false);

        // Кнопка импорта
        v.findViewById(R.id.importTextView).setOnClickListener((vv) -> {
            Intent intent = new Intent(getContext(), StatementImportActivity.class);
            startActivity(intent);
        });

        // Инициализация сервиса
        categoryService = new CategoryService(getContext());
        monthFormat = new SimpleDateFormat("LLLL yyyy", new Locale("ru"));
        
        // Инициализация выбранного месяца (текущий месяц)
        selectedMonth = Calendar.getInstance();
        selectedMonth.set(Calendar.DAY_OF_MONTH, 1);
        selectedMonth.set(Calendar.HOUR_OF_DAY, 0);
        selectedMonth.set(Calendar.MINUTE, 0);
        selectedMonth.set(Calendar.SECOND, 0);
        selectedMonth.set(Calendar.MILLISECOND, 0);

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        loadCategories();
    }

    private void initViews(View view) {
        categoriesRecyclerView = view.findViewById(R.id.categoriesRecyclerView);
        addButton = view.findViewById(R.id.addButton);
        progressBar = view.findViewById(R.id.progressBar);
        emptyTextView = view.findViewById(R.id.emptyTextView);
        statsTextView = view.findViewById(R.id.statsTextView);
        monthTextView = view.findViewById(R.id.monthTextView);
        monthPrevButton = view.findViewById(R.id.monthPrevButton);
        monthNextButton = view.findViewById(R.id.monthNextButton);

        addButton.setOnClickListener(v -> showAddCategoryDialog());
        
        // Обработчики для переключения месяцев
        monthPrevButton.setOnClickListener(v -> navigateMonth(-1));
        monthNextButton.setOnClickListener(v -> navigateMonth(1));
        
        // Обновляем отображение месяца
        updateMonthDisplay();
    }

    private void setupRecyclerView() {
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryAdapter = new CategoryAdapter(new ArrayList<>(), new CategoryAdapter.CategoryClickListener() {
            @Override
            public void onCategoryClick(Category category) {
                showCategoryDetails(category);
            }

            @Override
            public void onCategoryLongClick(Category category) {
                showCategoryOptions(category);
            }
        });
        categoriesRecyclerView.setAdapter(categoryAdapter);
    }

    /**
     * Загрузка категорий из БД
     */
    private void loadCategories() {
        // Показываем прогресс
        progressBar.setVisibility(View.VISIBLE);
        categoriesRecyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.GONE);

        // Загружаем в фоновом потоке
        new Thread(() -> {
            try {
                // Получаем категории за выбранный месяц
                Date monthDate = selectedMonth.getTime();
                List<Category> categories = categoryService.getCategoriesForMonth(monthDate);

                // Получаем статистику
                CategoryService.CategoryStats stats =
                        categoryService.getCategoryStats(monthDate);

                // Обновляем UI в главном потоке
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        if (categories.isEmpty()) {
                            showEmptyState();
                        } else {
                            showCategories(categories, stats);
                        }
                    });
                }

            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(),
                                "Ошибка загрузки категорий: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }



    /**
     * Показать список категорий
     */
    private void showCategories(List<Category> categories, CategoryService.CategoryStats stats) {
        categoriesRecyclerView.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);

        // Обновляем адаптер
        categoryAdapter.updateCategories(categories);

        // Обновляем статистику
        updateStats(stats);
    }

    /**
     * Показать пустое состояние
     */
    private void showEmptyState() {
        categoriesRecyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.VISIBLE);
        emptyTextView.setText("Нет категорий.\nДобавьте категорию или импортируйте выписку.");

        if (statsTextView != null) {
            statsTextView.setVisibility(View.GONE);
        }
    }

    /**
     * Обновить статистику
     */
    private void updateStats(CategoryService.CategoryStats stats) {
        if (statsTextView != null) {
            statsTextView.setVisibility(View.VISIBLE);

            String monthName = monthFormat.format(selectedMonth.getTime());
            String statsText = String.format(Locale.getDefault(),
                    "%s\n" +
                            "Категорий: %d | Расходы: %.0f ₽ | Бюджет: %.0f ₽",
                    monthName,
                    stats.totalCategories,
                    stats.totalExpense,
                    stats.totalBudget
            );

            statsTextView.setText(statsText);
        }
    }
    
    /**
     * Переключение месяца
     */
    private void navigateMonth(int direction) {
        selectedMonth.add(Calendar.MONTH, direction);
        updateMonthDisplay();
        loadCategories();
    }
    
    /**
     * Обновить отображение месяца
     */
    private void updateMonthDisplay() {
        if (monthTextView != null) {
            String monthName = monthFormat.format(selectedMonth.getTime());
            monthTextView.setText(monthName);
        }
    }

    /**
     * Диалог добавления расхода
     */
    private void showAddCategoryDialog() {
        // Загружаем категории в фоновом потоке
        new Thread(() -> {
            List<Category> categories = categoryService.getAllUserCategories();
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (categories.isEmpty()) {
                        Toast.makeText(getContext(),
                                "Нет доступных категорий. Сначала создайте категорию.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    showAddExpenseDialog(categories);
                });
            }
        }).start();
    }
    
    /**
     * Показать диалог добавления расхода
     */
    private void showAddExpenseDialog(List<Category> categories) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Добавить расход");
        
        // Создаем layout для диалога
        View dialogView = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, null);
        
        // Создаем контейнер для полей
        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(50, 40, 50, 10);
        
        // Spinner для выбора категории
        TextView categoryLabel = new TextView(requireContext());
        categoryLabel.setText("Категория:");
        categoryLabel.setTextSize(16);
        categoryLabel.setPadding(0, 0, 0, 10);
        container.addView(categoryLabel);
        
        Spinner categorySpinner = new Spinner(requireContext());
        List<String> categoryNames = new ArrayList<>();
        for (Category cat : categories) {
            categoryNames.add(cat.getIcon() + " " + cat.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        container.addView(categorySpinner);
        
        // Поле для суммы
        TextView amountLabel = new TextView(requireContext());
        amountLabel.setText("Сумма (₽):");
        amountLabel.setTextSize(16);
        amountLabel.setPadding(0, 30, 0, 10);
        container.addView(amountLabel);
        
        EditText amountEditText = new EditText(requireContext());
        amountEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountEditText.setHint("Введите сумму");
        container.addView(amountEditText);
        
        builder.setView(container);
        
        builder.setPositiveButton("Добавить", (dialog, which) -> {
            String amountStr = amountEditText.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Введите сумму", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(getContext(), "Сумма должна быть больше 0", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                int selectedPosition = categorySpinner.getSelectedItemPosition();
                if (selectedPosition >= 0 && selectedPosition < categories.size()) {
                    Category selectedCategory = categories.get(selectedPosition);
                    addExpense(selectedCategory.getUserCategoryId(), amount);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Некорректная сумма", Toast.LENGTH_SHORT).show();
            }
        });
        
        builder.setNegativeButton("Отмена", null);
        builder.show();
    }
    
    /**
     * Добавить расход
     */
    private void addExpense(long userCategoryId, double amount) {
        new Thread(() -> {
            // Используем выбранный месяц
            Date monthDate = selectedMonth.getTime();
            boolean success = categoryService.addExpense(userCategoryId, amount, monthDate);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(getContext(),
                                "Расход добавлен",
                                Toast.LENGTH_SHORT).show();
                        loadCategories(); // Перезагружаем список
                    } else {
                        Toast.makeText(getContext(),
                                "Ошибка добавления расхода",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /**
     * Создать новую категорию
     */
    private void createCategory(String name, String icon, boolean isFixed) {
        new Thread(() -> {
            long categoryId = categoryService.createCategory(name, icon, isFixed);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (categoryId > 0) {
                        Toast.makeText(getContext(),
                                "Категория создана",
                                Toast.LENGTH_SHORT).show();
                        loadCategories(); // Перезагружаем список
                    } else {
                        Toast.makeText(getContext(),
                                "Ошибка создания категории",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    /**
     * Показать детали категории
     */
    private void showCategoryDetails(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        String message = String.format(Locale.getDefault(),
                "Расходы: %.2f ₽\n" +
                        "Бюджет: %.2f ₽\n" +
                        "Процент: %d%%\n" +
                        "Разница: %.2f ₽\n" +
                        "Выполнение: %d%%",
                category.getCurrentExpense(),
                category.getBudget(),
                category.getPercentage(),
                category.getDifference(),
                category.getBudgetFulfillment()
        );

        builder.setTitle(category.getIcon() + " " + category.getName())
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show();
    }

    /**
     * Показать опции категории (редактировать/удалить)
     */
    private void showCategoryOptions(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        String[] options = {"Редактировать", "Удалить"};

        builder.setTitle(category.getName())
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Редактировать
                            showEditCategoryDialog(category);
                            break;
                        case 1: // Удалить
                            showDeleteConfirmation(category);
                            break;
                    }
                })
                .show();
    }

    /**
     * Диалог редактирования категории
     */
    private void showEditCategoryDialog(Category category) {
        // TODO: Реализовать редактирование
        Toast.makeText(getContext(),
                "Редактирование будет добавлено",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * Подтверждение удаления категории
     */
    private void showDeleteConfirmation(Category category) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());

        builder.setTitle("Удалить категорию?")
                .setMessage("Категория \"" + category.getName() +
                        "\" и все связанные расходы будут удалены. Это действие нельзя отменить.")
                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteCategory(category);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    /**
     * Удалить категорию
     */
    private void deleteCategory(Category category) {
        new Thread(() -> {
            boolean success = categoryService.deleteCategory(category.getUserCategoryId());

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(getContext(),
                                "Категория удалена",
                                Toast.LENGTH_SHORT).show();
                        loadCategories(); // Перезагружаем список
                    } else {
                        Toast.makeText(getContext(),
                                "Ошибка удаления категории",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Перезагружаем данные при возвращении на экран
        loadCategories();
    }
}