package com.jetpackduba.gitnuro.domain.models

data class ProxySettings(
    val useProxy: Boolean,
    val proxyType: ProxyType,
    val hostName: String,
    val hostPort: Int,
    val useAuth: Boolean,
    val hostUser: String,
    val hostPassword: String,
)