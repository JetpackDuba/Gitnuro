package com.jetpackduba.gitnuro.data.git.repository

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.extensions.isMerging
import com.jetpackduba.gitnuro.domain.extensions.isReverting
import com.jetpackduba.gitnuro.domain.interfaces.IPersistCommitMessageGitAction
import org.eclipse.jgit.lib.RepositoryState
import javax.inject.Inject

class PersistCommitMessageGitAction @Inject constructor(
    private val jgit: JGit,
) : IPersistCommitMessageGitAction {
    override suspend operator fun invoke(repositoryPath: String, message: String?) =
        jgit.provide(repositoryPath) { git ->
            val state = git.repository.repositoryState
            if (state.isMerging || state.isRebasing || state.isReverting) {
                git.repository.writeMergeCommitMsg(message)
            } else if (state == RepositoryState.SAFE) {
                git.repository.writeCommitEditMsg(message)
            }
        }
}
