package com.example.skyma.testrecognition.Retrofit;

import com.example.skyma.testrecognition.Models.VisionFile;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Created by skyma on 5/16/2017.
 */

public interface RetrofitInterface {

    @POST("Chmahi/GetOCRDataFromApi")
    // Call<VisionFile> getOCRData(@Header("imageURI") String imageURI);
    Call<VisionFile> getOCRData();

    @GET("Chmahi/TesterMethod2")
    Call<String> testy();
}
