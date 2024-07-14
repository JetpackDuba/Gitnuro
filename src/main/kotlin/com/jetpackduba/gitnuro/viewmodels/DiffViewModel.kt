package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import com.jetpackduba.gitnuro.exceptions.MissingDiffEntryException
import com.jetpackduba.gitnuro.extensions.delayedStateChange
import com.jetpackduba.gitnuro.git.DiffType
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.diff.*
import com.jetpackduba.gitnuro.git.workspace.*
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.system.OpenFileInExternalAppUseCase
import com.jetpackduba.gitnuro.ui.TabsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

private const val DIFF_MIN_TIME_IN_MS_TO_SHOW_LOAD = 200L

class DiffViewModel @Inject constructor(
    private val tabState: TabState,
    private val formatDiffUseCase: FormatDiffUseCase,
    private val stageHunkUseCase: StageHunkUseCase,
    private val unstageHunkUseCase: UnstageHunkUseCase,
    private val stageHunkLineUseCase: StageHunkLineUseCase,
    private val unstageHunkLineUseCase: UnstageHunkLineUseCase,
    private val resetHunkUseCase: ResetHunkUseCase,
    private val stageEntryUseCase: StageEntryUseCase,
    private val unstageEntryUseCase: UnstageEntryUseCase,
    private val openFileInExternalAppUseCase: OpenFileInExternalAppUseCase,
    private val settings: AppSettingsRepository,
    private val generateSplitHunkFromDiffResultUseCase: GenerateSplitHunkFromDiffResultUseCase,
    private val discardUnstagedHunkLineUseCase: DiscardUnstagedHunkLineUseCase,
    private val tabsManager: TabsManager,
    tabScope: CoroutineScope,
) {
    private val _diffResult = MutableStateFlow<ViewDiffResult>(ViewDiffResult.Loading(""))
    val diffResult: StateFlow<ViewDiffResult?> = _diffResult

    val diffTypeFlow = settings.textDiffTypeFlow
    val isDisplayFullFile = settings.diffDisplayFullFileFlow

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
    }

    val lazyListState = MutableStateFlow(
        LazyListState(
            0,
            0
        )
    )

    fun updateDiff(diffType: DiffType) {
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

            val isFirstLoad = oldDiffResult is ViewDiffResult.Loading && oldDiffResult.filePath.isEmpty()

            try {
                delayedStateChange(
                    delayMs = if (isFirstLoad) 0 else DIFF_MIN_TIME_IN_MS_TO_SHOW_LOAD,
                    onDelayTriggered = { _diffResult.value = ViewDiffResult.Loading(diffType.filePath) }
                ) {
                    val diffFormat = formatDiffUseCase(git, diffType, isDisplayFullFile.value)
                    val diffEntry = diffFormat.diffEntry
                    if (
                        diffTypeFlow.value == TextDiffType.SPLIT &&
                        diffFormat is DiffResult.Text &&
                        diffEntry.changeType != DiffEntry.ChangeType.ADD &&
                        diffEntry.changeType != DiffEntry.ChangeType.DELETE
                    ) {
                        val splitHunkList = generateSplitHunkFromDiffResultUseCase(diffFormat)
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
                    _diffResult.value = ViewDiffResult.DiffNotFound
                } else
                    ex.printStackTrace()
            }
        }
    }

    fun stageHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
    ) { git ->
        stageHunkUseCase(git, diffEntry, hunk)
    }

    fun resetHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        resetHunkUseCase(git, diffEntry, hunk)
    }

    fun unstageHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
    ) { git ->
        unstageHunkUseCase(git, diffEntry, hunk)
    }

    fun stageFile(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        stageEntryUseCase(git, statusEntry)
    }

    fun unstageFile(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        unstageEntryUseCase(git, statusEntry)
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
        stageHunkLineUseCase(git, entry, hunk, line)
    }

    fun unstageHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        unstageHunkLineUseCase(git, entry, hunk, line)
    }

    fun openFileWithExternalApp(path: String) {
        openFileInExternalAppUseCase(path)
    }

    fun discardHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITTED_CHANGES,
        showError = true,
    ) { git ->
        discardUnstagedHunkLineUseCase(git, entry, hunk, line)
    }

    fun openSubmodule(path: String) = tabState.runOperation(refreshType = RefreshType.NONE) { git ->
        tabsManager.addNewTabFromPath("${git.repository.workTree}/$path", true)
    }
}

enum class TextDiffType(val value: Int) {
    SPLIT(0),
    UNIFIED(1);
}


fun textDiffTypeFromValue(diffTypeValue: Int): TextDiffType {
    return when (diffTypeValue) {
        TextDiffType.SPLIT.value -> TextDiffType.SPLIT
        TextDiffType.UNIFIED.value -> TextDiffType.UNIFIED
        else -> throw NotImplementedError("Diff type not implemented")
    }
}
