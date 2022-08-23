//asdasd
package app.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import app.exceptions.MissingDiffEntryException
import app.extensions.delayedStateChange
import app.git.*
import app.git.diff.DiffResult
import app.git.diff.Hunk
import app.preferences.AppSettings
import app.git.diff.GenerateSplitHunkFromDiffResultUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

private const val DIFF_MIN_TIME_IN_MS_TO_SHOW_LOAD = 200L

class DiffViewModel @Inject constructor(
    private val tabState: TabState,
    private val diffManager: DiffManager,
    private val statusManager: StatusManager,
    private val settings: AppSettings,
    private val generateSplitHunkFromDiffResultUseCase: GenerateSplitHunkFromDiffResultUseCase,
) {
    private val _diffResult = MutableStateFlow<ViewDiffResult>(ViewDiffResult.Loading(""))
    val diffResult: StateFlow<ViewDiffResult?> = _diffResult

    val diffTypeFlow = settings.textDiffTypeFlow
    private var diffEntryType: DiffEntryType? = null
    private var diffTypeFlowChangesCount = 0

    private var diffJob: Job? = null

    init {
        tabState.managerScope.launch {
            diffTypeFlow.collect {
                val diffEntryType = this@DiffViewModel.diffEntryType
                if (diffTypeFlowChangesCount > 0 && diffEntryType != null) { // Ignore the first time the flow triggers, we only care about updates
                    updateDiff(diffEntryType)
                }

                diffTypeFlowChangesCount++
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
            if (oldDiffEntryType != null &&
                oldDiffEntryType is DiffEntryType.UncommitedDiff &&
                diffEntryType is DiffEntryType.UncommitedDiff &&
                oldDiffEntryType.statusEntry.filePath != diffEntryType.statusEntry.filePath
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
                    val diffFormat = diffManager.diffFormat(git, diffEntryType)
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
        statusManager.stageHunk(git, diffEntry, hunk)
    }

    fun resetHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
        showError = true,
    ) { git ->
        statusManager.resetHunk(git, diffEntry, hunk)
    }

    fun unstageHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.unstageHunk(git, diffEntry, hunk)
    }

    fun stageFile(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.stage(git, statusEntry)
    }

    fun unstageFile(statusEntry: StatusEntry) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.unstage(git, statusEntry)
    }

    fun cancelRunningJobs() {
        diffJob?.cancel()
    }

    fun changeTextDiffType(newDiffType: TextDiffType) {
        settings.textDiffType = newDiffType
    }
}