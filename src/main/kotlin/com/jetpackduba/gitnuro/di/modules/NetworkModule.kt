package com.jetpackduba.gitnuro.di.modules

import com.jetpackduba.gitnuro.updates.UpdatesService
import dagger.Provides
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.net.ssl.X509TrustManager

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

    @Provides
    fun provideKtorHttpClient(): HttpClient {
        val httpClient = HttpClient(CIO) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.ALL
            }
            engine {
                https {
                    trustManager = object : X509TrustManager {
                        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}

                        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}

                        override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                    }
                }
            }
        }

        return httpClient
    }
}