package com.example.moneyhelper.scheduler;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.moneyhelper.service.PredictionService;

import java.util.Calendar;

/**
 * Планировщик для автоматического создания прогнозов 1 числа каждого месяца
 */
public class PredictionScheduler extends BroadcastReceiver {

    private static final String TAG = "PredictionScheduler";
    private static final String PREFS_NAME = "PredictionPrefs";
    private static final String PREF_LAST_PREDICTION_MONTH = "last_prediction_month";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Получен сигнал для создания прогнозов");

        // Проверяем, не создавали ли уже прогноз в этом месяце
        if (shouldCreatePrediction(context)) {
            createPredictions(context);
        }
    }

    /**
     * Создает прогнозы
     */
    private void createPredictions(Context context) {
        new Thread(() -> {
            try {
                PredictionService predictionService = new PredictionService(context);
                int predictionsCreated = predictionService.createMonthlyPredictions();

                Log.d(TAG, "Создано прогнозов: " + predictionsCreated);

                // Сохраняем информацию о последнем создании прогноза
                saveLastPredictionDate(context);

            } catch (Exception e) {
                Log.e(TAG, "Ошибка создания прогнозов", e);
            }
        }).start();
    }

    /**
     * Проверяет, нужно ли создавать прогноз
     */
    private boolean shouldCreatePrediction(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastPredictionMonth = prefs.getString(PREF_LAST_PREDICTION_MONTH, "");

        Calendar now = Calendar.getInstance();
        String currentMonth = String.format("%04d-%02d",
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1);

        // Создаем прогноз, если это новый месяц
        return !currentMonth.equals(lastPredictionMonth);
    }

    /**
     * Сохраняет дату последнего создания прогноза
     */
    private void saveLastPredictionDate(Context context) {
        Calendar now = Calendar.getInstance();
        String currentMonth = String.format("%04d-%02d",
                now.get(Calendar.YEAR),
                now.get(Calendar.MONTH) + 1);

        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(PREF_LAST_PREDICTION_MONTH, currentMonth).apply();
    }

    /**
     * Настраивает автоматический запуск каждое 1 число месяца
     */
    public static void schedulePredictions(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, PredictionScheduler.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Вычисляем время первого запуска (1 число следующего месяца в 00:01)
        Calendar firstRun = getNextFirstOfMonth();

        // Устанавливаем повторяющийся alarm
        // Интервал - примерно месяц (30 дней)
        long intervalMillis = AlarmManager.INTERVAL_DAY * 30;

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                firstRun.getTimeInMillis(),
                intervalMillis,
                pendingIntent
        );

        Log.d(TAG, "Планировщик прогнозов настроен. Первый запуск: " + firstRun.getTime());
    }

    /**
     * Отменяет автоматический запуск
     */
    public static void cancelPredictions(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent intent = new Intent(context, PredictionScheduler.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);

        Log.d(TAG, "Планировщик прогнозов отменен");
    }

    /**
     * Вычисляет дату 1 числа следующего месяца в 00:01
     */
    private static Calendar getNextFirstOfMonth() {
        Calendar calendar = Calendar.getInstance();

        // Проверяем, если сегодня 1 число
        if (calendar.get(Calendar.DAY_OF_MONTH) == 1) {
            // Если еще не было запуска сегодня, запускаем сейчас
            calendar.add(Calendar.MINUTE, 1);
        } else {
            // Иначе переходим к 1 числу следующего месяца
            calendar.add(Calendar.MONTH, 1);
            calendar.set(Calendar.DAY_OF_MONTH, 1);
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 1);
            calendar.set(Calendar.SECOND, 0);
        }

        return calendar;
    }

    /**
     * Обработчик перезагрузки устройства
     */
    public static class BootReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                // После перезагрузки восстанавливаем планировщик
                schedulePredictions(context);
            }
        }
    }
}