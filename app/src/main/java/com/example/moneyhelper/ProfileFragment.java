package com.example.moneyhelper;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Category;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private TextView tvMoney;
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

        databaseHelper = DatabaseHelper.getInstance(requireContext());

        rvCategories.setLayoutManager(new LinearLayoutManager(getContext()));
        simpleAdapter = new SimpleCategoryAdapter(new ArrayList<>(), new SimpleCategoryAdapter.CategoryClickListener() {
            @Override
            public void onEditClick(Category category) {
                showEditCategoryDialog(category);
            }

            @Override
            public void onDeleteClick(Category category) {
                new AlertDialog.Builder(getContext())
                        .setTitle("Удалить категорию?")
                        .setMessage(category.getName())
                        .setPositiveButton("Удалить", (d, w) -> {
                            SQLiteDatabase db = databaseHelper.getWritableDatabase();
                            db.delete(
                                    "user_categories",
                                    "id = ?",
                                    new String[]{String.valueOf(category.getUserCategoryId())}
                            );
                            loadCategories();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            }
        });
        rvCategories.setAdapter(simpleAdapter);

        loadMoney();
        loadCategories();

        btnEditMoney.setOnClickListener(v1 -> showEditMoneyDialog());
        btnAddCategory.setOnClickListener(v1 -> showAddCategoryDialog());

        return v;
    }

    private void loadMoney() {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT money FROM users WHERE id = ?",
                new String[]{String.valueOf(userId)}
        );

        if (c.moveToFirst()) {
            tvMoney.setText("Доход: " + c.getInt(0) + " ₽");
        }
        c.close();
    }

    private void showEditMoneyDialog() {
        EditText et = new EditText(getContext());
        et.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        new AlertDialog.Builder(getContext())
                .setTitle("Изменить доход")
                .setView(et)
                .setPositiveButton("Сохранить", (d, w) -> {
                    SQLiteDatabase db = databaseHelper.getWritableDatabase();
                    ContentValues cv = new ContentValues();
                    cv.put("money", Integer.parseInt(et.getText().toString()));
                    db.update("users", cv, "id = ?",
                            new String[]{String.valueOf(userId)});
                    loadMoney();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }


    private void loadCategories() {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor c = db.rawQuery(
                "SELECT id, cat_id, name, fixed FROM user_categories WHERE user_id = ?",
                new String[]{String.valueOf(userId)}
        );

        while (c.moveToNext()) {
            long userCategoryId = c.getLong(0);
            long categoryId = c.getLong(1);
            String name = c.getString(2);
            boolean fixed = c.getInt(3) == 1;

            Category category = new Category(
                    userCategoryId,
                    categoryId,
                    name,
                    "",
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

    private void showCategoryDialog(Category category) {
        View v = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_category, null);

        EditText etName = v.findViewById(R.id.etName);
        Switch switchFixed = v.findViewById(R.id.switchFixed);
        Spinner spinnerCategory = v.findViewById(R.id.spinnerCategory);

        boolean isEdit = category != null;

        List<Category> categoriesList = new ArrayList<>();
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name FROM categories", null);
        while (c.moveToNext()) {
            long id = c.getLong(0);      // id из таблицы categories
            String name = c.getString(1);
            categoriesList.add(new Category(0, id, name, "", false, 0, 0)); // <-- здесь исправлено
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

        new AlertDialog.Builder(getContext())
                .setTitle(isEdit ? "Редактировать категорию" : "Добавить категорию")
                .setView(v)
                .setPositiveButton("Сохранить", (d, w) -> {
                    ContentValues cv = new ContentValues();
                    cv.put("name", etName.getText().toString());
                    cv.put("fixed", switchFixed.isChecked() ? 1 : 0);

                    int position = spinnerCategory.getSelectedItemPosition();
                    long catId = position >= 0 ? categoriesList.get(position).getCatId() : 0;

                    if (isEdit) {
                        cv.put("cat_id", catId);
                        db.update(
                                "user_categories",
                                cv,
                                "id = ?",
                                new String[]{String.valueOf(category.getUserCategoryId())}
                        );
                    } else {
                        cv.put("user_id", userId);
                        cv.put("cat_id", catId);
                        db.insert("user_categories", null, cv);
                    }

                    loadCategories();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

}
