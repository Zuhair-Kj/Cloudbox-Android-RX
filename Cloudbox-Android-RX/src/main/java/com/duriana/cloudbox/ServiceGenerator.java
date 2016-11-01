package com.duriana.cloudbox;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by tonyhaddad on 14/07/2016.
 */
public class ServiceGenerator {

    public static <S> S createService(String API_BASE_URL, Class<S> serviceClass) {
        return new Retrofit.Builder()
                .baseUrl(API_BASE_URL)
                .addConverterFactory(JacksonConverterFactory.create())
                .client(new OkHttpClient.Builder().build()).build().create(serviceClass);
    }

}