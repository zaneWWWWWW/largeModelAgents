package com.example.projectv3.api;

import com.example.projectv3.dto.unified.TestDetailDTO;
import com.example.projectv3.dto.unified.TestListDTO;
import com.example.projectv3.dto.unified.TestResultDTO;
import com.example.projectv3.dto.unified.TestSubmissionDTO;
import com.example.projectv3.dto.unified.TestSessionDTO;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface UnifiedTestApi {

    @GET("api/tests")
    Call<List<TestListDTO>> getActiveTests();

    @GET("api/tests/{id}")
    Call<TestDetailDTO> getTestDetails(@Path("id") Long testId);

    @POST("api/tests/{id}/sessions")
    Call<TestSessionDTO> createTestSession(@Path("id") Long testId);

    @POST("api/sessions/{sessionId}/responses")
    Call<TestResultDTO> submitAnswers(@Path("sessionId") Long sessionId, @Body TestSubmissionDTO submissionDTO);

    @GET("api/sessions/{sessionId}/result")
    Call<TestResultDTO> getTestResult(@Path("sessionId") Long sessionId);
}

