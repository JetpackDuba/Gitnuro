package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.errors.either
import com.jetpackduba.gitnuro.domain.interfaces.*
import com.jetpackduba.gitnuro.domain.models.RepositoryState
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.max

private const val INITIAL_COMMITS_LOAD = 2000

class RefreshDataUseCase @Inject constructor(
    private val useCaseExecutor: UseCaseExecutor,
    private val getBranchesGitAction: IGetBranchesGitAction,
    private val getCurrentBranchGitAction: IGetCurrentBranchGitAction,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val repositoryStateRepository: RepositoryStateRepository,
    private val getLogGitAction: IGetLogGitAction,
    private val getCurrentBranchAction: IGetCurrentBranchGitAction,
    private val getStashListGitAction: IGetStashListGitAction,
    private val loadAuthorGitAction: ILoadAuthorGitAction,
    private val getStatusGitAction: IGetStatusGitAction,
    private val getRemotesUseCase: GetRemotesUseCase,
    private val getSubmodulesGitAction: IGetSubmodulesGitAction,
    private val getTagsGitAction: IGetTagsGitAction,
    private val getRepositoryState: IGetRepositoryStateGitAction,
    private val getRebaseInteractiveTodoLinesUseCase: GetRebaseInteractiveTodoLinesUseCase,
    private val getRebaseLinesFullMessageUseCase: GetRebaseLinesFullMessageUseCase,
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
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            val branches = getBranchesGitAction(repositoryPath).bind()
            repositoryDataRepository.updateLocalBranches(branches)

            val currentBranch = getCurrentBranchGitAction(repositoryPath).bind()
            repositoryDataRepository.updateCurrentBranch(currentBranch)
        }
    }

    private fun refreshStashes() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            val stashes = getStashListGitAction(repositoryPath).bind()
            repositoryDataRepository.updateStashes(stashes)
        }
    }

    private fun refreshLog() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            val log = loadLog(repositoryPath).bind()
            repositoryDataRepository.updateLog(log)
        }
    }

    private suspend fun loadLog(repository: String) = either {
        val status = getStatusGitAction(repository).bind()
        val currentBranch = getCurrentBranchAction(repository).bind()

        getLogGitAction(
            repository,
            currentBranch,
            hasUncommittedChanges = status.staged.isNotEmpty() || status.unstaged.isNotEmpty(),
            commitsLimit = max(repositoryDataRepository.maxCommitsToLoadLimit, INITIAL_COMMITS_LOAD),
            isPaginated = false,
        )
    }

    private fun refreshStatus() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            val status = getStatusGitAction(repositoryPath).bind()
            repositoryDataRepository.updateStatus(status)
        }
    }

    private fun refreshGitConfig() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            val author = loadAuthorGitAction(repositoryPath).bind()
            repositoryDataRepository.updateAuthor(author)
        }
    }

    private fun refreshRemotes() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            val remotes = getRemotesUseCase()

            if (remotes is Either.Ok) {
                repositoryDataRepository.updateRemotes(remotes.value)
            }
        }
    }

    private fun refreshSubmodules() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            val submodules = getSubmodulesGitAction(repositoryPath).bind()
            repositoryDataRepository.updateSubmodules(submodules)
        }
    }

    private fun refreshTags() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            val tags = getTagsGitAction(repositoryPath).bind()
            repositoryDataRepository.updateTags(tags)
        }
    }

    private fun refreshRepositoryState() {
        useCaseExecutor.executeOnTabScope() { repositoryPath ->
            val state = getRepositoryState(repositoryPath).bind()

            repositoryDataRepository.updateRepositoryState(state)

            if (state == RepositoryState.REBASING_INTERACTIVE) {
                // TODO Error local handling or keep as it is?
                val originalLines = getRebaseInteractiveTodoLinesUseCase().bind()

                val fullLines = getRebaseLinesFullMessageUseCase(originalLines).bind()

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

                repositoryDataRepository.updateRebaseInteractiveState(fullLines)
            } else {
                repositoryDataRepository.updateRebaseInteractiveState(emptyList())
            }
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