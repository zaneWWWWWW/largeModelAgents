package com.example.projectv3.api;

import com.example.projectv3.dto.unified.TestCreateRequest;
import com.example.projectv3.dto.unified.TestListDTO;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface TestAdminApi {

    @POST("api/admin/tests")
    Call<TestListDTO> createTest(@Body TestCreateRequest request);

    @Multipart
    @POST("api/admin/tests/{id}/import")
    Call<Void> importQuestions(@Path("id") Long testId, @Part MultipartBody.Part file);

    @GET("api/admin/tests/{id}/export")
    Call<ResponseBody> exportResponses(@Path("id") Long testId);

    @POST("api/admin/tests/{id}/offline")
    Call<Void> offlineTest(@Path("id") Long testId);
}

