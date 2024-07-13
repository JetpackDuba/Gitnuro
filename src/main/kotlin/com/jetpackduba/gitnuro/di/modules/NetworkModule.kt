package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.updates.UpdatesService
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import javax.inject.Inject

@dagger.Module
class NetworkModule {
    @Provides
    fun provideWebService(): UpdatesService {
        return Retrofit.Builder()
            .baseUrl("https://github.com")
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()
            .create(UpdatesService::class.java)
    }
}