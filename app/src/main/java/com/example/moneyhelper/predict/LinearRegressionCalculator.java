package com.example.moneyhelper.predict;

import android.util.Log;
import java.util.List;

public class LinearRegressionCalculator {
    private static final String TAG = "LinearRegression";

    public static class RegressionResult {
        public double slope;
        public double intercept;
        public double nextPrediction;
        public boolean isValid;
        public String errorMessage;

        public RegressionResult() {
            isValid = false;
        }
    }
    public static RegressionResult calculateRegression(List<Double> xValues, List<Double> yValues) {
        RegressionResult result = new RegressionResult();

        if (xValues == null || yValues == null || xValues.size() != yValues.size()) {
            result.errorMessage = "Некорректные входные данные";
            return result;
        }

        int n = xValues.size();
        if (n < 2) {
            result.errorMessage = "Недостаточно данных для построения регрессии. Нужно минимум 2 точки.";
            return result;
        }

        try {
            double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;

            for (int i = 0; i < n; i++) {
                double x = xValues.get(i);
                double y = yValues.get(i);
                sumX += x;
                sumY += y;
                sumXY += x * y;
                sumX2 += x * x;
            }

            Log.d(TAG, String.format("n=%d, sumX=%.2f, sumY=%.2f, sumXY=%.2f, sumX2=%.2f",
                    n, sumX, sumY, sumXY, sumX2));

            double denominator = n * sumX2 - sumX * sumX;
            if (Math.abs(denominator) < 1e-10) {
                result.errorMessage = "Деление на ноль при вычислении наклона";
                return result;
            }

            result.slope = (n * sumXY - sumX * sumY) / denominator;
            result.intercept = (sumY - result.slope * sumX) / n;

            double lastX = xValues.get(n - 1);
            double step = (n > 1) ? (xValues.get(n - 1) - xValues.get(n - 2)) : 1.0;
            result.nextPrediction = result.slope * (lastX + step) + result.intercept;

            if (result.nextPrediction < 0) {
                result.nextPrediction = 0;
                Log.w(TAG, "Предсказание отрицательное, установлено в 0");
            }

            result.isValid = true;

            Log.d(TAG, String.format("Результат регрессии: a=%.2f, b=%.2f, прогноз=%.2f",
                    result.slope, result.intercept, result.nextPrediction));

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при вычислении линейной регрессии", e);
            result.errorMessage = "Ошибка вычисления: " + e.getMessage();
        }

        return result;
    }

    public static double predictNextValue(List<Double> yValues) {
        if (yValues == null || yValues.size() < 2) return -1;

        List<Double> xValues = new java.util.ArrayList<>();
        for (int i = 0; i < yValues.size(); i++) {
            xValues.add((double)(i + 1));
        }

        RegressionResult res = calculateRegression(xValues, yValues);
        return res.isValid ? res.nextPrediction : -1;
    }
}
