package com.example.project.api;

import com.example.project.model.MbtiQuestion;
import com.example.project.model.MbtiType;
import com.example.project.model.User;
import com.example.project.model.SCL90Result;
import com.example.project.model.SCL90Question;
import com.example.project.model.SCL90Factor;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import okhttp3.RequestBody;
import okhttp3.MultipartBody;

public interface UserApi {
    @POST("api/user/login")
    Call<User> login(@Body User user);

    @POST("api/user/register")
    Call<User> register(@Body User user);

    @GET("api/user/{userId}")
    Call<User> getUserInfo(@Path("userId") Long userId);

    @GET("api/mbti/questions")
    Call<List<MbtiQuestion>> getMbtiQuestions();

    @GET("api/mbti/types/{typeCode}")
    Call<MbtiType> getMbtiType(@Path("typeCode") String typeCode);

    @PUT("api/mbti/user/{userId}")
    Call<Void> updateUserMbtiType(@Path("userId") Long userId, @Body Map<String, String> mbtiType);

    @PUT("api/user/{userId}/field")
    Call<User> updateUserField(@Path("userId") Long userId, @Body Map<String, String> fieldData);

    @PUT("api/user/{userId}/password")
    Call<Void> updatePassword(@Path("userId") Long userId, @Body Map<String, String> passwordData);

    @Multipart
    @POST("api/upload/avatar")
    Call<Map<String, String>> uploadAvatar(
            @Part MultipartBody.Part file,
            @Part("userId") RequestBody userId
    );
    
    // SCL-90相关接口
    @GET("api/scl90/questions")
    Call<List<SCL90Question>> getSCL90Questions();
    
    @POST("api/scl90/results")
    Call<SCL90Result> saveSCL90Result(@Body SCL90Result result);
    
    @GET("api/scl90/results/{userId}")
    Call<SCL90Result> getSCL90Result(@Path("userId") Long userId);
    
    @DELETE("api/scl90/results/{userId}")
    Call<Void> deleteSCL90Result(@Path("userId") Long userId);
    
    // SCL-90因子相关接口
    @GET("api/scl90/factors")
    Call<List<SCL90Factor>> getSCL90Factors();
    
    @GET("api/scl90/factors/{id}")
    Call<SCL90Factor> getSCL90FactorById(@Path("id") Integer id);
    
    @GET("api/scl90/factors/name/{factorName}")
    Call<SCL90Factor> getSCL90FactorByName(@Path("factorName") String factorName);
} 