package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteLocallyRemoteBranchesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteRemoteGitAction
import com.jetpackduba.gitnuro.domain.models.RemoteInfo
import com.jetpackduba.gitnuro.domain.models.TaskType
import javax.inject.Inject

class DeleteRemoteInfoUseCase @Inject constructor(
    private val refreshAllUseCase: RefreshAllUseCase,
    private val deleteRemoteGitAction: IDeleteRemoteGitAction,
    private val deleteLocallyRemoteBranchesGitAction: IDeleteLocallyRemoteBranchesGitAction,
    private val useCaseExecutor: UseCaseExecutor,

    ) {
    operator fun invoke(remoteInfo: RemoteInfo) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.DeleteRemote,
            onRefresh = { refreshAllUseCase() }
        ) { repositoryPath ->
            deleteRemoteGitAction(repositoryPath, remoteInfo.remote.name)

            val remoteBranchesToDelete = remoteInfo.branchesList

            deleteLocallyRemoteBranchesGitAction(repositoryPath, remoteBranchesToDelete.map { it.name })
        }
    }
}
