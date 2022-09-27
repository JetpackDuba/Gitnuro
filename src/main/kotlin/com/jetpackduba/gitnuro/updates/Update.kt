package com.jetpackduba.gitnuro.updates

import kotlinx.serialization.Serializable

@Serializable
data class Update(
    val appVersion: String,
    val appCode: Int,
    val downloadUrl: String,
)
