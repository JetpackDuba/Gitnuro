package com.jetpackduba.gitnuro.data.git.remotes

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.data.mappers.RemoteConfigToRemoteMapper
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.interfaces.IGetRemotesGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.domain.models.RemoteInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class GetRemotesGitAction @Inject constructor(
    private val remoteMapper: RemoteConfigToRemoteMapper,
) : IGetRemotesGitAction {
    override suspend operator fun invoke(repositoryPath: String): Either<List<Remote>, GitError> =
        jgit(repositoryPath) {
            remoteList()
                .call()
                .map { remoteConfig -> remoteMapper.toDomain(remoteConfig)  }
        }
}