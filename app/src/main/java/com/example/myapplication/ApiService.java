package com.example.myapplication;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ApiService {

    // Аутентификация пользователя
    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest loginRequest);

    // Получение информации о товаре по штрих-коду
    @GET("api/warehouse/product")
    Call<ProductInfoResponse> getProductInfo(@Query("barcode") String barcode);

    // Отправка операции на склад (если нужно)
    @POST("api/warehouse/operation")
    Call<OperationResponse> sendOperation(@Body OperationRequest operationRequest);
}