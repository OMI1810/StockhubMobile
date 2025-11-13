package com.example.myapplication;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.ProductViewHolder> {

    private List<ScannedProduct> productList = new ArrayList<>();
    private DatabaseHelper databaseHelper;
    private UpdateCallback updateCallback;

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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ScannedProduct product = productList.get(position);
        holder.bind(product);
    }

    @Override
    public int getItemCount() {
        return productList.size();
    }

    public void setProductList(List<ScannedProduct> productList) {
        this.productList = productList;
        notifyDataSetChanged();
    }

    class ProductViewHolder extends RecyclerView.ViewHolder {
        private TextView textCodeType;
        private TextView textCodeContent;
        private TextView textTimestamp;
        private EditText editProductInfo;
        private Button buttonRemove;

        public ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            textCodeType = itemView.findViewById(R.id.textCodeType);
            textCodeContent = itemView.findViewById(R.id.textCodeContent);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            editProductInfo = itemView.findViewById(R.id.editProductInfo);
            buttonRemove = itemView.findViewById(R.id.buttonRemove);
        }

        public void bind(ScannedProduct product) {
            textCodeType.setText("Тип: " + product.getCodeType());
            textCodeContent.setText("Код: " + product.getCodeContent());
            textTimestamp.setText("Время: " + formatTimestamp(product.getTimestamp()));
            editProductInfo.setText(product.getProductInfo());

            // Сохранение информации о товаре при изменении
            editProductInfo.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    product.setProductInfo(s.toString());
                    databaseHelper.updateScannedProduct(product);
                }
            });

            // Удаление товара
            buttonRemove.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    ScannedProduct productToRemove = productList.get(position);
                    databaseHelper.deleteScannedProduct(productToRemove.getId());
                    productList.remove(position);
                    notifyItemRemoved(position);

                    if (updateCallback != null) {
                        updateCallback.onUpdate();
                    }
                }
            });
        }

        private String formatTimestamp(String timestamp) {
            try {
                if (timestamp.contains(" ")) {
                    String[] parts = timestamp.split(" ");
                    if (parts.length >= 2) {
                        return parts[1].substring(0, 5); // Возвращаем только время HH:mm
                    }
                }
                return timestamp;
            } catch (Exception e) {
                return timestamp;
            }
        }
    }
}