package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class OperationSelectionActivity extends AppCompatActivity {

    private Button buttonShipment, buttonLoading, buttonLogout;
    private TextView textViewWelcome;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WarehousePrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_operation_selection);

        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initViews();
        setupUserInfo();
        setupButtons();
    }

    private void initViews() {
        buttonShipment = findViewById(R.id.buttonShipment);
        buttonLoading = findViewById(R.id.buttonLoading);
        buttonLogout = findViewById(R.id.buttonLogout);
        textViewWelcome = findViewById(R.id.textViewWelcome);
    }

    private void setupUserInfo() {
        String fullName = sharedPreferences.getString("full_name", "Пользователь");
        String warehouseName = sharedPreferences.getString("warehouse_name", "Не выбран");
        textViewWelcome.setText("Добро пожаловать, " + fullName + "!\nСклад: " + warehouseName);
    }

    private void setupButtons() {
        buttonShipment.setOnClickListener(v -> startQRScanning("shipment"));
        buttonLoading.setOnClickListener(v -> startQRScanning("loading"));
        buttonLogout.setOnClickListener(v -> logout());
    }

    private void startQRScanning(String operationType) {
        Intent intent = new Intent(OperationSelectionActivity.this, QRCodeScannerActivity.class);
        intent.putExtra("OPERATION_TYPE", operationType);
        startActivity(intent);
        finish();
    }

    private void logout() {
        // Очистка сохраненных данных
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        // Возврат к экрану логина
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        // Запрещаем возврат к сканированию склада, только выход
        Toast.makeText(this, "Для выхода используйте кнопку выхода", Toast.LENGTH_SHORT).show();
    }
}