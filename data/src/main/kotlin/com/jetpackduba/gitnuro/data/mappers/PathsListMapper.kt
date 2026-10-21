package com.jetpackduba.gitnuro.data.mappers

import kotlinx.serialization.json.Json
import javax.inject.Inject

class PathsListMapper @Inject constructor() : DataMapper<List<String>, String> {
    private val json = Json { ignoreUnknownKeys = true }

    override fun toData(value: List<String>): String {
        return json.encodeToString(value)
    }

    override fun toDomain(value: String): List<String> {
        return json.decodeFromString(value)
    }
}