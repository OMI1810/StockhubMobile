package com.example.myapplication;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.ProductViewHolder> {

    private List<ScannedProduct> productList = new ArrayList<>();
    private DatabaseHelper databaseHelper;
    private UpdateCallback updateCallback;
    private static final String TAG = "ProductsAdapter";

    public interface UpdateCallback {
        void onUpdate();
    }

    public ProductsAdapter(DatabaseHelper databaseHelper) {
        this.databaseHelper = databaseHelper;
    }

    public void setUpdateCallback(UpdateCallback callback) {
        this.updateCallback = callback;
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        try {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_product, parent, false);
            return new ProductViewHolder(view);
        } catch (Exception e) {
            Log.e(TAG, "Error creating view holder: " + e.getMessage());
            View emptyView = new View(parent.getContext());
            return new ProductViewHolder(emptyView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        try {
            if (position < productList.size()) {
                ScannedProduct product = productList.get(position);
                holder.bind(product);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error binding view holder at position " + position + ": " + e.getMessage());
        }
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void setProductList(List<ScannedProduct> productList) {
        if (productList != null) {
            this.productList = productList;
        } else {
            this.productList = new ArrayList<>();
        }
        notifyDataSetChanged();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private TextView textCodeType;
        private TextView textCodeContent;
        private TextView textTimestamp;
        private TextView textQuantity;
        private EditText editProductInfo;
        private Button buttonRemove;
        private Button buttonDecrease;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            try {
                textCodeType = itemView.findViewById(R.id.textCodeType);
                textCodeContent = itemView.findViewById(R.id.textCodeContent);
                textTimestamp = itemView.findViewById(R.id.textTimestamp);
                textQuantity = itemView.findViewById(R.id.textQuantity);
                editProductInfo = itemView.findViewById(R.id.editProductInfo);
                buttonRemove = itemView.findViewById(R.id.buttonRemove);
                buttonDecrease = itemView.findViewById(R.id.buttonDecrease);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing view holder views: " + e.getMessage());
            }
        }

        public void bind(ScannedProduct product) {
            if (product == null) return;

            try {
                textCodeType.setText("Тип: " + (product.getCodeType() != null ? product.getCodeType() : ""));
                textCodeContent.setText(product.getCodeContent() != null ? product.getCodeContent() : "");
                textTimestamp.setText("Обновлено: " + formatTimestamp(product.getTimestamp()));
                textQuantity.setText(String.valueOf(product.getQuantity()));
                editProductInfo.setText(product.getProductInfo() != null ? product.getProductInfo() : "");

                // Обработчик для кнопки уменьшения количества
                buttonDecrease.setOnClickListener(v -> {
                    try {
                        int currentQuantity = product.getQuantity();
                        if (currentQuantity > 1) {
                            int result = databaseHelper.decreaseProductQuantity(product.getId());
                            if (result > 0) {
                                product.setQuantity(currentQuantity - 1);
                                textQuantity.setText(String.valueOf(product.getQuantity()));

                                if (updateCallback != null) {
                                    updateCallback.onUpdate();
                                }
                                Toast.makeText(itemView.getContext(), "Количество уменьшено", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Если количество становится 0, удаляем товар
                            databaseHelper.deleteScannedProduct(product.getId());
                            int position = getAdapterPosition();
                            if (position != RecyclerView.NO_POSITION && position < productList.size()) {
                                productList.remove(position);
                                notifyItemRemoved(position);

                                if (updateCallback != null) {
                                    updateCallback.onUpdate();
                                }
                                Toast.makeText(itemView.getContext(), "Товар удален", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error decreasing quantity: " + e.getMessage());
                        Toast.makeText(itemView.getContext(), "Ошибка при изменении количества", Toast.LENGTH_SHORT).show();
                    }
                });

                // Удаление товара
                buttonRemove.setOnClickListener(v -> {
                    try {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION && position < productList.size()) {
                            ScannedProduct productToRemove = productList.get(position);
                            databaseHelper.deleteScannedProduct(productToRemove.getId());
                            productList.remove(position);
                            notifyItemRemoved(position);

                            if (updateCallback != null) {
                                updateCallback.onUpdate();
                            }
                            Toast.makeText(itemView.getContext(), "Товар удален", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error removing product: " + e.getMessage());
                        Toast.makeText(itemView.getContext(), "Ошибка при удалении товара", Toast.LENGTH_SHORT).show();
                    }
                });

                // Сохранение информации о товаре при изменении
                editProductInfo.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {}

                    @Override
                    public void afterTextChanged(Editable s) {
                        try {
                            product.setProductInfo(s.toString());
                            databaseHelper.updateScannedProduct(product);
                        } catch (Exception e) {
                            Log.e(TAG, "Error updating product info: " + e.getMessage());
                        }
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Error binding product: " + e.getMessage());
            }
        }

        private String formatTimestamp(String timestamp) {
            try {
                if (timestamp != null && timestamp.contains(" ")) {
                    String[] parts = timestamp.split(" ");
                    if (parts.length >= 2) {
                        return parts[1].substring(0, 5);
                    }
                }
                return timestamp != null ? timestamp : "";
            } catch (Exception e) {
                return timestamp != null ? timestamp : "";
            }
        }
    }
}