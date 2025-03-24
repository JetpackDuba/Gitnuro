package com.jetpackduba.gitnuro.models.lfs

import kotlinx.serialization.Serializable

@Serializable
data class LfsSshAuthenticateResult(
    val href: String,
    val header: Map<String, String>,
)