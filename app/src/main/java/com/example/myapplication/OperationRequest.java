package com.example.myapplication;

public class OperationRequest {
    private String operationType; // "shipment" или "loading"
    private String qrCode;
    private String userId;
    private String timestamp;

    public OperationRequest(String operationType, String qrCode, String userId, String timestamp) {
        this.operationType = operationType;
        this.qrCode = qrCode;
        this.userId = userId;
        this.timestamp = timestamp;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}