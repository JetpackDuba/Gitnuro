package com.jetpackduba.gitnuro.ui.diff

import androidx.compose.foundation.lazy.LazyListState
import com.jetpackduba.gitnuro.SharedRepositoryStateManager
import com.jetpackduba.gitnuro.domain.exceptions.MissingDiffEntryException
import com.jetpackduba.gitnuro.extensions.delayedStateChange
import com.jetpackduba.gitnuro.domain.repositories.CloseableView
import com.jetpackduba.gitnuro.domain.git.DiffType
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.git.diff.*
import com.jetpackduba.gitnuro.domain.git.workspace.*
import com.jetpackduba.gitnuro.data.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.data.repositories.SelectedDiffItemRepository
import com.jetpackduba.gitnuro.domain.models.TextDiffType
import com.jetpackduba.gitnuro.system.OpenFileInExternalAppGitAction
import com.jetpackduba.gitnuro.ui.TabsManager
import com.jetpackduba.gitnuro.viewmodels.ViewDiffResult
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
    private val formatDiffGitAction: FormatDiffGitAction,
    private val stageHunkGitAction: StageHunkGitAction,
    private val unstageHunkGitAction: UnstageHunkGitAction,
    private val stageHunkLineGitAction: StageHunkLineGitAction,
    private val unstageHunkLineGitAction: UnstageHunkLineGitAction,
    private val resetHunkGitAction: ResetHunkGitAction,
    private val stageEntryGitAction: StageEntryGitAction,
    private val unstageEntryGitAction: UnstageEntryGitAction,
    private val openFileInExternalAppGitAction: OpenFileInExternalAppGitAction,
    private val settings: AppSettingsRepository,
    private val generateSplitHunkFromDiffResultGitAction: GenerateSplitHunkFromDiffResultGitAction,
    private val discardUnstagedHunkLineGitAction: DiscardUnstagedHunkLineGitAction,
    private val tabsManager: TabsManager,
    private val tabScope: CoroutineScope,
    private val selectedDiffTypeRepository: SelectedDiffItemRepository,
    private val sharedRepositoryStateManager: SharedRepositoryStateManager,
) {
    private val _diffResult = MutableStateFlow<ViewDiffResult>(ViewDiffResult.None)
    val diffResult: StateFlow<ViewDiffResult?> = _diffResult

    val closeViewFlow = tabState.closeViewFlow

    val diffTypeFlow = settings.textDiffTypeFlow
    val isDisplayFullFile = settings.diffDisplayFullFileFlow

    val isRepositoryInSafeState = sharedRepositoryStateManager.repositoryState
        .map { it == RepositoryState.SAFE }

    private var diffType: DiffType? = null
    private var diffJob: Job? = null

    init {
        tabScope.launch {
            diffTypeFlow
                .drop(1) // Ignore the first time the flow triggers, we only care about updates
                .collect {
                    val diffEntryType = this@DiffViewModel.diffType
                    if (diffEntryType != null) {
                        updateDiff(diffEntryType)
                    }
                }
        }

        tabScope.launch {
            isDisplayFullFile
                .drop(1) // Ignore the first time the flow triggers, we only care about updates
                .collect {
                    val diffEntryType = this@DiffViewModel.diffType
                    if (diffEntryType != null) {
                        updateDiff(diffEntryType)
                    }
                }
        }

        tabScope.launch {
            tabState.refreshFlowFiltered(
                RefreshType.UNCOMMITTED_CHANGES,
                RefreshType.UNCOMMITTED_CHANGES_AND_LOG,
            ) {
                val diffResultValue = diffResult.value
                if (diffResultValue is ViewDiffResult.Loaded) {
                    updateDiff(diffResultValue.diffType)
                }
            }
        }

        tabScope.launch {
            selectedDiffTypeRepository.diffSelected.collectLatest { diffSelected ->
                if (diffSelected != null && diffSelected.entries.count() == 1) {
                    updateDiff(diffSelected.entries.first())
                } else {
                    reset()
                }
            }
        }
    }

    val lazyListState = MutableStateFlow(
        LazyListState(
            0,
            0
        )
    )

    private fun updateDiff(diffType: DiffType) {
        addToCloseables()

        diffJob = tabState.runOperation(refreshType = RefreshType.NONE) { git ->
            this.diffType = diffType

            var oldDiffType: DiffType? = null
            val oldDiffResult = _diffResult.value

            if (oldDiffResult is ViewDiffResult.Loaded) {
                oldDiffType = oldDiffResult.diffType
            }

            // If it's a different file or different state (index or workdir), reset the scroll state
            if (
                oldDiffType?.filePath != diffType.filePath ||
                oldDiffType is DiffType.UncommittedDiff &&
                diffType is DiffType.UncommittedDiff &&
                oldDiffType.statusEntry.filePath == diffType.statusEntry.filePath &&
                oldDiffType::class != diffType::class
            ) {
                lazyListState.value = LazyListState(
                    0,
                    0
                )
            }

            val isFirstLoad = oldDiffResult is ViewDiffResult.Loading && oldDiffResult.diffType.filePath.isEmpty()

            try {
                delayedStateChange(
                    delayMs = if (isFirstLoad) 0 else DIFF_MIN_TIME_IN_MS_TO_SHOW_LOAD,
                    onDelayTriggered = { _diffResult.value = ViewDiffResult.Loading(diffType) }
                ) {
                    val diffFormat = formatDiffGitAction(git, diffType, isDisplayFullFile.value)
                    val diffEntry = diffFormat.diffEntry
                    if (
                        diffTypeFlow.value == TextDiffType.SPLIT &&
                        diffFormat is DiffResult.Text &&
                        diffEntry.changeType != DiffEntry.ChangeType.ADD &&
                        diffEntry.changeType != DiffEntry.ChangeType.DELETE
                    ) {
                        val splitHunkList = generateSplitHunkFromDiffResultGitAction(diffFormat)
                        _diffResult.value = ViewDiffResult.Loaded(
                            diffType,
                            DiffResult.TextSplit(diffEntry, splitHunkList)
                        )
                    } else {
                        _diffResult.value = ViewDiffResult.Loaded(diffType, diffFormat)
                    }
                }
            } catch (ex: Exception) {
                if (ex is MissingDiffEntryException) {
                    tabState.refreshData(refreshType = RefreshType.UNCOMMITTED_CHANGES)
                    _diffResult.value = ViewDiffResult.DiffNotFound(diffType)
                } else
                    ex.printStackTrace()
            }
        }
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

    fun stageFile(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        stageEntryGitAction(git, statusEntry)
    }

    fun unstageFile(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        unstageEntryGitAction(git, statusEntry)
    }

    fun cancelRunningJobs() {
        diffJob?.cancel()
    }

    fun changeTextDiffType(newDiffType: TextDiffType) {
        settings.textDiffType = newDiffType
    }

    fun changeDisplayFullFile(isDisplayFullFile: Boolean) {
        settings.diffDisplayFullFile = isDisplayFullFile
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
        val diff = when (val state = _diffResult.value) {
            is ViewDiffResult.DiffNotFound -> state.diff
            is ViewDiffResult.Loaded -> state.diffType
            is ViewDiffResult.Loading -> state.diffType
            ViewDiffResult.None -> null
        }

        if (diff != null) {
            when (diff) {
                is DiffType.CommitDiff -> selectedDiffTypeRepository.removeSelectedCommited(setOf(diff))
                is DiffType.UncommittedDiff -> selectedDiffTypeRepository.removeSelectedUncommited(
                    setOf(diff),
                    diff.entryType,
                )
            }
        }
    }
}

