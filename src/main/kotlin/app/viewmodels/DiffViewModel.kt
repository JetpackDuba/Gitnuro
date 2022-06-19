package app.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import app.exceptions.MissingDiffEntryException
import app.extensions.delayedStateChange
import app.git.*
import app.git.diff.Hunk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

private const val DIFF_MIN_TIME_IN_MS_TO_SHOW_LOAD = 200L

class DiffViewModel @Inject constructor(
    private val tabState: TabState,
    private val diffManager: DiffManager,
    private val statusManager: StatusManager,
) {
    private val _diffResult = MutableStateFlow<ViewDiffResult>(ViewDiffResult.Loading)
    val diffResult: StateFlow<ViewDiffResult?> = _diffResult

    val lazyListState = MutableStateFlow(
        LazyListState(
            0,
            0
        )
    )

    // TODO Cancel job if the user closed the diff view while loading
    fun updateDiff(diffEntryType: DiffEntryType) = tabState.runOperation(
        refreshType = RefreshType.NONE,
    ) { git ->
        var oldDiffEntryType: DiffEntryType? = null
        val oldDiffResult = _diffResult.value

        if (oldDiffResult is ViewDiffResult.Loaded) {
            oldDiffEntryType = oldDiffResult.diffEntryType
        }


        // If it's a different file or different state (index or workdir), reset the scroll state
        if (oldDiffEntryType != null &&
            oldDiffEntryType is DiffEntryType.UncommitedDiff && diffEntryType is DiffEntryType.UncommitedDiff &&
            oldDiffEntryType.statusEntry.filePath != diffEntryType.statusEntry.filePath
        ) {
            lazyListState.value = LazyListState(
                0,
                0
            )
        }

        try {
            delayedStateChange(
                delayMs = DIFF_MIN_TIME_IN_MS_TO_SHOW_LOAD,
                onDelayTriggered = { _diffResult.value = ViewDiffResult.Loading }
            ) {
                val diffFormat = diffManager.diffFormat(git, diffEntryType)
                _diffResult.value = ViewDiffResult.Loaded(diffEntryType, diffFormat)
            }
        } catch (ex: Exception) {
            if (ex is MissingDiffEntryException) {
                tabState.refreshData(refreshType = RefreshType.UNCOMMITED_CHANGES)
                _diffResult.value = ViewDiffResult.DiffNotFound
            } else
                ex.printStackTrace()
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
}


