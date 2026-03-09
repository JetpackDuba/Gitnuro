package com.jetpackduba.gitnuro.domain.models

enum class ProxyType(val value: Int) {
    HTTP(1),
    SOCKS(2);

    // TODO This should be in the data layer as the domain doesn't care of how this is persisted
    companion object {
        fun fromValue(value: Int?): ProxyType? {
            return when (value) {
                HTTP.value -> HTTP
                SOCKS.value -> SOCKS
                else -> null
            }
        }
    }
}