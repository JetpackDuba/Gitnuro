package com.jetpackduba.gitnuro.models

import kotlinx.serialization.Serializable

@Serializable
data class CustomAction(
    val id: String,
    val name: String,
    val command: String,
    val icon: String = "bolt" // default icon name
)