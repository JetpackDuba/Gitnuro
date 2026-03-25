package com.jetpackduba.gitnuro.ui.diff

import androidx.compose.foundation.lazy.LazyListState
import com.jetpackduba.gitnuro.SharedRepositoryStateManager
import com.jetpackduba.gitnuro.domain.interfaces.*
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.repositories.CloseableView
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import com.jetpackduba.gitnuro.domain.usecases.AddSelectedDiffUseCase
import com.jetpackduba.gitnuro.domain.usecases.RemoveSelectedDiffUseCase
import com.jetpackduba.gitnuro.domain.usecases.StatusStageUseCase
import com.jetpackduba.gitnuro.domain.usecases.StatusUnstageUseCase
import com.jetpackduba.gitnuro.system.OpenFileInExternalAppGitAction
import com.jetpackduba.gitnuro.ui.TabsManager
import com.jetpackduba.gitnuro.domain.models.ViewDiffResult
import com.jetpackduba.gitnuro.domain.usecases.GetDiffUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.RepositoryState
import javax.inject.Inject

private const val DIFF_MIN_TIME_IN_MS_TO_SHOW_LOAD = 200L

class DiffViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val formatDiffGitAction: IFormatDiffGitAction,
    private val stageHunkGitAction: IStageHunkGitAction,
    private val unstageHunkGitAction: IUnstageHunkGitAction,
    private val stageHunkLineGitAction: IStageHunkLineGitAction,
    private val unstageHunkLineGitAction: IUnstageHunkLineGitAction,
    private val resetHunkGitAction: IResetHunkGitAction,
    private val statusStageUseCase: StatusStageUseCase,
    private val statusUnstageUseCase: StatusUnstageUseCase,
    private val generateSplitHunkFromDiffResultGitAction: IGenerateSplitHunkFromDiffResultGitAction,
    private val discardUnstagedHunkLineGitAction: IDiscardUnstagedHunkLineGitAction,
    private val openFileInExternalAppGitAction: OpenFileInExternalAppGitAction,
    private val settings: AppSettingsService,
    private val tabsManager: TabsManager,
    private val tabScope: CoroutineScope,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val removeSelectedDiffUseCase: RemoveSelectedDiffUseCase,
    private val addSelectedDiffUseCase: AddSelectedDiffUseCase,
    private val getDiffUseCase: GetDiffUseCase,
    private val sharedRepositoryStateManager: SharedRepositoryStateManager,
) {
    private val _diffResult = MutableStateFlow<ViewDiffResult>(ViewDiffResult.None)
//    val diffResult: StateFlow<ViewDiffResult?> = _diffResult
    val diffResult: StateFlow<ViewDiffResult?> = repositoryDataRepository.diffSelected.map { diffSelected ->
        if (diffSelected?.entries?.count() == 1) {
            val diff = loadDiff(diffSelected.entries.first())

            if (diff is ViewDiffResult.Loaded) {
                addToCloseables()
            }

            diff
        } else {
            ViewDiffResult.DiffNotFound(null)
        }
    }.stateIn(
        tabScope,
        started = SharingStarted.Lazily,
        initialValue = null,
    )

    val closeViewFlow = tabState.closeViewFlow

    val diffTypeFlow = settings.diffTextViewType
    val isDisplayFullFile = settings.diffDisplayFullFile

    val isRepositoryInSafeState = sharedRepositoryStateManager.repositoryState
        .map { it == RepositoryState.SAFE }

    private var diffJob: Job? = null

    val lazyListState = MutableStateFlow(
        LazyListState(
            0,
            0
        )
    )

    private suspend fun loadDiff(diffType: DiffType): ViewDiffResult {
        return getDiffUseCase(diffType)
    }

    fun stageHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
    ) { git ->
        stageHunkGitAction(git, diffEntry, hunk)
    }

    fun resetHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        resetHunkGitAction(git, diffEntry, hunk)
    }

    fun unstageHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
    ) { git ->
        unstageHunkGitAction(git, diffEntry, hunk)
    }

    fun stageFile(statusEntry: StatusEntry) = statusStageUseCase(statusEntry)

    fun unstageFile(statusEntry: StatusEntry) = statusUnstageUseCase(statusEntry)

    fun cancelRunningJobs() {
        diffJob?.cancel()
    }

    fun changeTextDiffType(newDiffType: DiffTextViewType) = tabScope.launch {
        settings.setConfiguration(AppConfig.DiffTextViewType(newDiffType))
    }

    fun changeDisplayFullFile(isDisplayFullFile: Boolean) = tabScope.launch {
        settings.setConfiguration(AppConfig.DiffDisplayFullFile(isDisplayFullFile))
    }

    fun stageHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        stageHunkLineGitAction(git, entry, hunk, line)
    }

    fun unstageHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        unstageHunkLineGitAction(git, entry, hunk, line)
    }

    fun openFileWithExternalApp(path: String) {
        openFileInExternalAppGitAction(path)
    }

    fun discardHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        discardUnstagedHunkLineGitAction(git, entry, hunk, line)
    }

    fun openSubmodule(path: String) = tabState.runOperation(refreshType = RefreshType.NONE) { git ->
        tabsManager.addNewTabFromPath("${git.repository.workTree}/$path", true)
    }

    fun addToCloseables() = tabScope.launch {
        tabState.addCloseableView(CloseableView.DIFF)
    }

    private fun removeFromCloseables() = tabScope.launch {
        tabState.removeCloseableView(CloseableView.DIFF)
    }

    fun reset() {
        cancelRunningJobs()
        removeFromCloseables()
        _diffResult.value = ViewDiffResult.None
    }

    fun clearDiff() {
        val diff = when (val state = diffResult.value) {
            is ViewDiffResult.DiffNotFound -> state.diff
            is ViewDiffResult.Loaded -> state.diffType
            is ViewDiffResult.Loading -> state.diffType
            else -> null
        }

        if (diff != null) {
            when (diff) {
                is DiffType.CommitDiff -> removeSelectedDiffUseCase(setOf(diff))
                is DiffType.UncommittedDiff -> removeSelectedDiffUseCase(
                    setOf(diff),
                    diff.entryType,
                )
            }
        }
    }
}

