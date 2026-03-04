package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.exceptions.InvalidRemoteUrlException
import com.jetpackduba.gitnuro.domain.git.remotes.AddRemoteGitAction
import com.jetpackduba.gitnuro.domain.git.remotes.UpdateRemoteGitAction
import com.jetpackduba.gitnuro.domain.models.Remote
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import org.eclipse.jgit.api.RemoteSetUrlCommand
import javax.inject.Inject

class AddRemoteUseCase @Inject constructor(
    val tabState: TabInstanceRepository,
    private val addRemoteGitAction: AddRemoteGitAction,
    private val updateRemoteGitAction: UpdateRemoteGitAction,
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

            addRemoteGitAction(git, remote.remoteName, remote.fetchUri)

            updateRemoteGitAction(
                git = git,
                remoteName = remote.remoteName,
                uri = remote.fetchUri,
                uriType = RemoteSetUrlCommand.UriType.FETCH
            )

            updateRemoteGitAction(
                git = git,
                remoteName = remote.remoteName,
                uri = remote.pushUri,
                uriType = RemoteSetUrlCommand.UriType.PUSH
            )
        }
    }
}