package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.Pagination
import com.jetpackduba.gitnuro.domain.RebaseConstants
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.flatten
import com.jetpackduba.gitnuro.domain.errors.mapOk
import com.jetpackduba.gitnuro.domain.interfaces.*
import com.jetpackduba.gitnuro.domain.models.RebaseInteractiveState
import com.jetpackduba.gitnuro.domain.models.RepositoryState
import com.jetpackduba.gitnuro.domain.repositories.DataState
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.RebaseCommand
import java.io.File
import javax.inject.Inject

class RefreshDataUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val getBranchesGitAction: IGetBranchesGitAction,
    private val getCurrentBranchGitAction: IGetCurrentBranchGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val getStashListGitAction: IGetStashListGitAction,
    private val loadAuthorGitAction: ILoadAuthorGitAction,
    private val getStatusGitAction: IGetStatusGitAction,
    private val getRemotesUseCase: GetRemotesUseCase,
    private val getSubmodulesGitAction: IGetSubmodulesGitAction,
    private val getTagsGitAction: IGetTagsGitAction,
    private val getRepositoryState: IGetRepositoryStateGitAction,
    private val getRebaseInteractiveTodoLinesUseCase: GetRebaseInteractiveTodoLinesUseCase,
    private val getRebaseLinesFullMessageUseCase: GetRebaseLinesFullMessageUseCase,
    private val getLogUseCase: GetLogUseCase,
    private val scope: TabCoroutineScope,
) {
    operator fun invoke(vararg dataToRefresh: DataToRefresh) {
        val isRefreshAll = dataToRefresh.contains(DataToRefresh.ALL)

        scope.launch {
            repositoryStateRepository.refreshTriggered(dataToRefresh.toList())
        }

        if (isRefreshAll || dataToRefresh.contains(DataToRefresh.BRANCHES)) {
            refreshBranches()
        }

        if (isRefreshAll || dataToRefresh.contains(DataToRefresh.LOG)) {
            refreshLog()
        }

        if (isRefreshAll || dataToRefresh.contains(DataToRefresh.STASHES)) {
            refreshStashes()
        }

        if (isRefreshAll || dataToRefresh.contains(DataToRefresh.STATUS)) {
            refreshStatus()
        }

        if (isRefreshAll || dataToRefresh.contains(DataToRefresh.GIT_CONFIG)) {
            refreshGitConfig()
        }

        if (isRefreshAll || dataToRefresh.contains(DataToRefresh.REMOTES)) {
            refreshRemotes()
        }

        if (isRefreshAll || dataToRefresh.contains(DataToRefresh.SUBMODULES)) {
            refreshSubmodules()
        }

        if (isRefreshAll || dataToRefresh.contains(DataToRefresh.TAGS)) {
            refreshTags()
        }

        if (isRefreshAll || dataToRefresh.contains(DataToRefresh.REPO_STATE)) {
            refreshRepositoryState()
        }
    }

    private fun refreshBranches() {
        useCaseExecutor.executeOnTabScope { repositoryPath ->
            repositoryDataRepository.updateLocalBranches {
                getBranchesGitAction(repositoryPath)
            }

            repositoryDataRepository.updateCurrentBranch {
                getCurrentBranchGitAction(repositoryPath)
            }
        }
    }

    private fun refreshStashes() {
        useCaseExecutor.executeOnTabScope { repositoryPath ->
            repositoryDataRepository.updateStashes { getStashListGitAction(repositoryPath) }
        }
    }

    private fun refreshLog() {
        useCaseExecutor.executeOnTabScope { repositoryPath ->
            repositoryDataRepository.updateLog {
                getLogUseCase(repositoryPath, pagination = Pagination.None)
            }
        }
    }

    private fun refreshStatus() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            repositoryDataRepository.updateStatus {
                getStatusGitAction(repositoryPath)
            }
        }
    }

    private fun refreshGitConfig() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            repositoryDataRepository.updateAuthor {
                loadAuthorGitAction(repositoryPath)
            }
        }
    }

    private fun refreshRemotes() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            repositoryDataRepository.updateRemotes { getRemotesUseCase() }
        }
    }

    private fun refreshSubmodules() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            repositoryDataRepository.updateSubmodules {
                getSubmodulesGitAction(repositoryPath)
            }
        }
    }

    private fun refreshTags() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            repositoryDataRepository.updateTags {
                getTagsGitAction(repositoryPath)
            }
        }
    }

    private fun refreshRepositoryState() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            repositoryDataRepository.updateRepositoryState {
                getRepositoryState(repositoryPath)
            }

            val state = (repositoryDataRepository.repositoryState.value as? DataState.Loaded<RepositoryState>)?.data

            if (state == RepositoryState.REBASING_INTERACTIVE) {
                // TODO Error local handling or keep as it is?

                // TODO is this check necessary with this newer arch?
//            val isSameRebase = isSameRebase(rebaseLines, _rebaseState.value)

//            if (!isSameRebase) {
//                return@either Either.Ok(RebaseInteractiveViewState.Loaded(rebaseLines, messages))
//                val firstLine = rebaseLines.firstOrNull()
// TODO Check what is this logic for and if still necessary
//                if (firstLine != null) {
//                    val fullCommit = getCommitFromRebaseLineUseCase(firstLine.commit, firstLine.shortMessage)
//                    tabState.newSelectedCommit(fullCommit)
//                }
//            }

                repositoryDataRepository.updateRebaseInteractiveState {
                    val rebaseMergeDir = File(repositoryPath, RebaseConstants.REBASE_MERGE)
                    val doneFile = File(rebaseMergeDir, RebaseConstants.DONE)
                    val stoppedShaFile = File(rebaseMergeDir, RebaseConstants.STOPPED_SHA)

                    val newState = when {
                        !rebaseMergeDir.exists() -> RebaseInteractiveState.None
                        doneFile.exists() || stoppedShaFile.exists() -> {
                            val commitId: String? = getRebaseAmendCommitId(repositoryPath)

                            RebaseInteractiveState.ProcessingCommits(commitId)
                        }

                        else -> {
                            val lines = getRebaseInteractiveTodoLinesUseCase()
                                .mapOk { originalLines ->
                                    getRebaseLinesFullMessageUseCase(originalLines)
                                }
                                .flatten()

                            when (lines) {
                                is Either.Err -> return@updateRebaseInteractiveState lines
                                is Either.Ok -> RebaseInteractiveState.AwaitingInteraction(lines.value)
                            }
                        }
                    }

                    Either.Ok(newState)
                }
            } else {
                repositoryDataRepository.updateRebaseInteractiveState {
                    Either.Ok(RebaseInteractiveState.None)
                }
            }
        }
    }

    private fun getRebaseAmendCommitId(repository: String): String? {
        val amendFile = File(repository, "${RebaseCommand.REBASE_MERGE}/${RebaseConstants.AMEND}")

        return if (!amendFile.exists()) {
            null
        } else {
            amendFile.readText().removeSuffix("\n").removeSuffix("\r\n")
        }
    }
}

enum class DataToRefresh {
    ALL,
    BRANCHES,
    GIT_CONFIG,
    LOG,
    REMOTES,
    REPO_STATE,
    STASHES,
    STATUS,
    SUBMODULES,
    TAGS,
}