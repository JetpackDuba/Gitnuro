package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import org.eclipse.jgit.api.RemoteSetUrlCommand
import org.eclipse.jgit.transport.RemoteConfig

interface IUpdateRemoteGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        remoteName: String,
        uri: String,
        uriType: RemoteSetUrlCommand.UriType
    ): Either<RemoteConfig?, GitError>
}