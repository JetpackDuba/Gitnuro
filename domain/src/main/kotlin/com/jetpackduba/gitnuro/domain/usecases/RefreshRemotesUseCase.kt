package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.errors.either
import com.jetpackduba.gitnuro.domain.interfaces.IGetRemoteBranchesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetRemotesGitAction
import com.jetpackduba.gitnuro.domain.models.RemoteInfo
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import javax.inject.Inject

class RefreshRemotesUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val getRemotesGitAction: IGetRemotesGitAction,
    private val getRemoteBranchesGitAction: IGetRemoteBranchesGitAction,
) {
    suspend operator fun invoke() {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return
        when (val remoteInfoList = getRemoteInfoList(repositoryPath)) {
            is Either.Err -> {
                // TODO Add error?
            }

            is Either.Ok -> {
                repositoryDataRepository.updateRemotes(remoteInfoList.value)
            }
        }
    }

    private suspend fun getRemoteInfoList(repositoryPath: String) = either<List<RemoteInfo>, GitError> {
        val allRemoteBranches = getRemoteBranchesGitAction(repositoryPath).bind()
        val remoteInfoList = getRemotesGitAction(repositoryPath).bind()

        val remotes = remoteInfoList.map { remote ->
            val remoteBranches = allRemoteBranches.filter { branch ->
                branch.name.startsWith("refs/remotes/${remote.name}")
            }
            RemoteInfo(remote, remoteBranches)
        }

        Either.Ok(remotes)
    }
}