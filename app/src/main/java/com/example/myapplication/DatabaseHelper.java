package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "WarehouseDB";
    private static final int DATABASE_VERSION = 3;
    private static final String TAG = "DatabaseHelper";

    private static final String TABLE_SCAN_HISTORY = "scan_history";
    private static final String TABLE_SCANNED_PRODUCTS = "scanned_products";

    // Общие колонки
    private static final String KEY_ID = "id";
    private static final String KEY_OPERATION_TYPE = "operation_type";
    private static final String KEY_CODE_TYPE = "code_type";
    private static final String KEY_CODE_CONTENT = "code_content";
    private static final String KEY_TIMESTAMP = "timestamp";

    // Колонки для scan_history
    private static final String KEY_STATUS = "status";

    // Колонки для scanned_products
    private static final String KEY_PRODUCT_INFO = "product_info";
    private static final String KEY_SENT_TO_SERVER = "sent_to_server";
    private static final String KEY_QUANTITY = "quantity";

    // SQL запросы
    private static final String CREATE_TABLE_SCAN_HISTORY = "CREATE TABLE " + TABLE_SCAN_HISTORY + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_OPERATION_TYPE + " TEXT,"
            + KEY_CODE_TYPE + " TEXT,"
            + KEY_CODE_CONTENT + " TEXT,"
            + KEY_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + KEY_STATUS + " TEXT"
            + ")";

    private static final String CREATE_TABLE_SCANNED_PRODUCTS = "CREATE TABLE " + TABLE_SCANNED_PRODUCTS + "("
            + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + KEY_OPERATION_TYPE + " TEXT,"
            + KEY_CODE_TYPE + " TEXT,"
            + KEY_CODE_CONTENT + " TEXT,"
            + KEY_PRODUCT_INFO + " TEXT,"
            + KEY_QUANTITY + " INTEGER DEFAULT 1,"
            + KEY_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + KEY_SENT_TO_SERVER + " INTEGER DEFAULT 0"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_TABLE_SCAN_HISTORY);
            db.execSQL(CREATE_TABLE_SCANNED_PRODUCTS);
            Log.d(TAG, "Database created successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error creating database: " + e.getMessage());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE " + TABLE_SCANNED_PRODUCTS + " ADD COLUMN " + KEY_QUANTITY + " INTEGER DEFAULT 1");
                Log.d(TAG, "Database upgraded to version 3");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error upgrading database: " + e.getMessage());
        }
    }

    // === МЕТОДЫ ДЛЯ SCANNED_PRODUCTS ===

    // Добавление или обновление отсканированного товара
    public long addOrUpdateScannedProduct(ScannedProduct product) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;

        try {
            // Проверяем, существует ли уже товар с таким кодом
            Cursor cursor = db.query(TABLE_SCANNED_PRODUCTS,
                    new String[]{KEY_ID, KEY_QUANTITY},
                    KEY_CODE_CONTENT + " = ? AND " + KEY_OPERATION_TYPE + " = ?",
                    new String[]{product.getCodeContent(), product.getOperationType()},
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                // Товар существует - увеличиваем количество
                @SuppressLint("Range") int existingId = cursor.getInt(cursor.getColumnIndex(KEY_ID));
                @SuppressLint("Range") int currentQuantity = cursor.getInt(cursor.getColumnIndex(KEY_QUANTITY));

                ContentValues values = new ContentValues();
                values.put(KEY_QUANTITY, currentQuantity + 1);
                values.put(KEY_TIMESTAMP, product.getTimestamp());

                db.update(TABLE_SCANNED_PRODUCTS, values, KEY_ID + " = ?",
                        new String[]{String.valueOf(existingId)});
                id = existingId;
                Log.d(TAG, "Updated product quantity: " + product.getCodeContent() + ", new quantity: " + (currentQuantity + 1));
            } else {
                // Товар не существует - добавляем новый
                ContentValues values = new ContentValues();
                values.put(KEY_OPERATION_TYPE, product.getOperationType());
                values.put(KEY_CODE_TYPE, product.getCodeType());
                values.put(KEY_CODE_CONTENT, product.getCodeContent());
                values.put(KEY_PRODUCT_INFO, product.getProductInfo());
                values.put(KEY_TIMESTAMP, product.getTimestamp());
                values.put(KEY_SENT_TO_SERVER, product.isSentToServer() ? 1 : 0);
                values.put(KEY_QUANTITY, product.getQuantity());

                id = db.insert(TABLE_SCANNED_PRODUCTS, null, values);
                Log.d(TAG, "Added new product: " + product.getCodeContent());
            }

            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in addOrUpdateScannedProduct: " + e.getMessage());
        } finally {
            db.close();
        }
        return id;
    }

    // Уменьшение количества товара
    public int decreaseProductQuantity(int productId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = 0;

        try {
            // Получаем текущее количество
            Cursor cursor = db.query(TABLE_SCANNED_PRODUCTS,
                    new String[]{KEY_QUANTITY},
                    KEY_ID + " = ?",
                    new String[]{String.valueOf(productId)},
                    null, null, null);

            int currentQuantity = 1;
            if (cursor != null && cursor.moveToFirst()) {
                @SuppressLint("Range") int quantity = cursor.getInt(cursor.getColumnIndex(KEY_QUANTITY));
                currentQuantity = quantity;
                cursor.close();
            }

            int newQuantity = Math.max(0, currentQuantity - 1);

            ContentValues values = new ContentValues();
            values.put(KEY_QUANTITY, newQuantity);

            result = db.update(TABLE_SCANNED_PRODUCTS, values, KEY_ID + " = ?",
                    new String[]{String.valueOf(productId)});

            Log.d(TAG, "Decreased product quantity for ID " + productId + " from " + currentQuantity + " to " + newQuantity);
        } catch (Exception e) {
            Log.e(TAG, "Error in decreaseProductQuantity: " + e.getMessage());
        } finally {
            db.close();
        }
        return result;
    }

    // Получение всех отсканированных товаров
    @SuppressLint("Range")
    public List<ScannedProduct> getAllScannedProducts() {
        List<ScannedProduct> productList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String selectQuery = "SELECT * FROM " + TABLE_SCANNED_PRODUCTS + " ORDER BY " + KEY_TIMESTAMP + " DESC";
            cursor = db.rawQuery(selectQuery, null);

            if (cursor.moveToFirst()) {
                do {
                    ScannedProduct product = new ScannedProduct();
                    product.setId(cursor.getInt(cursor.getColumnIndex(KEY_ID)));
                    product.setOperationType(cursor.getString(cursor.getColumnIndex(KEY_OPERATION_TYPE)));
                    product.setCodeType(cursor.getString(cursor.getColumnIndex(KEY_CODE_TYPE)));
                    product.setCodeContent(cursor.getString(cursor.getColumnIndex(KEY_CODE_CONTENT)));
                    product.setProductInfo(cursor.getString(cursor.getColumnIndex(KEY_PRODUCT_INFO)));
                    product.setTimestamp(cursor.getString(cursor.getColumnIndex(KEY_TIMESTAMP)));
                    product.setSentToServer(cursor.getInt(cursor.getColumnIndex(KEY_SENT_TO_SERVER)) == 1);
                    product.setQuantity(cursor.getInt(cursor.getColumnIndex(KEY_QUANTITY)));

                    productList.add(product);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getAllScannedProducts: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return productList;
    }

    // Обновление информации о товаре
    public int updateScannedProduct(ScannedProduct product) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = 0;

        try {
            ContentValues values = new ContentValues();
            values.put(KEY_PRODUCT_INFO, product.getProductInfo());
            values.put(KEY_SENT_TO_SERVER, product.isSentToServer() ? 1 : 0);
            values.put(KEY_QUANTITY, product.getQuantity());

            result = db.update(TABLE_SCANNED_PRODUCTS, values, KEY_ID + " = ?",
                    new String[]{String.valueOf(product.getId())});
        } catch (Exception e) {
            Log.e(TAG, "Error in updateScannedProduct: " + e.getMessage());
        } finally {
            db.close();
        }
        return result;
    }

    // Удаление товара
    public void deleteScannedProduct(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_SCANNED_PRODUCTS, KEY_ID + " = ?", new String[]{String.valueOf(id)});
            Log.d(TAG, "Deleted product with ID: " + id);
        } catch (Exception e) {
            Log.e(TAG, "Error in deleteScannedProduct: " + e.getMessage());
        } finally {
            db.close();
        }
    }

    // Очистка всех товаров
    public void clearAllScannedProducts() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_SCANNED_PRODUCTS, null, null);
            Log.d(TAG, "Cleared all scanned products");
        } catch (Exception e) {
            Log.e(TAG, "Error in clearAllScannedProducts: " + e.getMessage());
        } finally {
            db.close();
        }
    }

    // Получение количества товаров
    public int getScannedProductsCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        int count = 0;
        Cursor cursor = null;

        try {
            String countQuery = "SELECT * FROM " + TABLE_SCANNED_PRODUCTS;
            cursor = db.rawQuery(countQuery, null);
            count = cursor.getCount();
        } catch (Exception e) {
            Log.e(TAG, "Error in getScannedProductsCount: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return count;
    }

    // Проверка существования товара с таким codeContent
    public boolean productExists(String codeContent) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean exists = false;

        try {
            cursor = db.query(TABLE_SCANNED_PRODUCTS,
                    new String[]{KEY_ID},
                    KEY_CODE_CONTENT + " = ?",
                    new String[]{codeContent},
                    null, null, null);

            exists = cursor.getCount() > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error in productExists: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return exists;
    }

    // === МЕТОДЫ ДЛЯ SCAN_HISTORY ===

    @SuppressLint("Range")
    public List<ScanHistory> getAllScanHistory() {
        List<ScanHistory> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;

        try {
            String selectQuery = "SELECT * FROM " + TABLE_SCAN_HISTORY + " ORDER BY " + KEY_TIMESTAMP + " DESC";
            cursor = db.rawQuery(selectQuery, null);

            if (cursor.moveToFirst()) {
                do {
                    ScanHistory history = new ScanHistory();
                    history.setId(cursor.getInt(cursor.getColumnIndex(KEY_ID)));
                    history.setOperationType(cursor.getString(cursor.getColumnIndex(KEY_OPERATION_TYPE)));
                    history.setCodeType(cursor.getString(cursor.getColumnIndex(KEY_CODE_TYPE)));
                    history.setCodeContent(cursor.getString(cursor.getColumnIndex(KEY_CODE_CONTENT)));
                    history.setTimestamp(cursor.getString(cursor.getColumnIndex(KEY_TIMESTAMP)));
                    history.setStatus(cursor.getString(cursor.getColumnIndex(KEY_STATUS)));

                    historyList.add(history);
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in getAllScanHistory: " + e.getMessage());
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.close();
        }
        return historyList;
    }

    public long addScanHistory(ScanHistory history) {
        SQLiteDatabase db = this.getWritableDatabase();
        long id = -1;

        try {
            ContentValues values = new ContentValues();
            values.put(KEY_OPERATION_TYPE, history.getOperationType());
            values.put(KEY_CODE_TYPE, history.getCodeType());
            values.put(KEY_CODE_CONTENT, history.getCodeContent());
            values.put(KEY_TIMESTAMP, history.getTimestamp());
            values.put(KEY_STATUS, history.getStatus());

            id = db.insert(TABLE_SCAN_HISTORY, null, values);
        } catch (Exception e) {
            Log.e(TAG, "Error in addScanHistory: " + e.getMessage());
        } finally {
            db.close();
        }
        return id;
    }

    public void clearAllHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        try {
            db.delete(TABLE_SCAN_HISTORY, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Error in clearAllHistory: " + e.getMessage());
        } finally {
            db.close();
        }
    }

    public int getHistoryCount() {
        String countQuery = "SELECT * FROM " + TABLE_SCAN_HISTORY;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count;
    }
    @SuppressLint("Range")
    public List<ScanHistory> getTodayScanHistory() {
        List<ScanHistory> historyList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_SCAN_HISTORY +
                " WHERE date(" + KEY_TIMESTAMP + ") = date('now')" +
                " ORDER BY " + KEY_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                ScanHistory history = new ScanHistory();
                history.setId(cursor.getInt(cursor.getColumnIndex(KEY_ID)));
                history.setOperationType(cursor.getString(cursor.getColumnIndex(KEY_OPERATION_TYPE)));
                history.setCodeType(cursor.getString(cursor.getColumnIndex(KEY_CODE_TYPE)));
                history.setCodeContent(cursor.getString(cursor.getColumnIndex(KEY_CODE_CONTENT)));
                history.setTimestamp(cursor.getString(cursor.getColumnIndex(KEY_TIMESTAMP)));
                history.setStatus(cursor.getString(cursor.getColumnIndex(KEY_STATUS)));

                historyList.add(history);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return historyList;
    }
}