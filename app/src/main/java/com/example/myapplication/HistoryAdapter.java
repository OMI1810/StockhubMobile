package com.example.myapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<ScanHistory> historyList = new ArrayList<>();

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        ScanHistory history = historyList.get(position);
        holder.bind(history);
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public void setHistoryList(List<ScanHistory> historyList) {
        this.historyList = historyList;
        notifyDataSetChanged();
    }

    static class HistoryViewHolder extends RecyclerView.ViewHolder {
        private TextView textOperation;
        private TextView textCodeType;
        private TextView textContent;
        private TextView textTimestamp;
        private TextView textStatus;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            // Исправленные ID - должны совпадать с item_history.xml
            textOperation = itemView.findViewById(R.id.textOperation);
            textCodeType = itemView.findViewById(R.id.textCodeType);
            textContent = itemView.findViewById(R.id.textContent);
            textTimestamp = itemView.findViewById(R.id.textTimestamp);
            textStatus = itemView.findViewById(R.id.textStatus);
        }

        public void bind(ScanHistory history) {
            textOperation.setText(getOperationDisplayName(history.getOperationType()));
            textCodeType.setText(history.getCodeType());
            textContent.setText(history.getCodeContent());
            textTimestamp.setText(formatTimestamp(history.getTimestamp()));
            textStatus.setText(history.getStatus());

            // Установка цвета статуса
            int statusColor = android.graphics.Color.GRAY;
            if ("УСПЕХ".equals(history.getStatus())) {
                statusColor = itemView.getContext().getResources().getColor(android.R.color.holo_green_dark);
            } else if ("ОШИБКА".equals(history.getStatus())) {
                statusColor = itemView.getContext().getResources().getColor(android.R.color.holo_red_dark);
            } else if ("ДЕМО".equals(history.getStatus())) {
                statusColor = itemView.getContext().getResources().getColor(android.R.color.holo_orange_dark);
            }
            textStatus.setTextColor(statusColor);
        }

        private String getOperationDisplayName(String operationType) {
            if ("shipment".equals(operationType)) {
                return "📤 Отгрузка";
            } else if ("loading".equals(operationType)) {
                return "📥 Загрузка";
            } else {
                return operationType;
            }
        }

        private String formatTimestamp(String timestamp) {
            try {
                // Преобразуем формат времени для лучшего отображения
                if (timestamp.contains(" ")) {
                    return timestamp.replace(" ", " в ");
                }
                return timestamp;
            } catch (Exception e) {
                return timestamp;
            }
        }
    }
}