package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private DatabaseHelper databaseHelper;
    private TextView emptyText;
    private LinearLayout summaryLayout;
    private TextView summaryToday;
    private TextView summaryTotal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        databaseHelper = new DatabaseHelper(this);

        initViews();
        setupRecyclerView();
        loadHistory();
        updateSummary();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewHistory);
        emptyText = findViewById(R.id.textEmptyHistory);
        summaryLayout = findViewById(R.id.layoutSummary);
        summaryToday = findViewById(R.id.textSummaryToday);
        summaryTotal = findViewById(R.id.textSummaryTotal);

        Button buttonBack = findViewById(R.id.buttonBack);
        Button buttonClear = findViewById(R.id.buttonClearHistory);

        buttonBack.setOnClickListener(v -> finish());
        buttonClear.setOnClickListener(v -> clearHistory());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void loadHistory() {
        List<ScanHistory> historyList = databaseHelper.getAllScanHistory();

        if (historyList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
            summaryLayout.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
            summaryLayout.setVisibility(View.VISIBLE);
            adapter.setHistoryList(historyList);
        }
    }

    private void updateSummary() {
        List<ScanHistory> todayHistory = databaseHelper.getTodayScanHistory();
        int totalCount = databaseHelper.getHistoryCount();

        summaryToday.setText("Сегодня: " + todayHistory.size() + " операций");
        summaryTotal.setText("Всего: " + totalCount + " операций");
    }

    private void clearHistory() {
        databaseHelper.clearAllHistory();
        loadHistory();
        updateSummary();
        // Показываем сообщение об успешной очистке
        Toast.makeText(this, "История очищена", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Обновляем данные при возвращении на экран
        loadHistory();
        updateSummary();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}