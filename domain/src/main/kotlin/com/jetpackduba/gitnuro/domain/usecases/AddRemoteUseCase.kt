package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.exceptions.InvalidRemoteUrlException
import com.jetpackduba.gitnuro.domain.interfaces.IAddRemoteGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IUpdateRemoteGitAction
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.RemoteSetUrlCommand
import javax.inject.Inject

class AddRemoteUseCase @Inject constructor(
    val tabState: TabInstanceRepository,
    private val addRemoteGitAction: IAddRemoteGitAction,
    private val updateRemoteGitAction: IUpdateRemoteGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val tabCoroutineScope: TabCoroutineScope,
//    private val appStateRepository: AppStateRepository,
    private val refreshRemotesUseCase: RefreshRemotesUseCase,
) {
    operator fun invoke(remote: Remote) {
        val repository: String = "" // TODO appStateRepository.repositoryPath ?: return

        tabCoroutineScope.launch {
            useCaseExecutor(
                taskType = TaskType.ADD_REMOTE,
            ) {
                if (remote.fetchUri.isBlank()) {
                    throw InvalidRemoteUrlException("Invalid empty fetch URI")
                }

                if (remote.pushUri.isBlank()) {
                    throw InvalidRemoteUrlException("Invalid empty push URI")
                }

                addRemoteGitAction(repository, remote.remoteName, remote.fetchUri)

                updateRemoteGitAction(
                    repositoryPath = repository,
                    remoteName = remote.remoteName,
                    uri = remote.fetchUri,
                    uriType = RemoteSetUrlCommand.UriType.FETCH
                )

                updateRemoteGitAction(
                    repositoryPath = repository,
                    remoteName = remote.remoteName,
                    uri = remote.pushUri,
                    uriType = RemoteSetUrlCommand.UriType.PUSH
                )
            }

            refreshRemotesUseCase()
        }
    }
}