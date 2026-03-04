package com.jetpackduba.gitnuro.domain.repositories

import com.jetpackduba.gitnuro.domain.models.ProxySettings

interface SystemProxyRepository {
    suspend fun setProxy(proxySettings: ProxySettings)
    suspend fun clearProxy()
}