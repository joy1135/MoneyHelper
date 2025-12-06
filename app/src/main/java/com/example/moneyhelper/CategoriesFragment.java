package com.example.moneyhelper;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
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

    private CategoryService categoryService;
    private SimpleDateFormat monthFormat;

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

        addButton.setOnClickListener(v -> showAddCategoryDialog());
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
                // Получаем категории за текущий месяц
                List<Category> categories = categoryService.getAllCategories();

                // Получаем статистику
                CategoryService.CategoryStats stats =
                        categoryService.getCategoryStats(new Date());

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

            String monthName = monthFormat.format(new Date());
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
     * Диалог добавления категории
     */
    private void showAddCategoryDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
//
//        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_category, null);
//
//        // TODO: Добавить поля для ввода имени и выбора иконки
//
//        builder.setView(dialogView)
//                .setTitle("Новая категория")
//                .setPositiveButton("Добавить", (dialog, which) -> {
//                    // TODO: Получить данные из полей и создать категорию
//                    String name = "Новая категория";
//                    String icon = "📦";
//
//                    createCategory(name, icon, false);
//                })
//                .setNegativeButton("Отмена", null)
//                .show();
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