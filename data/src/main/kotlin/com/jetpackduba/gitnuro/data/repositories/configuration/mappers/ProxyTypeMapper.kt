package com.jetpackduba.gitnuro.data.repositories.configuration.mappers

import com.jetpackduba.gitnuro.data.mappers.DataMapper
import com.jetpackduba.gitnuro.domain.models.ProxyType
import javax.inject.Inject

private const val HTTP = 1
private const val SOCKS = 2

class ProxyTypeMapper @Inject constructor(): DataMapper<ProxyType, Int> {
    override fun toData(value: ProxyType): Int {
        return when (value) {
            ProxyType.HTTP -> HTTP
            ProxyType.SOCKS -> SOCKS
        }
    }

    override fun toDomain(value: Int): ProxyType {
        return when (value) {
            HTTP -> ProxyType.HTTP
            SOCKS -> ProxyType.SOCKS
            else -> throw IllegalArgumentException("Unsupported proxy type")
        }
    }
}