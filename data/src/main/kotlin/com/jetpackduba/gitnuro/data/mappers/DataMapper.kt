package com.jetpackduba.gitnuro.data.mappers

interface DataMapper<Domain, Data> {
    fun toData(value: Domain): Data
    fun toDomain(value: Data): Domain
    fun toDataOrNull(value: Domain?): Data? = value?.let { toData(value) }
    fun toDomainOrNull(value: Data?): Domain? = value?.let { toDomain(value) }
}