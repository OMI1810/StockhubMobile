package com.example.myapplication;

public class ScanHistory {
    private int id;
    private String operationType;
    private String codeType;
    private String codeContent;
    private String timestamp;
    private String status;

    // Конструкторы
    public ScanHistory() {}

    public ScanHistory(String operationType, String codeType, String codeContent, String timestamp, String status) {
        this.operationType = operationType;
        this.codeType = codeType;
        this.codeContent = codeContent;
        this.timestamp = timestamp;
        this.status = status;
    }

    // Геттеры и сеттеры
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public String getCodeType() { return codeType; }
    public void setCodeType(String codeType) { this.codeType = codeType; }

    public String getCodeContent() { return codeContent; }
    public void setCodeContent(String codeContent) { this.codeContent = codeContent; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}