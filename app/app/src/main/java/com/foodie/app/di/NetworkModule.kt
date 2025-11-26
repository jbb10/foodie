package com.foodie.app.di

import com.foodie.app.data.local.preferences.SecurePreferences
import com.foodie.app.data.remote.api.AzureOpenAiApi
import com.foodie.app.data.remote.interceptor.AuthInterceptor
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber

/**
 * Network module providing Retrofit and OkHttp instances for Azure OpenAI API.
 *
 * **Configuration:**
 * - Base URL from SecurePreferences (fallback to placeholder if not configured)
 * - AuthInterceptor for api-key header injection
 * - Extended timeouts for AI vision analysis (15s connect, 30s read/write)
 * - Logging interceptor for debugging (body-level logging)
 *
 * **Timeout Rationale:**
 * - Connect timeout: 15s (Azure OpenAI typically responds fast, but allow buffer)
 * - Read timeout: 30s (AI vision analysis can take 10-30s)
 * - Write timeout: 30s (multimodal requests with base64 images can be large ~500KB-1MB)
 *
 * **Future Configuration:**
 * - Story 5.2 will implement user settings for API key, endpoint, and model
 * - Until then, placeholder endpoint used (API calls will fail without configuration)
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TAG = "NetworkModule"
    private const val DEFAULT_ENDPOINT = "https://placeholder.openai.azure.com/" // Fallback for unconfigured state

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .setLenient() // Allow lenient parsing for AI-generated JSON
            .create()
    }

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: AuthInterceptor,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // Auth interceptor BEFORE logging to see headers
            .addInterceptor(loggingInterceptor)
            .connectTimeout(15, TimeUnit.SECONDS) // Reduced from 30s - Azure typically fast
            .readTimeout(30, TimeUnit.SECONDS) // AI vision analysis can take 10-30s
            .writeTimeout(30, TimeUnit.SECONDS) // Base64 images can be large (~500KB-1MB)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        securePreferences: SecurePreferences,
        gson: Gson,
    ): Retrofit {
        // Read base URL from SecurePreferences, fallback to placeholder if not configured
        val baseUrl = securePreferences.azureOpenAiEndpoint?.let { endpoint ->
            // Ensure endpoint has trailing slash for Retrofit
            if (endpoint.endsWith("/")) endpoint else "$endpoint/"
        } ?: run {
            Timber.tag(TAG).w("Azure OpenAI endpoint not configured, using placeholder. API calls will fail until Story 5.2 implements settings.")
            DEFAULT_ENDPOINT
        }

        Timber.tag(TAG).d("Configuring Retrofit with base URL: $baseUrl")

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    @Provides
    @Singleton
    fun provideAzureOpenAiApi(retrofit: Retrofit): AzureOpenAiApi {
        return retrofit.create(AzureOpenAiApi::class.java)
    }
}
