package com.example.moneyhelper;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Category;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends BaseFragment {

    private TextView tvMoney;
    private TextView tvLangRu, tvLangEn;
    private Button btnEditMoney, btnAddCategory;
    private RecyclerView rvCategories;

    private DatabaseHelper databaseHelper;
    private SimpleCategoryAdapter simpleAdapter;

    private long userId = 1;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_profile, container, false);

        tvMoney = v.findViewById(R.id.tvMoney);
        btnEditMoney = v.findViewById(R.id.btnEditMoney);
        btnAddCategory = v.findViewById(R.id.btnEditCategories);
        rvCategories = v.findViewById(R.id.rvCategories);
        tvLangRu = v.findViewById(R.id.tvLangRu);
        tvLangEn = v.findViewById(R.id.tvLangEn);

        databaseHelper = DatabaseHelper.getInstance(requireContext());

        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        simpleAdapter = new SimpleCategoryAdapter(new ArrayList<>(), new SimpleCategoryAdapter.CategoryClickListener() {
            @Override
            public void onEditClick(Category category) {
                showEditCategoryDialog(category);
            }

            @Override
            public void onDeleteClick(Category category) {
                new MaterialAlertDialogBuilder(requireContext())
                        .setTitle(getString(R.string.delete_category_title))
                        .setMessage(category.getName())
                        .setPositiveButton(getString(R.string.delete_button), (d, w) -> {
                            SQLiteDatabase db = databaseHelper.getWritableDatabase();
                            db.delete(
                                    "user_categories",
                                    "id = ?",
                                    new String[]{String.valueOf(category.getUserCategoryId())}
                            );
                            loadCategories();
                        })
                        .setNegativeButton(getString(R.string.cancel_button), null)
                        .show();
            }
        });
        rvCategories.setAdapter(simpleAdapter);

        loadMoney();
        loadCategories();
        setupLanguageSwitcher();

        btnEditMoney.setOnClickListener(v1 -> showEditMoneyDialog());
        btnAddCategory.setOnClickListener(v1 -> showAddCategoryDialog());

        return v;
    }

    private void setupLanguageSwitcher() {
        updateLangHighlight();

        tvLangRu.setOnClickListener(v -> {
            LocaleHelper.setLocale(requireContext(), "ru");
            requireActivity().recreate();
        });

        tvLangEn.setOnClickListener(v -> {
            LocaleHelper.setLocale(requireContext(), "en");
            requireActivity().recreate();
        });
    }

    private void updateLangHighlight() {
        String currentLang = LocaleHelper.getSavedLanguage(requireContext());

        if (currentLang.equals("en")) {
            tvLangEn.setAlpha(1f);
            tvLangRu.setAlpha(0.4f);
        } else {
            tvLangRu.setAlpha(1f);
            tvLangEn.setAlpha(0.4f);
        }
    }

    private void loadMoney() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT money FROM users WHERE id = ?",
                new String[]{String.valueOf(userId)}
        );

        if (c.moveToFirst()) {
            tvMoney.setText(c.getInt(0) + " ₽");
        }
        c.close();
    }

    private void showEditMoneyDialog() {
        EditText et = new EditText(getContext());
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.edit_income_title))
                .setView(et)
                .setPositiveButton(getString(R.string.save_button), (d, w) -> {
                    String amountStr = et.getText().toString().trim();
                    if (amountStr.isEmpty()) {
                        Toast.makeText(getContext(), getString(R.string.enter_amount_hint), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    SQLiteDatabase db = databaseHelper.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    cv.put("money", Integer.parseInt(amountStr));
                    db.update("users", cv, "id = ?",
                            new String[]{String.valueOf(userId)});
                    loadMoney();
                })
                .setNegativeButton(getString(R.string.cancel_button), null)
                .show();
    }

    private void loadCategories() {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT uc.id, uc.cat_id, uc.name, uc.fixed, c.icon, c.name_en " +
                        "FROM user_categories uc " +
                        "LEFT JOIN categories c ON uc.cat_id = c.id " +
                        "WHERE uc.user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        while (c.moveToNext()) {
            long userCategoryId = c.getLong(0);
            long categoryId = c.getLong(1);
            String nameRu = c.getString(2);
            boolean fixed = c.getInt(3) == 1;
            String icon = c.getString(4);
            String nameEn = c.getString(5);

            String name = getLocalizedName(nameRu, nameEn); // ← локализация

            Category category = new Category(
                    userCategoryId,
                    categoryId,
                    name,
                    icon,
                    fixed,
                    0,
                    0
            );

            list.add(category);
        }
        c.close();

        simpleAdapter.updateCategories(list);
    }

    private void showAddCategoryDialog() {
        showCategoryDialog(null);
    }

    private void showEditCategoryDialog(Category category) {
        showCategoryDialog(category);
    }

    private String getLocalizedName(String nameRu, String nameEn) {
        String lang = LocaleHelper.getSavedLanguage(requireContext());
        if (lang.equals("en") && nameEn != null && !nameEn.isEmpty()) {
            return nameEn;
        }
        return nameRu;
    }

    private void showCategoryDialog(Category category) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_category, null);

        EditText etName = v.findViewById(R.id.etName);
        Switch switchFixed = v.findViewById(R.id.switchFixed);
        Spinner spinnerCategory = v.findViewById(R.id.spinnerCategory);

        boolean isEdit = category != null;

        List<Category> categoriesList = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        // Добавили name_en в запрос
        Cursor c = db.rawQuery("SELECT id, name, name_en FROM categories", null);
        while (c.moveToNext()) {
            long id = c.getLong(0);
            String nameRu = c.getString(1);
            String nameEn = c.getString(2);
            String localizedName = getLocalizedName(nameRu, nameEn);
            categoriesList.add(new Category(0, id, localizedName, "", false, 0, 0));
        }
        c.close();

        List<String> displayNames = new ArrayList<>();
        for (Category cat : categoriesList) {
            displayNames.add(cat.getDisplayName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_spinner_item,
                displayNames
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        if (isEdit) {
            etName.setText(category.getName());
            switchFixed.setChecked(category.isFixed());

            for (int i = 0; i < categoriesList.size(); i++) {
                if (categoriesList.get(i).getCatId() == category.getCatId()) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        new MaterialAlertDialogBuilder(getContext())
                .setTitle(isEdit ? getString(R.string.edit_category_title)
                        : getString(R.string.add_expense_dialog_title))
                .setView(v)
                .setPositiveButton(getString(R.string.save_button), (d, w) -> {
                    ContentValues cv = new ContentValues();
                    cv.put("name", etName.getText().toString());
                    cv.put("fixed", switchFixed.isChecked() ? 1 : 0);

                    int position = spinnerCategory.getSelectedItemPosition();
                    long catId = position >= 0 ? categoriesList.get(position).getCatId() : 0;

                    if (isEdit) {
                        cv.put("cat_id", catId);
                        db.update("user_categories", cv, "id = ?",
                                new String[]{String.valueOf(category.getUserCategoryId())});
                    } else {
                        cv.put("user_id", userId);
                        cv.put("cat_id", catId);
                        db.insert("user_categories", null, cv);
                    }

                    loadCategories();
                })
                .setNegativeButton(getString(R.string.cancel_button), null)
                .show();
    }
}