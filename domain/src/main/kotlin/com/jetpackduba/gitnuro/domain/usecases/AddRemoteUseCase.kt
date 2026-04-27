package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.exceptions.InvalidRemoteUrlException
import com.jetpackduba.gitnuro.domain.interfaces.IAddRemoteGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IUpdateRemoteGitAction
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import org.eclipse.jgit.api.RemoteSetUrlCommand
import javax.inject.Inject

class AddRemoteUseCase @Inject constructor(
    val tabState: TabInstanceRepository,
    private val addRemoteGitAction: IAddRemoteGitAction,
    private val updateRemoteGitAction: IUpdateRemoteGitAction,
    private val useCaseExecutor: UseCaseExecutor,
    private val refreshRemotesUseCase: RefreshRemotesUseCase,
) {
    operator fun invoke(remote: Remote) {
        useCaseExecutor.executeLaunch(
            taskType = TaskType.ADD_REMOTE,
            onRefresh = {
                refreshRemotesUseCase()
            }
        ) { repositoryPath ->
            if (remote.fetchUri.isBlank()) {
                throw InvalidRemoteUrlException("Invalid empty fetch URI")
            }

            if (remote.pushUri.isBlank()) {
                throw InvalidRemoteUrlException("Invalid empty push URI")
            }

            addRemoteGitAction(
                repositoryPath = repositoryPath,
                remoteName = remote.name,
                fetchUri = remote.fetchUri
            ).bind()

            updateRemoteGitAction(
                repositoryPath = repositoryPath,
                remoteName = remote.name,
                uri = remote.fetchUri,
                uriType = RemoteSetUrlCommand.UriType.FETCH
            ).bind()

            updateRemoteGitAction(
                repositoryPath = repositoryPath,
                remoteName = remote.name,
                uri = remote.pushUri,
                uriType = RemoteSetUrlCommand.UriType.PUSH
            )
        }
    }
}