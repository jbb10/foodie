package com.foodie.app.di

import com.google.common.truth.Truth.assertThat
import okhttp3.OkHttpClient
import org.junit.Test
import retrofit2.Retrofit

/**
 * Test to verify NetworkModule provides dependencies correctly.
 */
class NetworkModuleTest {

    @Test
    fun `NetworkModule should provide OkHttpClient`() {
        val loggingInterceptor = NetworkModule.provideHttpLoggingInterceptor()
        val okHttpClient = NetworkModule.provideOkHttpClient(loggingInterceptor)
        
        assertThat(okHttpClient).isNotNull()
        assertThat(okHttpClient.interceptors).contains(loggingInterceptor)
    }

    @Test
    fun `NetworkModule should provide Retrofit`() {
        val loggingInterceptor = NetworkModule.provideHttpLoggingInterceptor()
        val okHttpClient = NetworkModule.provideOkHttpClient(loggingInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient)
        
        assertThat(retrofit).isNotNull()
        assertThat(retrofit.baseUrl().toString()).contains("openai.com")
    }
}
