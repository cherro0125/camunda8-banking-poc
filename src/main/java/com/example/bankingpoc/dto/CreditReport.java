package com.example.bankingpoc.dto;

public record CreditReport(int score, double monthlyIncome, double existingDebtRatio, int regionRiskScore) {
}
