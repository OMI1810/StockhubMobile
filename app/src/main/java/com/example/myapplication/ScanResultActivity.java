package com.example.myapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class ScanResultActivity extends AppCompatActivity {

    private TextView resultText;
    private TextView formatText;
    private TextView contentAnalysis;
    private Button copyButton;
    private Button shareButton;
    private Button openLinkButton;
    private Button newScanButton;

    private String scanResult;
    private String scanFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_result);

        initViews();
        getIntentData();
        displayResult();
        analyzeContent();
        setupButtons();
    }

    private void initViews() {
        resultText = findViewById(R.id.resultText);
        formatText = findViewById(R.id.formatText);
        contentAnalysis = findViewById(R.id.contentAnalysis);
        copyButton = findViewById(R.id.copyButton);
        shareButton = findViewById(R.id.shareButton);
        openLinkButton = findViewById(R.id.openLinkButton);
        newScanButton = findViewById(R.id.newScanButton);
    }

    private void getIntentData() {
        Intent intent = getIntent();
        scanResult = intent.getStringExtra("SCAN_RESULT");
        scanFormat = intent.getStringExtra("SCAN_FORMAT");
    }

    private void displayResult() {
        resultText.setText(scanResult);
        formatText.setText("Format: " + scanFormat);
    }

    private void analyzeContent() {
        StringBuilder analysis = new StringBuilder();

        // Анализ типа содержимого
        if (isURL(scanResult)) {
            analysis.append("🔗 URL Link\n");
            analysis.append("This appears to be a web link\n");
        } else if (isEmail(scanResult)) {
            analysis.append("📧 Email Address\n");
            analysis.append("This appears to be an email\n");
        } else if (isPhoneNumber(scanResult)) {
            analysis.append("📞 Phone Number\n");
            analysis.append("This appears to be a phone number\n");
        } else if (isGeoLocation(scanResult)) {
            analysis.append("📍 Geographic Coordinates\n");
            analysis.append("This appears to be a location\n");
        } else if (isWifiConfig(scanResult)) {
            analysis.append("📶 WiFi Configuration\n");
            analysis.append("This appears to be WiFi network settings\n");
        } else if (isVCard(scanResult)) {
            analysis.append("👤 Contact Information (vCard)\n");
            analysis.append("This appears to be contact details\n");
        } else {
            analysis.append("📝 Plain Text\n");
            analysis.append("This appears to be plain text\n");
        }

        // Дополнительная информация
        analysis.append("\nContent Length: ").append(scanResult.length()).append(" characters");

        contentAnalysis.setText(analysis.toString());
    }

    private void setupButtons() {
        // Копирование в буфер обмена
        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Scan result", scanResult);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        // Поделиться результатом
        shareButton.setOnClickListener(v -> {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, scanResult);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Scan result - " + scanFormat);
            startActivity(Intent.createChooser(shareIntent, "Share scan result"));
        });

        // Открыть ссылку (если это URL)
        openLinkButton.setOnClickListener(v -> {
            if (isURL(scanResult)) {
                try {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(scanResult));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Cannot open this link", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "This is not a valid URL", Toast.LENGTH_SHORT).show();
            }
        });

        // Новое сканирование
        newScanButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, QRCodeScannerActivity.class);
            startActivity(intent);
            finish();
        });
    }

    // Методы для анализа типа содержимого
    private boolean isURL(String text) {
        return text.startsWith("http://") || text.startsWith("https://") ||
                text.startsWith("www.") || text.contains(".com") ||
                text.contains(".org") || text.contains(".net");
    }

    private boolean isEmail(String text) {
        return text.contains("@") && text.contains(".") &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(text).matches();
    }

    private boolean isPhoneNumber(String text) {
        return android.util.Patterns.PHONE.matcher(text).matches();
    }

    private boolean isGeoLocation(String text) {
        return text.startsWith("geo:") || text.startsWith("http://maps.") ||
                text.startsWith("https://maps.") || text.matches(".*[-+]?[0-9]*\\.?[0-9]+,[-+]?[0-9]*\\.?[0-9]+.*");
    }

    private boolean isWifiConfig(String text) {
        return text.startsWith("WIFI:") || text.toUpperCase().contains("WIFI");
    }

    private boolean isVCard(String text) {
        return text.startsWith("BEGIN:VCARD") || text.toUpperCase().contains("VCARD");
    }
}