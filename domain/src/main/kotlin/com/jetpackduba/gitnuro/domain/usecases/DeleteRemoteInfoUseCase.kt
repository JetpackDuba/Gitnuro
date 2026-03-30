package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.extensions.runOperationInTabScope
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteLocallyRemoteBranchesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteRemoteGitAction
import com.jetpackduba.gitnuro.domain.models.RemoteInfo
import com.jetpackduba.gitnuro.domain.models.positiveNotification
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import javax.inject.Inject

class DeleteRemoteInfoUseCase @Inject constructor(
    private val repositoryDataRepository: RepositoryDataRepository,
    private val refreshAllUseCase: RefreshAllUseCase,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val deleteRemoteGitAction: IDeleteRemoteGitAction,
    private val deleteLocallyRemoteBranchesGitAction: IDeleteLocallyRemoteBranchesGitAction,
    private val tabCoroutineScope: TabCoroutineScope,

    ) {
    operator fun invoke(remoteInfo: RemoteInfo) {
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return

        repositoryStateRepository.runOperationInTabScope(tabCoroutineScope) {
            deleteRemoteGitAction(repositoryPath, remoteInfo.remote.name)

            val remoteBranchesToDelete = remoteInfo.branchesList

            deleteLocallyRemoteBranchesGitAction(repositoryPath, remoteBranchesToDelete.map { it.name })

//            positiveNotification("Remote ${remoteInfo.remote.name} deleted")()
        }

        refreshAllUseCase()

        /*tabState.safeProcessing(
            refreshType = RefreshType.ALL_DATA,
            taskType = TaskType.DELETE_REMOTE,
        ) { git ->
            deleteRemoteGitAction(git, remoteInfo.remote.name)

            val remoteBranchesToDelete = remoteInfo.branchesList

            deleteLocallyRemoteBranchesGitAction(git, remoteBranchesToDelete.map { it.name })

            positiveNotification("Remote ${remoteInfo.remote.name} deleted")
        }*/
    }
}