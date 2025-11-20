package com.example.projectv3.api;

import com.example.projectv3.dto.PsychStatusDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface PsychStatusApi {
    @POST("api/status/report")
    Call<Void> reportStatus(@Body PsychStatusDTO dto);
}
