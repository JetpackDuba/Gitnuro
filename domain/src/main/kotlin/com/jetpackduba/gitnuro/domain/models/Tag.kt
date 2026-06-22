package com.jetpackduba.gitnuro.domain.models

data class Tag(
    val commitHash: String,
    val hash: String,
    val name: String,
) {
    val simpleName: String
        get() = name.removePrefix("refs/tags/")
}