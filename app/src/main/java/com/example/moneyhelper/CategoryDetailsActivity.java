package com.example.moneyhelper;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Category;
import com.example.moneyhelper.DataTypes.Expense;
import com.example.moneyhelper.service.CategoryService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CategoryDetailsActivity extends BaseActivity {

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

    private ImageView ivCategoryIcon;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_details);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        userCategoryId = getIntent().getLongExtra(EXTRA_CATEGORY_ID, -1);
        String categoryName = getIntent().getStringExtra(EXTRA_CATEGORY_NAME);
        String categoryIcon = getIntent().getStringExtra(EXTRA_CATEGORY_ICON);
        long monthDateLong = getIntent().getLongExtra(EXTRA_MONTH_DATE, -1);

        monthDate = monthDateLong > 0 ? new Date(monthDateLong) : new Date();

        categoryService = new CategoryService(this);

        initViews();

        if (categoryIcon != null && !categoryIcon.isEmpty()) {
            int resId = getResources().getIdentifier(
                    categoryIcon, "drawable", getPackageName());
            if (resId != 0) {
                ivCategoryIcon.setImageResource(resId);
                ivCategoryIcon.setVisibility(View.VISIBLE);
            } else {
                ivCategoryIcon.setVisibility(View.GONE);
            }
        } else {
            ivCategoryIcon.setVisibility(View.GONE);
        }

        setupRecyclerView();
        loadExpenses();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
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
        ivCategoryIcon = findViewById(R.id.ivCategoryIcon);

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
                                getString(R.string.error_loading_transactions, e.getMessage()),
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
        totalAmountTextView.setText(getString(R.string.total_amount_format, totalAmount));
        totalAmountTextView.setVisibility(View.VISIBLE);
    }

    private void showEmptyState() {
        expensesRecyclerView.setVisibility(View.GONE);
        emptyTextView.setVisibility(View.VISIBLE);
        emptyTextView.setText(getString(R.string.empty_transactions_month));
        totalAmountTextView.setVisibility(View.GONE);
    }

    private void showEditExpenseDialog(Expense expense) {
        new Thread(() -> {
            List<Category> categories = categoryService.getAllUserCategories();

            runOnUiThread(() -> {
                if (categories.isEmpty()) {
                    Toast.makeText(this,
                            getString(R.string.no_categories_available),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.edit_expense_title));

                android.widget.LinearLayout container = new android.widget.LinearLayout(this);
                container.setOrientation(android.widget.LinearLayout.VERTICAL);
                container.setPadding(50, 40, 50, 10);

                TextView categoryLabel = new TextView(this);
                categoryLabel.setText(getString(R.string.category_label));
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
                        this, android.R.layout.simple_spinner_item, categoryNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                categorySpinner.setAdapter(adapter);
                categorySpinner.setSelection(selectedIndex);
                container.addView(categorySpinner);

                TextView amountLabel = new TextView(this);
                amountLabel.setText(getString(R.string.amount_label));
                amountLabel.setTextSize(16);
                amountLabel.setPadding(0, 30, 0, 10);
                container.addView(amountLabel);

                EditText amountEditText = new EditText(this);
                amountEditText.setInputType(
                        InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
                amountEditText.setText(String.format(Locale.getDefault(),
                        "%.2f", expense.getAmount()));
                container.addView(amountEditText);

                builder.setView(container);

                builder.setPositiveButton(getString(R.string.save_button), (dialog, which) -> {
                    String amountStr = amountEditText.getText().toString().trim();
                    if (amountStr.isEmpty()) {
                        Toast.makeText(this, getString(R.string.enter_amount_error),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        double amount = Double.parseDouble(amountStr);
                        if (amount <= 0) {
                            Toast.makeText(this, getString(R.string.amount_positive_error),
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                        int selectedPosition = categorySpinner.getSelectedItemPosition();
                        if (selectedPosition >= 0 && selectedPosition < categories.size()) {
                            updateExpense(expense.getId(),
                                    categories.get(selectedPosition).getUserCategoryId(),
                                    amount);
                        }
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, getString(R.string.invalid_amount_error),
                                Toast.LENGTH_SHORT).show();
                    }
                });

                builder.setNegativeButton(getString(R.string.cancel_button), null);
                builder.show();
            });
        }).start();
    }

    private void showDeleteExpenseConfirmation(Expense expense) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_transaction_title))
                .setMessage(getString(R.string.delete_transaction_message, expense.getAmount()))
                .setPositiveButton(getString(R.string.delete_button), (dialog, which) -> {
                    deleteExpense(expense);
                })
                .setNegativeButton(getString(R.string.cancel_button), null)
                .show();
    }

    private void deleteExpense(Expense expense) {
        new Thread(() -> {
            boolean success = categoryService.deleteExpense(expense.getId());
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, getString(R.string.transaction_deleted),
                            Toast.LENGTH_SHORT).show();
                    loadExpenses();
                } else {
                    Toast.makeText(this, getString(R.string.error_deleting_transaction),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void updateExpense(long expenseId, long userCategoryId, double amount) {
        new Thread(() -> {
            boolean success = categoryService.updateExpense(expenseId, userCategoryId, amount);
            runOnUiThread(() -> {
                if (success) {
                    Toast.makeText(this, getString(R.string.expense_updated),
                            Toast.LENGTH_SHORT).show();
                    loadExpenses();
                } else {
                    Toast.makeText(this, getString(R.string.error_updating_expense),
                            Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
}