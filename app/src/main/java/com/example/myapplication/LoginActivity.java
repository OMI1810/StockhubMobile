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

    private EditText editTextUsername, editTextPassword;
    private CheckBox checkBoxRemember;
    private Button buttonLogin;
    private ProgressBar progressBar;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "WarehousePrefs";
    private static final String KEY_USERNAME = "username";
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

        // Автоматический вход если есть сохраненный токен
        checkAutoLogin();
    }

    private void initViews() {
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        checkBoxRemember = findViewById(R.id.checkBoxRemember);
        buttonLogin = findViewById(R.id.buttonLogin);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER, false);
        if (rememberMe) {
            String savedUsername = sharedPreferences.getString(KEY_USERNAME, "");
            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");

            editTextUsername.setText(savedUsername);
            editTextPassword.setText(savedPassword);
            checkBoxRemember.setChecked(true);
        }
    }

    private void checkAutoLogin() {
        String savedToken = sharedPreferences.getString(KEY_TOKEN, "");
        if (!savedToken.isEmpty()) {
            // Переход сразу к выбору операции если есть валидный токен
            Intent intent = new Intent(LoginActivity.this, OperationSelectionActivity.class);
            startActivity(intent);
            finish();
        }
    }

    private void setupLoginButton() {
        buttonLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username)) {
            editTextUsername.setError("Введите имя пользователя");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            editTextPassword.setError("Введите пароль");
            return;
        }

        showProgress(true);
        performLogin(username, password);
    }

    private void performLogin(String username, String password) {
        LoginRequest loginRequest = new LoginRequest(username, password);
        ApiService apiService = RetrofitClient.getApiService();

        Call<LoginResponse> call = apiService.login(loginRequest);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                showProgress(false);

                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();

                    if (loginResponse.isSuccess()) {
                        handleSuccessfulLogin(loginResponse, username, password);
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
                useDemoMode(username, password);
            }
        });
    }

    private void handleSuccessfulLogin(LoginResponse loginResponse, String username, String password) {
        // Сохранение данных пользователя
        SharedPreferences.Editor editor = sharedPreferences.edit();

        if (checkBoxRemember.isChecked()) {
            editor.putString(KEY_USERNAME, username);
            editor.putString(KEY_PASSWORD, password);
            editor.putBoolean(KEY_REMEMBER, true);
        } else {
            editor.remove(KEY_USERNAME);
            editor.remove(KEY_PASSWORD);
            editor.putBoolean(KEY_REMEMBER, false);
        }

        editor.putString(KEY_TOKEN, loginResponse.getToken());
        if (loginResponse.getUser() != null) {
            editor.putString(KEY_USER_ID, loginResponse.getUser().getId());
            editor.putString(KEY_FULL_NAME, loginResponse.getUser().getFullName());
        }
        editor.apply();

        // Переход к выбору операции
        Intent intent = new Intent(LoginActivity.this, OperationSelectionActivity.class);
        startActivity(intent);
        finish();
    }

    private void useDemoMode(String username, String password) {
        // Демо-режим для тестирования без сервера
        if ("admin".equals(username) && "123456".equals(password)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            if (checkBoxRemember.isChecked()) {
                editor.putString(KEY_USERNAME, username);
                editor.putString(KEY_PASSWORD, password);
                editor.putBoolean(KEY_REMEMBER, true);
            }

            editor.putString(KEY_TOKEN, "demo_token");
            editor.putString(KEY_USER_ID, "user_123");
            editor.putString(KEY_FULL_NAME, "Демо Пользователь");
            editor.apply();

            Intent intent = new Intent(LoginActivity.this, OperationSelectionActivity.class);
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