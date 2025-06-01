package com.jetpackduba.gitnuro.di.modules

import dagger.Provides
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.logging.*
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

@dagger.Module
class NetworkModule {
    @Provides
    fun provideKtorHttpClient(): HttpClient {
        val httpClient = HttpClient(CIO) {
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.NONE
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