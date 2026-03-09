package com.jetpackduba.gitnuro.data.mappers

interface DataMapper<T, R> {
    fun map(value: T): R
    fun map(value: R): T
}