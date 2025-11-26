package com.foodie.app.di

import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.remote.interceptor.AuthInterceptor
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Test to verify NetworkModule provides dependencies correctly.
 */
class NetworkModuleTest {

    private lateinit var mockSecurePreferences: SecurePreferences
    private lateinit var mockAuthInterceptor: AuthInterceptor

    @Before
    fun setUp() {
        mockSecurePreferences = mock()
        mockAuthInterceptor = mock()

        // Configure mocks with default values
        whenever(mockSecurePreferences.azureOpenAiEndpoint).thenReturn(null)
        whenever(mockSecurePreferences.azureOpenAiModel).thenReturn(null)
    }

    @Test
    fun `NetworkModule should provide OkHttpClient`() {
        val loggingInterceptor = NetworkModule.provideHttpLoggingInterceptor()
        val okHttpClient = NetworkModule.provideOkHttpClient(loggingInterceptor, mockAuthInterceptor)

        assertThat(okHttpClient).isNotNull()
        assertThat(okHttpClient.interceptors).contains(mockAuthInterceptor)
        assertThat(okHttpClient.interceptors).contains(loggingInterceptor)
    }

    @Test
    fun `NetworkModule should provide Retrofit`() {
        val loggingInterceptor = NetworkModule.provideHttpLoggingInterceptor()
        val gson = NetworkModule.provideGson()
        val okHttpClient = NetworkModule.provideOkHttpClient(loggingInterceptor, mockAuthInterceptor)
        val retrofit = NetworkModule.provideRetrofit(okHttpClient, mockSecurePreferences, gson)

        assertThat(retrofit).isNotNull()
        assertThat(retrofit.baseUrl().toString()).contains("openai.azure.com")
    }

    @Test
    fun `NetworkModule should provide Gson`() {
        val gson = NetworkModule.provideGson()

        assertThat(gson).isNotNull()
    }
}
