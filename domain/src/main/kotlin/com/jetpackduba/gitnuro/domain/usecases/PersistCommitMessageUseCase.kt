package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.extensions.isMerging
import com.jetpackduba.gitnuro.domain.extensions.isReverting
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import org.eclipse.jgit.lib.RepositoryState
import javax.inject.Inject

class PersistCommitMessageUseCase @Inject constructor(
    private val tabState: TabInstanceRepository,
) {
    operator fun invoke(message: String) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        val messageToPersist = message.ifBlank { null }

        // TODO move logic to data layer
        if (git.repository.repositoryState.isMerging ||
            git.repository.repositoryState.isRebasing ||
            git.repository.repositoryState.isReverting
        ) {
            git.repository.writeMergeCommitMsg(messageToPersist)
        } else if (git.repository.repositoryState == RepositoryState.SAFE) {
            git.repository.writeCommitEditMsg(messageToPersist)
        }
    }
}