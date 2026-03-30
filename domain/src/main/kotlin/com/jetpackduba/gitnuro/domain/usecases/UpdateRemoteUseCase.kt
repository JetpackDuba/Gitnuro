package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.exceptions.InvalidRemoteUrlException
import com.jetpackduba.gitnuro.domain.interfaces.IUpdateRemoteGitAction
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import org.eclipse.jgit.api.RemoteSetUrlCommand
import javax.inject.Inject

class UpdateRemoteUseCase @Inject constructor(
    val tabState: TabInstanceRepository,
    private val updateRemoteGitAction: IUpdateRemoteGitAction,
) {
    operator fun invoke(remote: Remote) {
        tabState.runOperation(
            refreshType = RefreshType.REMOTES,
            showError = true,
        ) { git ->
            if (remote.fetchUri.isBlank()) {
                throw InvalidRemoteUrlException("Invalid empty fetch URI")
            }

            if (remote.pushUri.isBlank()) {
                throw InvalidRemoteUrlException("Invalid empty push URI")
            }

            updateRemoteGitAction(
                repositoryPath = git.repository.directory.absolutePath,
                remoteName = remote.name,
                uri = remote.fetchUri,
                uriType = RemoteSetUrlCommand.UriType.FETCH
            )

            updateRemoteGitAction(
                repositoryPath = git.repository.directory.absolutePath,
                remoteName = remote.name,
                uri = remote.pushUri,
                uriType = RemoteSetUrlCommand.UriType.PUSH
            )
        }
    }
}