package com.jetpackduba.gitnuro.models

import org.eclipse.jgit.transport.RemoteConfig

data class RemoteWrapper constructor(
    val remoteName: String,
    val fetchUri: String,
    val pushUri: String,
    val isNew: Boolean,
)

fun RemoteConfig.toRemoteWrapper(): RemoteWrapper {
    val fetchUri = this.urIs.firstOrNull()
    val pushUri = this.pushURIs.firstOrNull()
        ?: this.urIs.firstOrNull() // If push URI == null, take fetch URI

    return RemoteWrapper(
        remoteName = this.name,
        fetchUri = fetchUri?.toString().orEmpty(),
        pushUri = pushUri?.toString().orEmpty(),
        isNew = false,
    )
}

fun newRemoteWrapper(): RemoteWrapper {
    return RemoteWrapper(
        remoteName = "",
        fetchUri = "",
        pushUri = "",
        isNew = true,
    )
}