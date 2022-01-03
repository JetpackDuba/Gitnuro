package app.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import app.git.*
import app.git.diff.Hunk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class DiffViewModel @Inject constructor(
    private val tabState: TabState,
    private val diffManager: DiffManager,
    private val statusManager: StatusManager,
) {
    // TODO Maybe use a sealed class instead of a null to represent that a diff is not selected?
    private val _diffResult = MutableStateFlow<DiffResult?>(null)
    val diffResult: StateFlow<DiffResult?> = _diffResult

    val lazyListState = MutableStateFlow(
        LazyListState(
        0,
        0
        )
    )

    suspend fun updateDiff(git: Git, diffEntryType: DiffEntryType) = withContext(Dispatchers.IO) {
        val oldDiffEntryType = _diffResult.value?.diffEntryType

        _diffResult.value = null

        // If it's a different file or different state (index or workdir), reset the scroll state
        if(oldDiffEntryType != null &&
            (oldDiffEntryType.diffEntry.oldPath != diffEntryType.diffEntry.oldPath ||
            oldDiffEntryType.diffEntry.newPath != diffEntryType.diffEntry.newPath ||
            oldDiffEntryType::class != diffEntryType::class)
        ) {
            lazyListState.value = LazyListState(
                0,
                0
            )
        }

        val hunks = diffManager.diffFormat(git, diffEntryType)

        _diffResult.value = DiffResult(diffEntryType, hunks)
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

data class DiffResult(val diffEntryType: DiffEntryType, val hunks: List<Hunk>)