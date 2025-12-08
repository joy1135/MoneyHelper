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
            result.errorMessage = "Недостаточно данных для построения регрессии. Нужно минимум 2 месяца.";
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

            double numerator = n * sumXY - sumX * sumY;
            double denominator = n * sumX2 - sumX * sumX;

            if (Math.abs(denominator) < 0.000001) {
                result.errorMessage = "Деление на ноль при вычислении наклона";
                return result;
            }

            result.slope = numerator / denominator;

            result.intercept = (sumY - result.slope * sumX) / n;

            result.nextPrediction = result.slope * (n + 1) + result.intercept;

            if (result.nextPrediction < 0) {
                result.nextPrediction = 0;
                Log.w(TAG, "Предсказание отрицательное, установлено в 0");
            }

            result.isValid = true;
            Log.d(TAG, String.format("Результат: a=%.2f, b=%.2f, предсказание=%.2f",
                    result.slope, result.intercept, result.nextPrediction));

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при вычислении линейной регрессии", e);
            result.errorMessage = "Ошибка вычисления: " + e.getMessage();
        }

        return result;
    }
    public static double predictNextValue(List<Double> values) {
        if (values == null || values.size() < 2) {
            return -1;
        }

        double[] xValues = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            xValues[i] = i + 1;
        }

        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        int n = values.size();

        for (int i = 0; i < n; i++) {
            double x = xValues[i];
            double y = values.get(i);

            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }

        double a = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double b = (sumY - a * sumX) / n;

        return a * (n + 1) + b;
    }
}
