package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.models.RebaseInteractiveState
import com.jetpackduba.gitnuro.domain.interfaces.IContinueRebaseGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IDoCommitGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetRebaseInteractiveStateGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetRepositoryStateGitAction
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.RepositoryState
import javax.inject.Inject

class RebaseContinueUseCase @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val getRepositoryStateGitAction: IGetRepositoryStateGitAction,
    private val getRebaseInteractiveStateGitAction: IGetRebaseInteractiveStateGitAction,
    private val continueRebaseGitAction: IContinueRebaseGitAction,
    private val doCommitGitAction: IDoCommitGitAction,
) {
    operator fun invoke(
        message: String,
        isAmendRebaseInteractive: Boolean,
        personIdent: suspend (Git) -> PersonIdent?,
    ) = tabState.safeProcessing(
        refreshType = RefreshType.ALL_DATA,
        taskType = TaskType.CONTINUE_REBASE,
    ) { git ->
        val repositoryState = getRepositoryStateGitAction(git)
        val rebaseInteractiveState = getRebaseInteractiveStateGitAction(git)

        if (
            repositoryState == RepositoryState.REBASING_INTERACTIVE &&
            rebaseInteractiveState is RebaseInteractiveState.ProcessingCommits &&
            rebaseInteractiveState.isCurrentStepAmenable &&
            isAmendRebaseInteractive
        ) {
            val amendCommitId = rebaseInteractiveState.commitToAmendId

            if (!amendCommitId.isNullOrBlank()) {
                doCommitGitAction(git, message, true, personIdent(git))
            }
        }

        continueRebaseGitAction(git)

        null
    }

}