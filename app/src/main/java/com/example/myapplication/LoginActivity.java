package com.example.myapplication;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextPassword;
    private CheckBox checkBoxRemember;
    private Button buttonLogin;

    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember";

    // Тестовые учетные данные
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initViews();
        setupPreferences();
        setupLoginButton();
    }

    private void initViews() {
        // Используем правильные ID из activity_login.xml
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextPassword = findViewById(R.id.editTextPassword);
        checkBoxRemember = findViewById(R.id.checkBoxRemember);
        buttonLogin = findViewById(R.id.buttonLogin);
    }

    private void setupPreferences() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Восстановление сохраненных данных
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER, false);
        if (rememberMe) {
            String savedUsername = sharedPreferences.getString(KEY_USERNAME, "");
            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");

            editTextUsername.setText(savedUsername);
            editTextPassword.setText(savedPassword);
            checkBoxRemember.setChecked(true);
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

        if (isValidCredentials(username, password)) {
            // Сохранение данных если выбрано "Запомнить меня"
            if (checkBoxRemember.isChecked()) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(KEY_USERNAME, username);
                editor.putString(KEY_PASSWORD, password);
                editor.putBoolean(KEY_REMEMBER, true);
                editor.apply();
            } else {
                // Очистка сохраненных данных
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.clear();
                editor.apply();
            }

            // Переход к сканеру
            Intent intent = new Intent(LoginActivity.this, QRCodeScannerActivity.class);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Неверные учетные данные", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isValidCredentials(String username, String password) {
        return DEFAULT_USERNAME.equals(username) && DEFAULT_PASSWORD.equals(password);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Очистка если не выбрано "Запомнить меня"
        if (!checkBoxRemember.isChecked()) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
        }
    }
}