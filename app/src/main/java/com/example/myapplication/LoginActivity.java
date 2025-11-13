package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextEmail, editTextPassword;
    private CheckBox checkBoxRemember;
    private Button buttonLogin;
    private ProgressBar progressBar;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WarehousePrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_FULL_NAME = "full_name";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupPreferences();
        setupLoginButton();

        checkAutoLogin();
    }

    private void initViews() {
        editTextEmail = findViewById(R.id.editTextUsername); // Используем существующее поле, но для email
        editTextPassword = findViewById(R.id.editTextPassword);
        checkBoxRemember = findViewById(R.id.checkBoxRemember);
        buttonLogin = findViewById(R.id.buttonLogin);
        progressBar = findViewById(R.id.progressBar);

        // Устанавливаем hint для email
        editTextEmail.setHint("Email");
        editTextEmail.setInputType(android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
    }

    private void setupPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER, false);
        if (rememberMe) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");

            editTextEmail.setText(savedEmail);
            editTextPassword.setText(savedPassword);
            checkBoxRemember.setChecked(true);
        }
    }

    private void checkAutoLogin() {
        String savedToken = sharedPreferences.getString(KEY_TOKEN, "");
        String savedWarehouse = sharedPreferences.getString("warehouse_id", "");
        if (!savedToken.isEmpty() && !savedWarehouse.isEmpty()) {
            // Переход сразу к сканированию если есть токен и выбран склад
            Intent intent = new Intent(LoginActivity.this, QRCodeScannerActivity.class);
            startActivity(intent);
            finish();
        } else if (!savedToken.isEmpty()) {
            // Переход к сканированию склада если есть токен но нет склада
            Intent intent = new Intent(LoginActivity.this, WarehouseQRActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void setupLoginButton() {
        buttonLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(email)) {
            editTextEmail.setError("Введите email");
            return;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            editTextEmail.setError("Введите корректный email");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Введите пароль");
            return;
        }

        showProgress(true);
        performLogin(email, password);
    }

    private void performLogin(String email, String password) {
        LoginRequest loginRequest = new LoginRequest(email, password);
        ApiService apiService = RetrofitClient.getApiService();

        Call<LoginResponse> call = apiService.login(loginRequest);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                showProgress(false);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess()) {
                        handleSuccessfulLogin(loginResponse, email, password);
                    } else {
                        Toast.makeText(LoginActivity.this,
                                "Ошибка: " + loginResponse.getMessage(), Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this,
                            "Ошибка сервера: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showProgress(false);
                Toast.makeText(LoginActivity.this,
                        "Ошибка сети: " + t.getMessage(), Toast.LENGTH_LONG).show();

                // Для тестирования без сервера - использовать демо-режим
                useDemoMode(email, password);
            }
        });
    }

    private void handleSuccessfulLogin(LoginResponse loginResponse, String email, String password) {
        // Сохранение данных пользователя
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (checkBoxRemember.isChecked()) {
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PASSWORD, password);
            editor.putBoolean(KEY_REMEMBER, true);
        } else {
            editor.remove(KEY_EMAIL);
            editor.remove(KEY_PASSWORD);
            editor.putBoolean(KEY_REMEMBER, false);
        }

        editor.putString(KEY_TOKEN, loginResponse.getToken());
        if (loginResponse.getUser() != null) {
            editor.putString(KEY_USER_ID, loginResponse.getUser().getId());
            editor.putString(KEY_FULL_NAME, loginResponse.getUser().getFullName());
        }
        editor.apply();

        // Переход к сканированию QR-кода склада
        Intent intent = new Intent(LoginActivity.this, WarehouseQRActivity.class);
        startActivity(intent);
        finish();
    }

    private void useDemoMode(String email, String password) {
        // Демо-режим для тестирования без сервера
        if ("admin@warehouse.com".equals(email) && "123456".equals(password)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            if (checkBoxRemember.isChecked()) {
                editor.putString(KEY_EMAIL, email);
                editor.putString(KEY_PASSWORD, password);
                editor.putBoolean(KEY_REMEMBER, true);
            }

            editor.putString(KEY_TOKEN, "demo_token");
            editor.putString(KEY_USER_ID, "user_123");
            editor.putString(KEY_FULL_NAME, "Демо Пользователь");
            editor.apply();

            // Переход к сканированию склада
            Intent intent = new Intent(LoginActivity.this, WarehouseQRActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Неверные учетные данные", Toast.LENGTH_SHORT).show();
        }
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? android.view.View.VISIBLE : android.view.View.GONE);
        buttonLogin.setEnabled(!show);
    }
}