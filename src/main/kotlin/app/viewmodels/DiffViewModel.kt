package app.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import app.exceptions.MissingDiffEntryException
import app.git.*
import app.git.diff.DiffResult
import app.git.diff.Hunk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

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

        if(oldDiffResult is ViewDiffResult.Loaded) {
            oldDiffEntryType = oldDiffResult.diffEntryType
        }

        _diffResult.value = ViewDiffResult.Loading

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
            val diffFormat = diffManager.diffFormat(git, diffEntryType)
            _diffResult.value = ViewDiffResult.Loaded(diffEntryType, diffFormat)
        } catch (ex: Exception) {
            if(ex is MissingDiffEntryException) {
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

    fun unstageHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation(
        refreshType = RefreshType.UNCOMMITED_CHANGES,
    ) { git ->
        statusManager.unstageHunk(git, diffEntry, hunk)
    }
}


sealed interface ViewDiffResult {
    object Loading: ViewDiffResult
    object DiffNotFound: ViewDiffResult
    data class Loaded(val diffEntryType: DiffEntryType, val diffResult: DiffResult): ViewDiffResult
}
