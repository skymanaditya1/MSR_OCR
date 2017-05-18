package com.example.skyma.testrecognition.Retrofit;

import android.text.TextUtils;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by skyma on 5/16/2017.
 */

public class RetrofitBuilder {
    public static final String API_BASE_URL = "http://secopserver4.azurewebsites.net/api/";

    //static Retrofit retrofit;
    static OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    private static Retrofit.Builder builder =
            new Retrofit.Builder()
                    .baseUrl(API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create());

    public static <S> S createService(Class<S> serviceClass) {
        return createService(serviceClass, null);
    }


    public static <S> S createService(
            Class<S> serviceClass, final String authToken) {
        if (!TextUtils.isEmpty(authToken)) {
            AuthenticationInterceptor interceptor =
                    new AuthenticationInterceptor(authToken);

            if (!httpClient.interceptors().contains(interceptor)) {
                httpClient.addInterceptor(interceptor);

                builder.client(httpClient.build());
                retrofit = builder.build();
            }
        }

        return retrofit.create(serviceClass);
    }

    static Retrofit retrofit = new Retrofit.Builder().baseUrl(API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build()).build();
}
