//asdasd
package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import com.jetpackduba.gitnuro.exceptions.MissingDiffEntryException
import com.jetpackduba.gitnuro.extensions.delayedStateChange
import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import com.jetpackduba.gitnuro.git.diff.*
import com.jetpackduba.gitnuro.git.workspace.*
import com.jetpackduba.gitnuro.preferences.AppSettings
import com.jetpackduba.gitnuro.system.OpenFileInExternalAppUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val settings: AppSettings,
    private val generateSplitHunkFromDiffResultUseCase: GenerateSplitHunkFromDiffResultUseCase,
    private val discardUnstagedHunkLineUseCase: DiscardUnstagedHunkLineUseCase,
    tabScope: CoroutineScope,
) {
    private val _diffResult = MutableStateFlow<ViewDiffResult>(ViewDiffResult.Loading(""))
    val diffResult: StateFlow<ViewDiffResult?> = _diffResult

    val diffTypeFlow = settings.textDiffTypeFlow
    private var diffEntryType: DiffEntryType? = null
    private var diffTypeFlowChangesCount = 0

    private var diffJob: Job? = null

    init {
        tabScope.launch {
            diffTypeFlow.collect {
                val diffEntryType = this@DiffViewModel.diffEntryType
                if (diffTypeFlowChangesCount > 0 && diffEntryType != null) { // Ignore the first time the flow triggers, we only care about updates
                    updateDiff(diffEntryType)
                }

                diffTypeFlowChangesCount++
            }
        }

        tabScope.launch {
            tabState.refreshFlowFiltered(
                RefreshType.UNCOMMITED_CHANGES,
                RefreshType.UNCOMMITED_CHANGES_AND_LOG,
            ) {
                val diffResultValue = diffResult.value
                if (diffResultValue is ViewDiffResult.Loaded) {
                    updateDiff(diffResultValue.diffEntryType)
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

    fun updateDiff(diffEntryType: DiffEntryType) {
        diffJob = tabState.runOperation(refreshType = RefreshType.NONE) { git ->
            this.diffEntryType = diffEntryType

            var oldDiffEntryType: DiffEntryType? = null
            val oldDiffResult = _diffResult.value

            if (oldDiffResult is ViewDiffResult.Loaded) {
                oldDiffEntryType = oldDiffResult.diffEntryType
            }

            // If it's a different file or different state (index or workdir), reset the scroll state
            if (
                oldDiffEntryType?.filePath != diffEntryType.filePath ||
                oldDiffEntryType is DiffEntryType.UncommitedDiff &&
                diffEntryType is DiffEntryType.UncommitedDiff &&
                oldDiffEntryType.statusEntry.filePath == diffEntryType.statusEntry.filePath &&
                oldDiffEntryType::class != diffEntryType::class
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
                    onDelayTriggered = { _diffResult.value = ViewDiffResult.Loading(diffEntryType.filePath) }
                ) {
                    val diffFormat = formatDiffUseCase(git, diffEntryType)
                    val diffEntry = diffFormat.diffEntry
                    if (
                        diffTypeFlow.value == TextDiffType.SPLIT &&
                        diffFormat is DiffResult.Text &&
                        diffEntry.changeType != DiffEntry.ChangeType.ADD &&
                        diffEntry.changeType != DiffEntry.ChangeType.DELETE
                    ) {
                        val splitHunkList = generateSplitHunkFromDiffResultUseCase(diffFormat)
                        _diffResult.value = ViewDiffResult.Loaded(
                            diffEntryType,
                            DiffResult.TextSplit(diffEntry, splitHunkList)
                        )
                    } else {
                        _diffResult.value = ViewDiffResult.Loaded(diffEntryType, diffFormat)
                    }
                }
            } catch (ex: Exception) {
                if (ex is MissingDiffEntryException) {
                    tabState.refreshData(refreshType = RefreshType.UNCOMMITED_CHANGES)
                    _diffResult.value = ViewDiffResult.DiffNotFound
                } else
                    ex.printStackTrace()
            }
        }
    }

    fun stageHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        stageHunkUseCase(git, diffEntry, hunk)
    }

    fun resetHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
        showError = true,
    ) { git ->
        resetHunkUseCase(git, diffEntry, hunk)
    }

    fun unstageHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        unstageHunkUseCase(git, diffEntry, hunk)
    }

    fun stageFile(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
        showError = true,
    ) { git ->
        stageEntryUseCase(git, statusEntry)
    }

    fun unstageFile(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
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

    fun stageHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
        showError = true,
    ) { git ->
        stageHunkLineUseCase(git, entry, hunk, line)
    }

    fun unstageHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
        showError = true,
    ) { git ->
        unstageHunkLineUseCase(git, entry, hunk, line)
    }

    fun openFileWithExternalApp(path: String) {
        openFileInExternalAppUseCase(path)
    }

    fun discardHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
        showError = true,
    ) { git ->
        discardUnstagedHunkLineUseCase(git, entry, hunk, line)
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
