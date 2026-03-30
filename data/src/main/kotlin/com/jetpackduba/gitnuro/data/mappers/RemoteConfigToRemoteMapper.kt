package com.jetpackduba.gitnuro.data.mappers

import com.jetpackduba.gitnuro.domain.models.Remote
import org.eclipse.jgit.transport.RemoteConfig
import javax.inject.Inject

class RemoteConfigToRemoteMapper @Inject constructor(): DataMapper<Remote, RemoteConfig> {
    override fun toData(value: Remote): RemoteConfig {
        throw NotImplementedError("Remote to RemoteWrapper not implemented")
    }

    override fun toDomain(value: RemoteConfig): Remote {
        return value.toRemoteWrapper()
    }
}

// TODO remove this extension in the future after refactoring
fun RemoteConfig.toRemoteWrapper(): Remote {
    val fetchUri = this.urIs.firstOrNull()
    val pushUri = this.pushURIs.firstOrNull()
        ?: this.urIs.firstOrNull() // If push URI == null, take fetch URI

    return Remote(
        name = this.name,
        fetchUri = fetchUri?.toString().orEmpty(),
        pushUri = pushUri?.toString().orEmpty(),
    )
}
