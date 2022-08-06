package app.di.modules

import app.updates.UpdatesService
import dagger.Provides
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory

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