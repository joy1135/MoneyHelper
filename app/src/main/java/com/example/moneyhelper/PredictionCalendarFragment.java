package com.example.moneyhelper;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CalendarView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.moneyhelper.DataTypes.Category;
import com.example.moneyhelper.DataTypes.Expense;
import com.example.moneyhelper.DataTypes.UpcomingExpense;
import com.example.moneyhelper.service.CategoryService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PredictionCalendarFragment extends BaseFragment {

    private CalendarView calendarView;
    private RecyclerView rvDailyPredictions;
    private TextView tvEmptyDay;
    private UpcomingExpenseAdapter adapter;
    private CategoryService categoryService;
    private final SimpleDateFormat displayDateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("ru"));

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_prediction_calendar, container, false);

        calendarView = view.findViewById(R.id.calendarView);
        rvDailyPredictions = view.findViewById(R.id.rvDailyPredictions);
        tvEmptyDay = view.findViewById(R.id.tvEmptyDay);

        categoryService = new CategoryService(requireContext());

        setupRecyclerView();

        // Загружаем данные для сегодняшнего дня по умолчанию
        loadDataForDate(new Date(calendarView.getDate()));

        // Слушатель выбора даты в календаре
        calendarView.setOnDateChangeListener((view1, year, month, dayOfMonth) -> {
            Calendar calendar = Calendar.getInstance();
            calendar.set(year, month, dayOfMonth);
            loadDataForDate(calendar.getTime());
        });

        return view;
    }

    private void setupRecyclerView() {
        adapter = new UpcomingExpenseAdapter(new ArrayList<>());
        rvDailyPredictions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvDailyPredictions.setAdapter(adapter);
    }

    private void loadDataForDate(Date selectedDate) {
        List<UpcomingExpense> combinedList = new ArrayList<>();
        String formattedDate = displayDateFormat.format(selectedDate);

        // 1. Получаем ФАКТИЧЕСКИЕ расходы за выбранный месяц и фильтруем по выбранному дню
        List<Expense> monthlyExpenses = categoryService.getExpensesForMonth(selectedDate);
        for (Expense expense : monthlyExpenses) {
            if (isSameDay(expense.getDate(), selectedDate)) {
                combinedList.add(new UpcomingExpense(
                        expense.getCategoryIcon() + " " + expense.getCategoryName(),
                        (int) expense.getAmount(),
                        formattedDate,
                        "✓ Фактический расход"
                ));
            }
        }

        // 2. Получаем ПРОГНОЗЫ и рассчитываем "ежедневную норму" (Daily Pace)
        List<Category> predictions = categoryService.getCategoriesWithPredictions();
        int daysInMonth = getDaysInMonth(selectedDate);

        for (Category category : predictions) {
            if (category.getBudget() > 0) {
                // Размазываем прогноз на количество дней в месяце
                int dailyPace = (int) (category.getBudget() / daysInMonth);
                if (dailyPace > 0) {
                    combinedList.add(new UpcomingExpense(
                            category.getIcon() + " " + category.getName(),
                            dailyPace,
                            formattedDate,
                            "✕ Прогноз (норма в день)"
                    ));
                }
            }
        }

        // 3. Обновляем UI
        adapter.updateExpenses(combinedList);

        if (combinedList.isEmpty()) {
            rvDailyPredictions.setVisibility(View.GONE);
            tvEmptyDay.setVisibility(View.VISIBLE);
        } else {
            rvDailyPredictions.setVisibility(View.VISIBLE);
            tvEmptyDay.setVisibility(View.GONE);
        }
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance();
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private int getDaysInMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        return cal.getActualMaximum(Calendar.DAY_OF_MONTH);
    }
}