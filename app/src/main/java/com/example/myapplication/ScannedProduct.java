package com.example.myapplication;

public class ScannedProduct {
    private int id;
    private String operationType;
    private String codeType;
    private String codeContent;
    private String productInfo;
    private String timestamp;
    private boolean sentToServer;

    // Конструкторы
    public ScannedProduct() {}

    public ScannedProduct(String operationType, String codeType, String codeContent, String timestamp) {
        this.operationType = operationType;
        this.codeType = codeType;
        this.codeContent = codeContent;
        this.timestamp = timestamp;
        this.productInfo = "";
        this.sentToServer = false;
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

    public String getProductInfo() { return productInfo; }
    public void setProductInfo(String productInfo) { this.productInfo = productInfo; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public boolean isSentToServer() { return sentToServer; }
    public void setSentToServer(boolean sentToServer) { this.sentToServer = sentToServer; }
}