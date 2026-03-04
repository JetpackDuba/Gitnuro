package com.jetpackduba.gitnuro.domain.models

data class Remote(
    val remoteName: String,
    val fetchUri: String,
    val pushUri: String,
)

fun newRemoteWrapper(): Remote {
    return Remote(
        remoteName = "",
        fetchUri = "",
        pushUri = "",
    )
}