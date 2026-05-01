package com.jetpackduba.gitnuro.data.git.remotes

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.mappers.RemoteConfigToRemoteMapper
import com.jetpackduba.gitnuro.domain.interfaces.IGetRemotesGitAction
import javax.inject.Inject

class GetRemotesGitAction @Inject constructor(
    private val remoteMapper: RemoteConfigToRemoteMapper,
    private val jgit: JGit,
) : IGetRemotesGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        git
            .remoteList()
            .call()
            .map { remoteConfig -> remoteMapper.toDomain(remoteConfig) }
    }
}