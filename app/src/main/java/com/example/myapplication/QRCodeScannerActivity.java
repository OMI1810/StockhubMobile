package com.example.myapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.View;
import android.widget.Button;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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
    private TextView operationTypeText;
    private TextView warehouseText;
    private Button buttonHistory;
    private Button buttonNewSession;
    private Button buttonChangeOperation; // Новая кнопка для смены операции
    private RecyclerView recyclerViewProducts;

    private String currentOperationType = "loading";
    private boolean isScanning = true;
    private String lastScanResult = "";

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WarehousePrefs";

    private DatabaseHelper databaseHelper;
    private ProductsAdapter productsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_scanner);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        databaseHelper = new DatabaseHelper(this);

        // Получаем тип операции из интента
        currentOperationType = getIntent().getStringExtra("OPERATION_TYPE");
        if (currentOperationType == null) {
            currentOperationType = "loading"; // значение по умолчанию
        }

        initViews();
        setupRecyclerView();
        setupBarcodeScanner();
        updateUI();

        cameraExecutor = Executors.newSingleThreadExecutor();
        checkCameraPermission();
    }

    private void initViews() {
        previewView = findViewById(R.id.camera_preview);
        statusText = findViewById(R.id.statusText);
        operationTypeText = findViewById(R.id.operationTypeText);
        warehouseText = findViewById(R.id.warehouseText);
        buttonHistory = findViewById(R.id.buttonHistory);
        buttonNewSession = findViewById(R.id.buttonNewSession);
        buttonChangeOperation = findViewById(R.id.buttonChangeOperation); // Новая кнопка
        recyclerViewProducts = findViewById(R.id.recyclerViewProducts);

        buttonHistory.setOnClickListener(v -> openHistory());
        buttonNewSession.setOnClickListener(v -> startNewSession());
        buttonChangeOperation.setOnClickListener(v -> changeOperation()); // Обработчик новой кнопки
    }

    private void setupRecyclerView() {
        recyclerViewProducts.setLayoutManager(new LinearLayoutManager(this));
        productsAdapter = new ProductsAdapter(databaseHelper);
        recyclerViewProducts.setAdapter(productsAdapter);
        loadScannedProducts();
    }

    private void updateUI() {
        String warehouseName = sharedPreferences.getString("warehouse_name", "Не выбран");
        warehouseText.setText("Склад: " + warehouseName);

        // Обновляем отображение типа операции
        String operationName = "shipment".equals(currentOperationType) ? "ОТГРУЗКА" : "ЗАГРУЗКА";
        operationTypeText.setText("Операция: " + operationName);

        // Меняем цвет в зависимости от операции
        int color = "shipment".equals(currentOperationType) ?
                getResources().getColor(android.R.color.holo_green_dark) :
                getResources().getColor(android.R.color.holo_blue_dark);
        operationTypeText.setTextColor(color);
    }

    private void setupBarcodeScanner() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8
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

                imageAnalysis.setAnalyzer(cameraExecutor, new BarcodeAnalyzer());

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

                statusText.setText("Сканирование...");
                statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: " + e.getMessage());
                statusText.setText("Ошибка камеры");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private class BarcodeAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            if (!isScanning) {
                imageProxy.close();
                return;
            }

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

                            if (content != null && !content.equals(lastScanResult)) {
                                String formatName = getBarcodeFormatName(format);
                                runOnUiThread(() -> handleScanResult(content, formatName));
                            }
                        }
                        imageProxy.close();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Ошибка распознавания: " + e.getMessage());
                        imageProxy.close();
                    });
        }
    }

    private String getBarcodeFormatName(int format) {
        switch (format) {
            case Barcode.FORMAT_QR_CODE: return "QR-код";
            case Barcode.FORMAT_DATA_MATRIX: return "Data Matrix";
            case Barcode.FORMAT_CODE_128: return "Code 128";
            case Barcode.FORMAT_CODE_39: return "Code 39";
            case Barcode.FORMAT_EAN_13: return "EAN-13";
            case Barcode.FORMAT_EAN_8: return "EAN-8";
            default: return "Штрих-код";
        }
    }

    private void handleScanResult(String content, String format) {
        lastScanResult = content;
        isScanning = false;

        statusText.setText("✓ Код распознан: " + format);
        statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));

        // Вибрация
        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (vibrator != null) {
                vibrator.vibrate(100);
            }
        } catch (Exception e) {
            Log.d(TAG, "Vibration not available");
        }

        // Получаем информацию о товаре с сервера
        getProductInfoFromServer(content, format);
    }

    private void getProductInfoFromServer(String barcode, String format) {
        String warehouseId = sharedPreferences.getString("warehouse_id", "");
        String userId = sharedPreferences.getString("user_id", "");
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());

        ApiService apiService = RetrofitClient.getApiService();
        Call<ProductInfoResponse> call = apiService.getProductInfo(barcode);

        call.enqueue(new Callback<ProductInfoResponse>() {
            @Override
            public void onResponse(Call<ProductInfoResponse> call, Response<ProductInfoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ProductInfoResponse productResponse = response.body();
                    if (productResponse.isSuccess()) {
                        handleProductInfoSuccess(productResponse, barcode, format, timestamp);
                    } else {
                        useDemoProductInfo(barcode, format, timestamp);
                    }
                } else {
                    useDemoProductInfo(barcode, format, timestamp);
                }
            }

            @Override
            public void onFailure(Call<ProductInfoResponse> call, Throwable t) {
                useDemoProductInfo(barcode, format, timestamp);
            }
        });
    }

    private void handleProductInfoSuccess(ProductInfoResponse response, String barcode, String format, String timestamp) {
        // Сохраняем товар в базу с правильным типом операции
        ScannedProduct product = new ScannedProduct();
        product.setOperationType(currentOperationType);
        product.setCodeType(format);
        product.setCodeContent(barcode);
        product.setTimestamp(timestamp);

        if (response.getProductInfo() != null) {
            ProductInfoResponse.ProductInfo info = response.getProductInfo();
            String productInfo = String.format("%s\n%s\nКол-во: %s\nМесто: %s",
                    info.getProductName(),
                    info.getDescription(),
                    info.getQuantity(),
                    info.getLocation());
            product.setProductInfo(productInfo);
        }

        databaseHelper.addScannedProduct(product);

        // Сохраняем в историю с правильным типом операции
        ScanHistory history = new ScanHistory(
                currentOperationType,
                format,
                barcode,
                timestamp,
                "УСПЕХ"
        );
        databaseHelper.addScanHistory(history);

        Toast.makeText(this, "Товар добавлен: " + barcode, Toast.LENGTH_SHORT).show();
        loadScannedProducts();
        resumeScanning();
    }

    private void useDemoProductInfo(String barcode, String format, String timestamp) {
        // Демо-режим
        ScannedProduct product = new ScannedProduct();
        product.setOperationType(currentOperationType);
        product.setCodeType(format);
        product.setCodeContent(barcode);
        product.setTimestamp(timestamp);
        product.setProductInfo("Демо товар\nШтрих-код: " + barcode + "\nКол-во: 1\nМесто: A-01");

        databaseHelper.addScannedProduct(product);

        // Сохраняем в историю
        ScanHistory history = new ScanHistory(
                currentOperationType,
                format,
                barcode,
                timestamp,
                "ДЕМО"
        );
        databaseHelper.addScanHistory(history);

        Toast.makeText(this, "Демо-товар добавлен: " + barcode, Toast.LENGTH_SHORT).show();
        loadScannedProducts();
        resumeScanning();
    }

    private void resumeScanning() {
        // Автоматически возобновляем сканирование через 1 секунду
        new android.os.Handler().postDelayed(() -> {
            lastScanResult = "";
            isScanning = true;
            statusText.setText("Сканирование...");
            statusText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark, getTheme()));
        }, 1000);
    }

    private void loadScannedProducts() {
        List<ScannedProduct> productList = databaseHelper.getAllScannedProducts();
        productsAdapter.setProductList(productList);

        // Прокручиваем к последнему элементу
        if (!productList.isEmpty()) {
            recyclerViewProducts.smoothScrollToPosition(productList.size() - 1);
        }
    }

    private void openHistory() {
        Intent intent = new Intent(this, HistoryActivity.class);
        startActivity(intent);
    }

    private void startNewSession() {
        // Очищаем текущий список товаров (но оставляем историю)
        databaseHelper.clearAllScannedProducts();
        loadScannedProducts();
        Toast.makeText(this, "Новая сессия начата", Toast.LENGTH_SHORT).show();
    }

    // Новый метод для возврата к выбору операции
    private void changeOperation() {
        Intent intent = new Intent(this, OperationSelectionActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                statusText.setText("Доступ к камере запрещен");
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
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}