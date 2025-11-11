package com.example.myapplication;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class QRCodeScannerActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CAMERA = 1;
    private static final String TAG = "QRCodeScanner";

    private PreviewView previewView;
    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;

    private TextView statusText;
    private TextView formatText;
    private TextView resultText;
    private TextView contentAnalysis;
    private Button copyButton;
    private Button shareButton;
    private Button newScanButton;
    private Button logoutButton;
    private LinearLayout formatCard;
    private LinearLayout contentCard;
    private LinearLayout analysisCard;
    private LinearLayout actionButtons;

    private String lastScanResult = "";
    private String lastScanFormat = "";
    private boolean isScanning = true;
    private long lastAnalysisTime = 0;
    private static final long ANALYSIS_INTERVAL = 300;
    private int frameCounter = 0;
    private static final int FRAME_SKIP = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode_scanner);

        initViews();
        setupButtons();
        setupBarcodeScanner();

        cameraExecutor = Executors.newSingleThreadExecutor();
        checkCameraPermission();
    }

    private void initViews() {
        previewView = findViewById(R.id.camera_preview);

        statusText = findViewById(R.id.statusText);
        formatText = findViewById(R.id.formatText);
        resultText = findViewById(R.id.resultText);
        contentAnalysis = findViewById(R.id.contentAnalysis);
        copyButton = findViewById(R.id.copyButton);
        shareButton = findViewById(R.id.shareButton);
        newScanButton = findViewById(R.id.newScanButton);
        logoutButton = findViewById(R.id.logoutButton); // Добавлена инициализация
        formatCard = findViewById(R.id.format_card);
        contentCard = findViewById(R.id.content_card);
        analysisCard = findViewById(R.id.analysis_card);
        actionButtons = findViewById(R.id.action_buttons);
    }

    private void setupButtons() {
        copyButton.setOnClickListener(v -> copyToClipboard());
        shareButton.setOnClickListener(v -> shareResult());
        newScanButton.setOnClickListener(v -> startNewScan());
        logoutButton.setOnClickListener(v -> logout());
    }

    private void logout() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    // Остальной код остается без изменений...
    private void setupBarcodeScanner() {
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_AZTEC,
                        Barcode.FORMAT_DATA_MATRIX,
                        Barcode.FORMAT_PDF417,
                        Barcode.FORMAT_CODE_128,
                        Barcode.FORMAT_CODE_39,
                        Barcode.FORMAT_CODE_93,
                        Barcode.FORMAT_CODABAR,
                        Barcode.FORMAT_EAN_13,
                        Barcode.FORMAT_EAN_8,
                        Barcode.FORMAT_ITF,
                        Barcode.FORMAT_UPC_A,
                        Barcode.FORMAT_UPC_E
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

                statusText.setText("Камера запущена - сканируйте код");
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
            case Barcode.FORMAT_CODE_128: return "CODE_128";
            case Barcode.FORMAT_CODE_39: return "CODE_39";
            case Barcode.FORMAT_CODE_93: return "CODE_93";
            case Barcode.FORMAT_CODABAR: return "CODABAR";
            case Barcode.FORMAT_EAN_13: return "EAN_13";
            case Barcode.FORMAT_EAN_8: return "EAN_8";
            case Barcode.FORMAT_ITF: return "ITF";
            case Barcode.FORMAT_UPC_A: return "UPC_A";
            case Barcode.FORMAT_UPC_E: return "UPC_E";
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

        statusText.setText("Сканирование... Наведите на код");
        statusText.setTextColor(getResources().getColor(android.R.color.holo_blue_dark, getTheme()));

        Toast.makeText(this, "Наведите камеру на QR-код или штрих-код", Toast.LENGTH_SHORT).show();
    }

    private void handleScanResult(String content, String format) {
        isScanning = false;

        lastScanResult = content;
        lastScanFormat = format;

        statusText.setText("Сканирование успешно!");
        statusText.setTextColor(getResources().getColor(android.R.color.holo_green_dark, getTheme()));

        formatText.setText("Формат: " + format);
        resultText.setText(content);

        analyzeContent(content);

        formatCard.setVisibility(View.VISIBLE);
        contentCard.setVisibility(View.VISIBLE);
        analysisCard.setVisibility(View.VISIBLE);
        actionButtons.setVisibility(View.VISIBLE);

        try {
            android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(android.content.Context.VIBRATOR_SERVICE);
            if (vibrator != null && vibrator.hasVibrator()) {
                vibrator.vibrate(100);
            }
        } catch (Exception e) {
            Log.d(TAG, "Vibration not available");
        }

        Toast.makeText(this, "Код распознан: " + format, Toast.LENGTH_SHORT).show();
    }

    private void analyzeContent(String content) {
        StringBuilder analysis = new StringBuilder();

        if (isURL(content)) {
            analysis.append("🔗 Ссылка URL\n");
            analysis.append("• Можно открыть в браузере\n");
        } else if (isEmail(content)) {
            analysis.append("📧 Email адрес\n");
            analysis.append("• Можно использовать для отправки email\n");
        } else if (isPhoneNumber(content)) {
            analysis.append("📞 Номер телефона\n");
            analysis.append("• Можно использовать для звонков\n");
        } else if (isWifiConfig(content)) {
            analysis.append("📶 Настройки WiFi\n");
            analysis.append("• Параметры подключения к сети\n");
        } else if (isVCard(content)) {
            analysis.append("👤 Контактная информация\n");
            analysis.append("• Данные контакта vCard\n");
        } else if (isGeoLocation(content)) {
            analysis.append("📍 Географические координаты\n");
            analysis.append("• Координаты на карте\n");
        } else {
            analysis.append("📝 Обычный текст\n");
            analysis.append("• Общая информация\n");
        }

        analysis.append("\nДетали:\n");
        analysis.append("• Длина: ").append(content.length()).append(" символов\n");
        analysis.append("• Тип: ").append(detectContentType(content));

        contentAnalysis.setText(analysis.toString());
    }

    private void copyToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Scan result", lastScanResult);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show();
    }

    private void shareResult() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, lastScanResult);
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Результат сканирования - " + lastScanFormat);
        startActivity(Intent.createChooser(shareIntent, "Поделиться результатом"));
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

    private boolean isURL(String text) {
        return text.startsWith("http://") || text.startsWith("https://") ||
                text.startsWith("www.") || text.contains(".com") ||
                text.contains(".org") || text.contains(".net");
    }

    private boolean isEmail(String text) {
        return text.contains("@") && text.contains(".");
    }

    private boolean isPhoneNumber(String text) {
        return text.replaceAll("[^0-9]", "").length() >= 7;
    }

    private boolean isWifiConfig(String text) {
        return text.startsWith("WIFI:") || text.toUpperCase().contains("WIFI");
    }

    private boolean isVCard(String text) {
        return text.startsWith("BEGIN:VCARD") || text.toUpperCase().contains("VCARD");
    }

    private boolean isGeoLocation(String text) {
        return text.startsWith("geo:") || text.contains("maps.google.com") ||
                text.matches(".*[-+]?[0-9]*\\.?[0-9]+,[-+]?[0-9]*\\.?[0-9]+.*");
    }

    private String detectContentType(String text) {
        if (text.length() > 100) return "Длинный текст";
        if (text.contains("\n")) return "Многострочный текст";
        if (text.matches(".*[a-zA-Z].*") && text.matches(".*[0-9].*")) return "Буквенно-цифровой";
        if (text.matches("[0-9]+")) return "Числовой";
        return "Текст";
    }
}