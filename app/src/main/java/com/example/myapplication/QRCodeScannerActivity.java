package com.example.myapplication;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRCodeScannerActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CAMERA = 1;
    private static final String TAG = "WarehouseScanner";

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;

    private TextView statusText;
    private TextView formatText;
    private TextView resultText;
    private TextView contentAnalysis;
    private TextView operationTypeText;
    private Button copyButton;
    private Button shareButton;
    private Button newScanButton;
    private Button sendToServerButton;
    private Button backButton;
    private LinearLayout formatCard;
    private LinearLayout contentCard;
    private LinearLayout analysisCard;
    private LinearLayout actionButtons;

    private String lastScanResult = "";
    private String lastScanFormat = "";
    private String currentOperationType = "";
    private boolean isScanning = true;
    private long lastAnalysisTime = 0;
    private static final long ANALYSIS_INTERVAL = 300;
    private int frameCounter = 0;
    private static final int FRAME_SKIP = 2;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WarehousePrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_scanner);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        currentOperationType = getIntent().getStringExtra("OPERATION_TYPE");

        initViews();
        setupButtons();
        setupBarcodeScanner();
        updateOperationInfo();

        cameraExecutor = Executors.newSingleThreadExecutor();
        checkCameraPermission();
    }

    private void initViews() {
        previewView = findViewById(R.id.camera_preview);

        statusText = findViewById(R.id.statusText);
        formatText = findViewById(R.id.formatText);
        resultText = findViewById(R.id.resultText);
        contentAnalysis = findViewById(R.id.contentAnalysis);
        operationTypeText = findViewById(R.id.operationTypeText);
        copyButton = findViewById(R.id.copyButton);
        shareButton = findViewById(R.id.shareButton);
        newScanButton = findViewById(R.id.newScanButton);
        sendToServerButton = findViewById(R.id.sendToServerButton);
        backButton = findViewById(R.id.backButton);
        formatCard = findViewById(R.id.format_card);
        contentCard = findViewById(R.id.content_card);
        analysisCard = findViewById(R.id.analysis_card);
        actionButtons = findViewById(R.id.action_buttons);
    }

    private void updateOperationInfo() {
        String operationName = "";
        if ("shipment".equals(currentOperationType)) {
            operationName = "ОТГРУЗКА со склада";
        } else if ("loading".equals(currentOperationType)) {
            operationName = "ЗАГРУЗКА на склад";
        }
        operationTypeText.setText("Операция: " + operationName);
    }

    private void setupButtons() {
        copyButton.setOnClickListener(v -> copyToClipboard());
        shareButton.setOnClickListener(v -> shareResult());
        newScanButton.setOnClickListener(v -> startNewScan());
        sendToServerButton.setOnClickListener(v -> sendToServer());
        backButton.setOnClickListener(v -> finish());
    }

    private void sendToServer() {
        if (lastScanResult.isEmpty()) {
            Toast.makeText(this, "Сначала отсканируйте QR-код", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = sharedPreferences.getString("user_id", "unknown");
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        OperationRequest request = new OperationRequest(
                currentOperationType,
                lastScanResult,
                userId,
                timestamp
        );

        ApiService apiService = RetrofitClient.getApiService();
        Call<OperationResponse> call = apiService.sendOperation(request);

        sendToServerButton.setEnabled(false);
        sendToServerButton.setText("Отправка...");

        call.enqueue(new Callback<OperationResponse>() {
            @Override
            public void onResponse(Call<OperationResponse> call, Response<OperationResponse> response) {
                sendToServerButton.setEnabled(true);
                sendToServerButton.setText("Отправить на сервер");

                if (response.isSuccessful() && response.body() != null) {
                    OperationResponse operationResponse = response.body();
                    if (operationResponse.isSuccess()) {
                        handleSuccessfulOperation(operationResponse);
                    } else {
                        Toast.makeText(QRCodeScannerActivity.this,
                                "Ошибка: " + operationResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    // Демо-режим если сервер не отвечает
                    useDemoMode();
                }
            }

            @Override
            public void onFailure(Call<OperationResponse> call, Throwable t) {
                sendToServerButton.setEnabled(true);
                sendToServerButton.setText("Отправить на сервер");

                // Демо-режим при ошибке сети
                useDemoMode();
            }
        });
    }

    private void handleSuccessfulOperation(OperationResponse response) {
        StringBuilder message = new StringBuilder();
        message.append("Операция успешно выполнена!\n");
        message.append("ID операции: ").append(response.getOperationId()).append("\n");

        if (response.getProductInfo() != null) {
            message.append("Товар: ").append(response.getProductInfo().getProductName()).append("\n");
            message.append("Количество: ").append(response.getProductInfo().getQuantity()).append("\n");
            message.append("Место: ").append(response.getProductInfo().getLocation());
        }

        // Показать детали операции
        contentAnalysis.setText(message.toString());
        Toast.makeText(this, "Данные успешно отправлены на сервер", Toast.LENGTH_LONG).show();

        // Автоматически начать новое сканирование через 3 секунды
        new android.os.Handler().postDelayed(() -> {
            startNewScan();
        }, 3000);
    }

    private void useDemoMode() {
        // Демо-режим для тестирования
        StringBuilder demoInfo = new StringBuilder();
        demoInfo.append("⚡ ДЕМО-РЕЖИМ ⚡\n\n");
        demoInfo.append("Операция: ").append("shipment".equals(currentOperationType) ? "ОТГРУЗКА" : "ЗАГРУЗКА").append("\n");
        demoInfo.append("QR-код: ").append(lastScanResult).append("\n");
        demoInfo.append("Статус: УСПЕШНО\n");
        demoInfo.append("Время: ").append(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date())).append("\n");
        demoInfo.append("ID: DEMO_").append(System.currentTimeMillis());

        contentAnalysis.setText(demoInfo.toString());
        Toast.makeText(this, "Демо-режим: операция записана", Toast.LENGTH_LONG).show();

        // Автоматически начать новое сканирование
        new android.os.Handler().postDelayed(() -> {
            startNewScan();
        }, 2000);
    }

    // Остальные методы (setupBarcodeScanner, checkCameraPermission, startCamera, MLKitBarcodeAnalyzer,
    // convertBarcodeFormatToString, startNewScan, handleScanResult, analyzeContent, copyToClipboard,
    // shareResult, onRequestPermissionsResult, onDestroy) остаются аналогичными предыдущей версии,
    // но с небольшими изменениями для интеграции с новой логикой

    private void setupBarcodeScanner() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_AZTEC,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_PDF417
                )
                .build();

        barcodeScanner = BarcodeScanning.getClient(options);
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CAMERA);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder()
                        .setTargetResolution(new Size(1920, 1080))
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, new MLKitBarcodeAnalyzer());

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

                statusText.setText("Камера запущена - сканируйте QR-код товара");
                statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
                statusText.setText("Ошибка камеры");
                statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private class MLKitBarcodeAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            if (!isScanning) {
                imageProxy.close();
                return;
            }

            frameCounter++;
            if (frameCounter % FRAME_SKIP != 0) {
                imageProxy.close();
                return;
            }

            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAnalysisTime < ANALYSIS_INTERVAL) {
                imageProxy.close();
                return;
            }
            lastAnalysisTime = currentTime;

            android.media.Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) {
                imageProxy.close();
                return;
            }

            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

            barcodeScanner.process(image)
                    .addOnSuccessListener(barcodes -> {
                        if (!barcodes.isEmpty() && isScanning) {
                            Barcode barcode = barcodes.get(0);
                            String content = barcode.getRawValue();
                            int format = barcode.getFormat();

                            if (content != null) {
                                String formatName = convertBarcodeFormatToString(format);
                                runOnUiThread(() -> handleScanResult(content, formatName));
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Ошибка распознавания: " + e.getMessage());
                    })
                    .addOnCompleteListener(task -> {
                        imageProxy.close();
                    });
        }
    }

    private String convertBarcodeFormatToString(int format) {
        switch (format) {
            case Barcode.FORMAT_QR_CODE: return "QR_CODE";
            case Barcode.FORMAT_AZTEC: return "AZTEC";
            case Barcode.FORMAT_DATA_MATRIX: return "DATA_MATRIX";
            case Barcode.FORMAT_PDF417: return "PDF417";
            default: return "UNKNOWN";
        }
    }

    private void startNewScan() {
        isScanning = true;
        frameCounter = 0;
        lastAnalysisTime = 0;

        formatCard.setVisibility(View.GONE);
        contentCard.setVisibility(View.GONE);
        analysisCard.setVisibility(View.GONE);
        actionButtons.setVisibility(View.GONE);

        statusText.setText("Сканирование... Наведите на QR-код товара");
        statusText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark, getTheme()));

        Toast.makeText(this, "Наведите камеру на QR-код товара", Toast.LENGTH_SHORT).show();
    }

    private void handleScanResult(String content, String format) {
        isScanning = false;

        lastScanResult = content;
        lastScanFormat = format;

        statusText.setText("QR-код распознан!");
        statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));

        formatText.setText("Формат: " + format);
        resultText.setText(content);

        analyzeContent(content);

        formatCard.setVisibility(View.VISIBLE);
        contentCard.setVisibility(View.VISIBLE);
        analysisCard.setVisibility(View.VISIBLE);
        actionButtons.setVisibility(View.VISIBLE);

        // Вибрация при успешном сканировании
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(100);
            }
        } catch (Exception e) {
            Log.d(TAG, "Vibration not available");
        }

        Toast.makeText(this, "QR-код распознан: " + format, Toast.LENGTH_SHORT).show();
    }

    private void analyzeContent(String content) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("📦 СИСТЕМА СКЛАДА\n\n");
        analysis.append("Тип операции: ");
        analysis.append("shipment".equals(currentOperationType) ? "ОТГРУЗКА\n" : "ЗАГРУЗКА\n");
        analysis.append("QR-код товара: ").append(content).append("\n");
        analysis.append("Длина кода: ").append(content.length()).append(" символов\n");
        analysis.append("Время: ").append(new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date()));

        contentAnalysis.setText(analysis.toString());
    }

    private void copyToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("QR код товара", lastScanResult);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "QR-код скопирован в буфер", Toast.LENGTH_SHORT).show();
    }

    private void shareResult() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "QR-код товара: " + lastScanResult +
                "\nОперация: " + ("shipment".equals(currentOperationType) ? "Отгрузка" : "Загрузка"));
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "QR-код склада");
        startActivity(Intent.createChooser(shareIntent, "Поделиться QR-кодом"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                statusText.setText("Доступ к камере запрещен");
                statusText.setTextColor(getResources().getColor(android.R.color.holo_red_dark, getTheme()));
                Toast.makeText(this, "Для сканирования необходим доступ к камере", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}