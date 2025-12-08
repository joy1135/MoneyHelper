package com.example.moneyhelper.predict;

public class PredictionResult {
    private int userCatId;
    private String categoryName;
    private double predictedAmount;
    private boolean hasEnoughData;
    private String errorMessage;

    public PredictionResult(int userCatId, String categoryName, double predictedAmount, boolean hasEnoughData) {
        this.userCatId = userCatId;
        this.categoryName = categoryName;
        this.predictedAmount = predictedAmount;
        this.hasEnoughData = hasEnoughData;
    }

    public PredictionResult(int userCatId, String categoryName, String errorMessage) {
        this.userCatId = userCatId;
        this.categoryName = categoryName;
        this.errorMessage = errorMessage;
        this.hasEnoughData = false;
    }

    // Getters and setters
    public int getUserCatId() { return userCatId; }
    public String getCategoryName() { return categoryName; }
    public double getPredictedAmount() { return predictedAmount; }
    public boolean hasEnoughData() { return hasEnoughData; }
    public String getErrorMessage() { return errorMessage; }

    public void setPredictedAmount(double predictedAmount) { this.predictedAmount = predictedAmount; }
    public void setHasEnoughData(boolean hasEnoughData) { this.hasEnoughData = hasEnoughData; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
