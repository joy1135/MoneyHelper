package com.example.moneyhelper;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Category;
import com.example.moneyhelper.DataTypes.Expense;
import com.example.moneyhelper.service.CategoryService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CategoryDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_CATEGORY_ID = "category_id";
    public static final String EXTRA_CATEGORY_NAME = "category_name";
    public static final String EXTRA_CATEGORY_ICON = "category_icon";
    public static final String EXTRA_MONTH_DATE = "month_date";

    private RecyclerView expensesRecyclerView;
    private ExpenseAdapter expenseAdapter;
    private ProgressBar progressBar;
    private TextView emptyTextView;
    private TextView categoryTitleTextView;
    private TextView totalAmountTextView;

    private CategoryService categoryService;
    private long userCategoryId;
    private Date monthDate;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_details);

        // Кнопка «назад» в ActionBar (стрелка вверху слева)
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Получаем данные из Intent
        userCategoryId = getIntent().getLongExtra(EXTRA_CATEGORY_ID, -1);
        String categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        String categoryIcon = getIntent().getStringExtra(EXTRA_CATEGORY_ICON);
        long monthDateLong = getIntent().getLongExtra(EXTRA_MONTH_DATE, -1);
        
        if (monthDateLong > 0) {
            monthDate = new Date(monthDateLong);
        } else {
            monthDate = new Date();
        }

        categoryService = new CategoryService(this);

        initViews();
        
        // Устанавливаем заголовок категории
        if (categoryIcon != null && !categoryIcon.isEmpty()) {
            categoryTitleTextView.setText(categoryIcon + " " + categoryName);
        } else {
            categoryTitleTextView.setText(categoryName);
        }

        setupRecyclerView();
        loadExpenses();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Обработка нажатия на стрелку «назад»
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initViews() {
        expensesRecyclerView = findViewById(R.id.expensesRecyclerView);
        progressBar = findViewById(R.id.progressBar);
        emptyTextView = findViewById(R.id.emptyTextView);
        categoryTitleTextView = findViewById(R.id.categoryTitleTextView);
        totalAmountTextView = findViewById(R.id.totalAmountTextView);

        // Кнопка назад в верхней панели
        TextView backButtonTextView = findViewById(R.id.backButtonTextView);
        backButtonTextView.setOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        expensesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        expenseAdapter = new ExpenseAdapter(new ArrayList<>(), new ExpenseAdapter.ExpenseClickListener() {
            @Override
            public void onEditClick(Expense expense) {
                showEditExpenseDialog(expense);
            }

            @Override
            public void onDeleteClick(Expense expense) {
                showDeleteExpenseConfirmation(expense);
            }
        });
        expensesRecyclerView.setAdapter(expenseAdapter);
    }

    private void loadExpenses() {
        progressBar.setVisibility(View.VISIBLE);
        expensesRecyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.GONE);

        new Thread(() -> {
            try {
                List<Expense> expenses = categoryService.getExpensesByCategory(userCategoryId, monthDate);

                // Вычисляем общую сумму
                double totalAmount = expenses.stream().mapToDouble(Expense::getAmount).sum();

                if (getApplicationContext() != null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);

                        if (expenses.isEmpty()) {
                            showEmptyState();
                        } else {
                            showExpenses(expenses, totalAmount);
                        }
                    });
                }

            } catch (Exception e) {
                if (getApplicationContext() != null) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(this,
                                "Ошибка загрузки транзакций: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    private void showExpenses(List<Expense> expenses, double totalAmount) {
        expensesRecyclerView.setVisibility(View.VISIBLE);
        emptyTextView.setVisibility(View.GONE);

        expenseAdapter.updateExpenses(expenses);

        // Обновляем общую сумму
        totalAmountTextView.setText(String.format(Locale.getDefault(), 
                "Всего: %.0f ₽", totalAmount));
        totalAmountTextView.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        expensesRecyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.VISIBLE);
        emptyTextView.setText("Нет транзакций за этот месяц.");
        totalAmountTextView.setVisibility(View.GONE);
    }

    private void showEditExpenseDialog(Expense expense) {
        new Thread(() -> {
            List<Category> categories = categoryService.getAllUserCategories();

            runOnUiThread(() -> {
                if (categories.isEmpty()) {
                    Toast.makeText(this,
                            "Нет доступных категорий.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Редактировать расход");

                View dialogView = getLayoutInflater()
                        .inflate(android.R.layout.simple_list_item_1, null);

                android.widget.LinearLayout container =
                        new android.widget.LinearLayout(this);
                container.setOrientation(android.widget.LinearLayout.VERTICAL);
                container.setPadding(50, 40, 50, 10);

                TextView categoryLabel = new TextView(this);
                categoryLabel.setText("Категория:");
                categoryLabel.setTextSize(16);
                categoryLabel.setPadding(0, 0, 0, 10);
                container.addView(categoryLabel);

                Spinner categorySpinner = new Spinner(this);
                List<String> categoryNames = new ArrayList<>();
                int selectedIndex = 0;
                for (int i = 0; i < categories.size(); i++) {
                    Category cat = categories.get(i);
                    categoryNames.add(cat.getIcon() + " " + cat.getName());
                    if (cat.getUserCategoryId() == expense.getUserCategoryId()) {
                        selectedIndex = i;
                    }
                }
                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categoryNames
                );
                adapter.setDropDownViewResource(
                        android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setAdapter(adapter);
                categorySpinner.setSelection(selectedIndex);
                container.addView(categorySpinner);

                TextView amountLabel = new TextView(this);
                amountLabel.setText("Сумма (₽):");
                amountLabel.setTextSize(16);
                amountLabel.setPadding(0, 30, 0, 10);
                container.addView(amountLabel);

                EditText amountEditText = new EditText(this);
                amountEditText.setInputType(
                        InputType.TYPE_CLASS_NUMBER
                                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                );
                amountEditText.setText(String.format(
                        Locale.getDefault(),
                        "%.2f",
                        expense.getAmount()
                ));
                container.addView(amountEditText);

                builder.setView(container);

                builder.setPositiveButton("Сохранить", (dialog, which) -> {
                    String amountStr = amountEditText.getText()
                            .toString()
                            .trim();
                    if (amountStr.isEmpty()) {
                        Toast.makeText(this,
                                "Введите сумму",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        double amount = Double.parseDouble(amountStr);
                        if (amount <= 0) {
                            Toast.makeText(this,
                                    "Сумма должна быть больше 0",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        int selectedPosition =
                                categorySpinner.getSelectedItemPosition();
                        if (selectedPosition >= 0
                                && selectedPosition < categories.size()) {
                            Category selectedCategory =
                                    categories.get(selectedPosition);
                            updateExpense(expense.getId(),
                                    selectedCategory.getUserCategoryId(),
                                    amount);
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this,
                                "Некорректная сумма",
                                Toast.LENGTH_SHORT).show();
                    }
                });

                builder.setNegativeButton("Отмена", null);
                builder.show();
            });
        }).start();
    }

    private void showDeleteExpenseConfirmation(Expense expense) {
        new android.app.AlertDialog.Builder(this)
                .setTitle("Удалить транзакцию?")
                .setMessage(String.format(Locale.getDefault(),
                        "Транзакция на сумму %.0f ₽ будет удалена. Это действие нельзя отменить.",
                        expense.getAmount()))
                .setPositiveButton("Удалить", (dialog, which) -> {
                    deleteExpense(expense);
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void deleteExpense(Expense expense) {
        new Thread(() -> {
            boolean success = categoryService.deleteExpense(expense.getId());

            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this,
                            "Транзакция удалена",
                            Toast.LENGTH_SHORT).show();
                    loadExpenses(); // Перезагружаем список
                } else {
                    Toast.makeText(this,
                            "Ошибка удаления транзакции",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void updateExpense(long expenseId, long userCategoryId, double amount) {
        new Thread(() -> {
            boolean success = categoryService.updateExpense(
                    expenseId,
                    userCategoryId,
                    amount
            );

            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this,
                            "Расход обновлен",
                            Toast.LENGTH_SHORT).show();
                    loadExpenses();
                } else {
                    Toast.makeText(this,
                            "Ошибка обновления расхода",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}
