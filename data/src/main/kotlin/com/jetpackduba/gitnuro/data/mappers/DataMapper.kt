package com.jetpackduba.gitnuro.data.mappers

interface DataMapper<T, R> {
    fun map(from: T): R
}