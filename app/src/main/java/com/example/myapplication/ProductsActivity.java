package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class ProductsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ProductsAdapter adapter;
    private DatabaseHelper databaseHelper;
    private TextView emptyText;
    private TextView summaryCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_products);

        databaseHelper = new DatabaseHelper(this);

        initViews();
        setupRecyclerView();
        loadProducts();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerViewProducts);
        emptyText = findViewById(R.id.textEmptyProducts);
        summaryCount = findViewById(R.id.textSummaryCount);

        Button buttonBack = findViewById(R.id.buttonBack);
        Button buttonClear = findViewById(R.id.buttonClearProducts);

        buttonBack.setOnClickListener(v -> finish());
        buttonClear.setOnClickListener(v -> clearProducts());
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProductsAdapter(databaseHelper);
        recyclerView.setAdapter(adapter);

        // Устанавливаем callback для обновления summary при изменениях
        adapter.setUpdateCallback(() -> updateSummary());
    }

    private void loadProducts() {
        List<ScannedProduct> productList = databaseHelper.getAllScannedProducts();

        if (productList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyText.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyText.setVisibility(View.GONE);
            adapter.setProductList(productList);
        }

        updateSummary();
    }

    private void updateSummary() {
        int count = databaseHelper.getScannedProductsCount();
        summaryCount.setText("Всего товаров: " + count);
    }

    private void clearProducts() {
        databaseHelper.clearAllScannedProducts();
        loadProducts();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (databaseHelper != null) {
            databaseHelper.close();
        }
    }
}