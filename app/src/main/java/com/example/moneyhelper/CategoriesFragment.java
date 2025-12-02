package com.example.moneyhelper;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoriesFragment extends Fragment {

    private RecyclerView categoriesRecyclerView;
    private CategoryAdapter categoryAdapter;
    private Button addButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_categories, container, false);
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

        addButton.setOnClickListener(v -> {
            // Добавление новой категории
        });
    }

    private void setupRecyclerView() {
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        categoryAdapter = new CategoryAdapter(new ArrayList<>());
        categoriesRecyclerView.setAdapter(categoryAdapter);
    }

    private void loadCategories() {
        List<Category> categories = getSampleCategories();
        categoryAdapter.updateCategories(categories);
    }

    private List<Category> getSampleCategories() {
        List<Category> categories = new ArrayList<>();
        categories.add(new Category("ЖКХ", 2000, 73, 1499));
        categories.add(new Category("Авто", 4000, 73, 1499));
        return categories;
    }
}