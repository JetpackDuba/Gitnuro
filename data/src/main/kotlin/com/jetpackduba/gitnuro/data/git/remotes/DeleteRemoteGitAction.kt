package com.jetpackduba.gitnuro.data.git.remotes

import com.jetpackduba.gitnuro.domain.interfaces.IDeleteRemoteGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class DeleteRemoteGitAction @Inject constructor() : IDeleteRemoteGitAction {
    override suspend operator fun invoke(git: Git, remoteName: String): Unit = withContext(Dispatchers.IO) {
        git.remoteRemove()
            .setRemoteName(remoteName)
            .call()
    }
}