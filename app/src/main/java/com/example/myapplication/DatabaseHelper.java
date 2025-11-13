package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Информация о базе данных
    private static final String DATABASE_NAME = "WarehouseDB";
    private static final int DATABASE_VERSION = 2;

    // Таблица истории сканирований
    private static final String TABLE_SCAN_HISTORY = "scan_history";

    // Новая таблица для отсканированных товаров
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

    // SQL запрос для создания таблицы
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
            + KEY_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
            + KEY_SENT_TO_SERVER + " INTEGER DEFAULT 0"
            + ")";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_SCAN_HISTORY);
        db.execSQL(CREATE_TABLE_SCANNED_PRODUCTS);
    }
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // При обновлении базы добавляем новую таблицу
        if (oldVersion < 2) {
            db.execSQL(CREATE_TABLE_SCANNED_PRODUCTS);
        }
    }


    // Добавление записи в историю
    public long addScanHistory(ScanHistory history) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_OPERATION_TYPE, history.getOperationType());
        values.put(KEY_CODE_TYPE, history.getCodeType());
        values.put(KEY_CODE_CONTENT, history.getCodeContent());
        values.put(KEY_TIMESTAMP, history.getTimestamp());
        values.put(KEY_STATUS, history.getStatus());

        long id = db.insert(TABLE_SCAN_HISTORY, null, values);
        db.close();

        return id;
    }


    // === МЕТОДЫ ДЛЯ SCANNED_PRODUCTS ===

    // Добавление отсканированного товара
    public long addScannedProduct(ScannedProduct product) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_OPERATION_TYPE, product.getOperationType());
        values.put(KEY_CODE_TYPE, product.getCodeType());
        values.put(KEY_CODE_CONTENT, product.getCodeContent());
        values.put(KEY_PRODUCT_INFO, product.getProductInfo());
        values.put(KEY_TIMESTAMP, product.getTimestamp());
        values.put(KEY_SENT_TO_SERVER, product.isSentToServer() ? 1 : 0);

        long id = db.insert(TABLE_SCANNED_PRODUCTS, null, values);
        db.close();

        return id;
    }

    // Получение всех отсканированных товаров
    @SuppressLint("Range")
    public List<ScannedProduct> getAllScannedProducts() {
        List<ScannedProduct> productList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_SCANNED_PRODUCTS + " ORDER BY " + KEY_TIMESTAMP + " DESC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

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

                productList.add(product);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return productList;
    }

    // Обновление информации о товаре
    public int updateScannedProduct(ScannedProduct product) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_PRODUCT_INFO, product.getProductInfo());
        values.put(KEY_SENT_TO_SERVER, product.isSentToServer() ? 1 : 0);

        int result = db.update(TABLE_SCANNED_PRODUCTS, values, KEY_ID + " = ?",
                new String[]{String.valueOf(product.getId())});
        db.close();

        return result;
    }

    // Удаление товара
    public void deleteScannedProduct(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SCANNED_PRODUCTS, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Очистка всех товаров
    public void clearAllScannedProducts() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SCANNED_PRODUCTS, null, null);
        db.close();
    }

    // Получение количества товаров
    public int getScannedProductsCount() {
        String countQuery = "SELECT * FROM " + TABLE_SCANNED_PRODUCTS;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count;
    }

    // Проверка существования товара с таким codeContent
    public boolean productExists(String codeContent) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_SCANNED_PRODUCTS,
                new String[]{KEY_ID},
                KEY_CODE_CONTENT + " = ?",
                new String[]{codeContent},
                null, null, null);

        boolean exists = cursor.getCount() > 0;
        cursor.close();
        db.close();
        return exists;
    }
    // Получение всей истории
    @SuppressLint("Range")
    public List<ScanHistory> getAllScanHistory() {
        List<ScanHistory> historyList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_SCAN_HISTORY + " ORDER BY " + KEY_TIMESTAMP + " DESC";

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

    // Получение истории за сегодня
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

    // Удаление записи по ID
    public void deleteScanHistory(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SCAN_HISTORY, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    // Очистка всей истории
    public void clearAllHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SCAN_HISTORY, null, null);
        db.close();
    }

    // Получение количества записей
    public int getHistoryCount() {
        String countQuery = "SELECT * FROM " + TABLE_SCAN_HISTORY;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count;
    }
}