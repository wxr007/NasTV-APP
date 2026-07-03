package com.nastv.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.nastv.data.local.PreferencesRepository
import com.nastv.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrl

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthInterceptor

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BaseUrlInterceptor

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile(Constants.DATASTORE_NAME)
        }
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    @AuthInterceptor
    fun provideAuthInterceptor(
        preferencesRepository: PreferencesRepository
    ): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val token = runBlocking {
                preferencesRepository.authTokenFlow.first()
            }

            val newRequest = if (!token.isNullOrEmpty()) {
                originalRequest.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                originalRequest
            }
            chain.proceed(newRequest)
        }
    }

    @Provides
    @Singleton
    @BaseUrlInterceptor
    fun provideBaseUrlInterceptor(
        preferencesRepository: PreferencesRepository
    ): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()
            val baseUrl = runBlocking {
                preferencesRepository.serverUrlFlow.first()
            }

            val normalizedUrl = if (baseUrl.startsWith("http://") || baseUrl.startsWith("https://")) {
                baseUrl
            } else {
                "http://$baseUrl"
            }
            val scheme = normalizedUrl.substringBefore("://")
            val hostAndPort = normalizedUrl.removePrefix("http://").removePrefix("https://").substringBefore("/")

            val newUrl = originalRequest.url.newBuilder()
                .scheme(scheme)
                .host(hostAndPort.substringBefore(":"))
                .apply {
                    val portStr = hostAndPort.substringAfter(":", "")
                    if (portStr.isNotEmpty()) {
                        port(portStr.toInt())
                    }
                }
                .build()

            val newRequest = originalRequest.newBuilder()
                .url(newUrl)
                .build()

            chain.proceed(newRequest)
        }
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @AuthInterceptor authInterceptor: Interceptor,
        @BaseUrlInterceptor baseUrlInterceptor: Interceptor
    ): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(baseUrlInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(Constants.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(Constants.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(Constants.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        okHttpClient: OkHttpClient,
        moshi: Moshi
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.DEFAULT_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}
