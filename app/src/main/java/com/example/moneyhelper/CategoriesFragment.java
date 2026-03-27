package com.example.moneyhelper;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Category;
import com.example.moneyhelper.service.CategoryService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CategoriesFragment extends BaseFragment {

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

        v.findViewById(R.id.importTextView).setOnClickListener((vv) -> {
            Intent intent = new Intent(getContext(), StatementImportActivity.class);
            startActivity(intent);
        });

        categoryService = new CategoryService(getContext());

        // Локаль берём из настроек приложения
        Locale locale = new Locale(LocaleHelper.getSavedLanguage(requireContext()));
        monthFormat = new SimpleDateFormat("LLLL yyyy", locale);

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
        monthPrevButton.setOnClickListener(v -> navigateMonth(-1));
        monthNextButton.setOnClickListener(v -> navigateMonth(1));

        updateMonthDisplay();
    }

    private void setupRecyclerView() {
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryAdapter = new CategoryAdapter(new ArrayList<>(), new CategoryAdapter.CategoryClickListener() {
            @Override
            public void onCategoryClick(Category category) {
                openCategoryDetails(category);
            }

            @Override
            public void onCategoryLongClick(Category category) {}
        });
        categoriesRecyclerView.setAdapter(categoryAdapter);
    }

    private void openCategoryDetails(Category category) {
        Intent intent = new Intent(getContext(), CategoryDetailsActivity.class);
        intent.putExtra(CategoryDetailsActivity.EXTRA_CATEGORY_ID, category.getUserCategoryId());
        intent.putExtra(CategoryDetailsActivity.EXTRA_CATEGORY_NAME, category.getName());
        intent.putExtra(CategoryDetailsActivity.EXTRA_CATEGORY_ICON, category.getIcon());
        intent.putExtra(CategoryDetailsActivity.EXTRA_MONTH_DATE, selectedMonth.getTime().getTime());
        startActivity(intent);
    }

    private void loadCategories() {
        progressBar.setVisibility(View.VISIBLE);
        categoriesRecyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                Date monthDate = selectedMonth.getTime();
                List<Category> categories = categoryService.getCategoriesForMonth(monthDate);
                CategoryService.CategoryStats stats = categoryService.getCategoryStats(monthDate);

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
                                getString(R.string.error_loading_categories, e.getMessage()),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void showCategories(List<Category> categories, CategoryService.CategoryStats stats) {
        categoriesRecyclerView.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);
        categoryAdapter.updateCategories(categories);
        updateStats(stats);
    }

    private void showEmptyState() {
        categoriesRecyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.VISIBLE);
        emptyTextView.setText(getString(R.string.empty_categories));

        if (statsTextView != null) {
            statsTextView.setVisibility(View.GONE);
        }
    }

    private void updateStats(CategoryService.CategoryStats stats) {
        if (statsTextView != null) {
            statsTextView.setVisibility(View.VISIBLE);
            String monthName = monthFormat.format(selectedMonth.getTime());
            String statsText = getString(R.string.stats_format,
                    monthName,
                    stats.totalCategories,
                    stats.totalExpense,
                    stats.totalBudget
            );
            statsTextView.setText(statsText);
        }
    }

    private void navigateMonth(int direction) {
        selectedMonth.add(Calendar.MONTH, direction);
        updateMonthDisplay();
        loadCategories();
    }

    private void updateMonthDisplay() {
        if (monthTextView != null) {
            monthTextView.setText(monthFormat.format(selectedMonth.getTime()));
        }
    }

    private void showAddCategoryDialog() {
        new Thread(() -> {
            List<Category> categories = categoryService.getAllUserCategories();

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (categories.isEmpty()) {
                        Toast.makeText(getContext(),
                                getString(R.string.no_categories_available),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showAddExpenseDialog(categories);
                });
            }
        }).start();
    }

    private void showAddExpenseDialog(List<Category> categories) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        builder.setTitle(R.string.add_expense_dialog_title);

        android.widget.LinearLayout container = new android.widget.LinearLayout(requireContext());
        container.setOrientation(android.widget.LinearLayout.VERTICAL);
        container.setPadding(50, 40, 50, 10);

        TextView categoryLabel = new TextView(requireContext());
        categoryLabel.setText(getString(R.string.category_label));
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

        TextView amountLabel = new TextView(requireContext());
        amountLabel.setText(getString(R.string.amount_label));
        amountLabel.setTextSize(16);
        amountLabel.setPadding(0, 30, 0, 10);
        container.addView(amountLabel);

        EditText amountEditText = new EditText(requireContext());
        amountEditText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amountEditText.setHint(getString(R.string.amount_hint));
        container.addView(amountEditText);

        builder.setView(container);

        builder.setPositiveButton(getString(R.string.add_button_text), (dialog, which) -> {
            String amountStr = amountEditText.getText().toString().trim();
            if (amountStr.isEmpty()) {
                Toast.makeText(getContext(), getString(R.string.enter_amount_error), Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                double amount = Double.parseDouble(amountStr);
                if (amount <= 0) {
                    Toast.makeText(getContext(), getString(R.string.amount_positive_error), Toast.LENGTH_SHORT).show();
                    return;
                }
                int selectedPosition = categorySpinner.getSelectedItemPosition();
                if (selectedPosition >= 0 && selectedPosition < categories.size()) {
                    addExpense(categories.get(selectedPosition).getUserCategoryId(), amount);
                }
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), getString(R.string.invalid_amount_error), Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton(getString(R.string.cancel_button), null);
        builder.show();
    }

    private void addExpense(long userCategoryId, double amount) {
        new Thread(() -> {
            Date monthDate = selectedMonth.getTime();
            boolean success = categoryService.addExpense(userCategoryId, amount, monthDate);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(getContext(), getString(R.string.expense_added), Toast.LENGTH_SHORT).show();
                        loadCategories();
                    } else {
                        Toast.makeText(getContext(), getString(R.string.error_adding_expense), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void createCategory(String name, String icon, boolean isFixed) {
        new Thread(() -> {
            long categoryId = categoryService.createCategory(name,null, icon, isFixed);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (categoryId > 0) {
                        Toast.makeText(getContext(), getString(R.string.category_created), Toast.LENGTH_SHORT).show();
                        loadCategories();
                    } else {
                        Toast.makeText(getContext(), getString(R.string.error_creating_category), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Обновляем формат месяца при возврате (язык мог измениться)
        Locale locale = new Locale(LocaleHelper.getSavedLanguage(requireContext()));
        monthFormat = new SimpleDateFormat("LLLL yyyy", locale);
        loadCategories();
    }
}