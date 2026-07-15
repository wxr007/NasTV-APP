package com.nastv.di
import com.nastv.data.api.AuthApi
import com.nastv.data.api.PlayApi
import com.nastv.data.api.SearchApi
import com.nastv.data.api.SourceApi
import com.nastv.data.api.UserDataApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSourceApi(retrofit: Retrofit): SourceApi {
        return retrofit.create(SourceApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSearchApi(retrofit: Retrofit): SearchApi {
        return retrofit.create(SearchApi::class.java)
    }

    @Provides
    @Singleton
    fun providePlayApi(retrofit: Retrofit): PlayApi {
        return retrofit.create(PlayApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUserDataApi(retrofit: Retrofit): UserDataApi {
        return retrofit.create(UserDataApi::class.java)
    }
}
