package com.example.bankingpoc.dto;

public record DisbursementResult(boolean success, String transactionRef, String failureReason) {

    public static DisbursementResult success(String transactionRef) {
        return new DisbursementResult(true, transactionRef, null);
    }

    public static DisbursementResult failure(String reason) {
        return new DisbursementResult(false, null, reason);
    }
}
