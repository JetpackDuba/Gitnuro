package app.viewmodels

import androidx.compose.foundation.lazy.LazyListState
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
    // TODO Maybe use a sealed class instead of a null to represent that a diff is not selected?
    private val _diffResult = MutableStateFlow<ViewDiffResult?>(null)
    val diffResult: StateFlow<ViewDiffResult?> = _diffResult

    val lazyListState = MutableStateFlow(
        LazyListState(
            0,
            0
        )
    )

    fun updateDiff(diffEntryType: DiffEntryType) = tabState.runOperation { git ->
        val oldDiffEntryType = _diffResult.value?.diffEntryType

        _diffResult.value = null

        // If it's a different file or different state (index or workdir), reset the scroll state
        if (oldDiffEntryType != null &&
            (oldDiffEntryType.diffEntry.oldPath != diffEntryType.diffEntry.oldPath ||
                    oldDiffEntryType.diffEntry.newPath != diffEntryType.diffEntry.newPath ||
                    oldDiffEntryType::class != diffEntryType::class)
        ) {
            lazyListState.value = LazyListState(
                0,
                0
            )
        }

        //TODO: Just a workaround when trying to diff binary files
        try {
            val hunks = diffManager.diffFormat(git, diffEntryType)
            _diffResult.value = ViewDiffResult(diffEntryType, hunks)
        } catch (ex: Exception) {
            ex.printStackTrace()
            _diffResult.value = ViewDiffResult(diffEntryType, DiffResult.Text(emptyList()))
        }

        return@runOperation RefreshType.NONE
    }

    fun stageHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation { git ->
        statusManager.stageHunk(git, diffEntry, hunk)

        return@runOperation RefreshType.UNCOMMITED_CHANGES
    }

    fun unstageHunk(diffEntry: DiffEntry, hunk: Hunk) = tabState.runOperation { git ->
        statusManager.unstageHunk(git, diffEntry, hunk)

        return@runOperation RefreshType.UNCOMMITED_CHANGES
    }
}

data class ViewDiffResult(val diffEntryType: DiffEntryType, val diffResult: DiffResult)