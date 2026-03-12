package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.RemoteSetUrlCommand

interface IUpdateRemoteGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        remoteName: String,
        uri: String,
        uriType: RemoteSetUrlCommand.UriType
    ): Unit
}