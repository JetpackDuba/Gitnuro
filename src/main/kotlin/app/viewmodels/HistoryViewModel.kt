package app.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import app.exceptions.MissingDiffEntryException
import app.extensions.filePath
import app.git.DiffEntryType
import app.git.DiffManager
import app.git.RefreshType
import app.git.TabState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
    private val tabState: TabState,
    private val diffManager: DiffManager,
) {
    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Loading(""))
    val historyState: StateFlow<HistoryState> = _historyState

    private val _viewDiffResult = MutableStateFlow<ViewDiffResult>(ViewDiffResult.None)
    val viewDiffResult: StateFlow<ViewDiffResult> = _viewDiffResult
    var filePath: String = ""

    val lazyListState = MutableStateFlow(
        LazyListState(
            0,
            0
        )
    )

    fun fileHistory(filePath: String) = tabState.safeProcessing(
        refreshType = RefreshType.NONE,
    ) { git ->
        this.filePath = filePath
        _historyState.value = HistoryState.Loading(filePath)

        val log = git.log()
            .addPath(filePath)
            .call()
            .toList()

        _historyState.value = HistoryState.Loaded(filePath, log)
    }

    fun selectCommit(commit: RevCommit) = tabState.runOperation(
        refreshType = RefreshType.NONE,
        showError = true,
    ) { git ->

        try {

            val diffEntries = diffManager.commitDiffEntries(git, commit)
            val diffEntry = diffEntries.firstOrNull {entry ->
                entry.filePath == this.filePath
            }

            if(diffEntry == null) {
                _viewDiffResult.value = ViewDiffResult.DiffNotFound
                return@runOperation
            }
            val diffEntryType = DiffEntryType.CommitDiff(diffEntry)

            val diffFormat = diffManager.diffFormat(git, diffEntryType)

            _viewDiffResult.value = ViewDiffResult.Loaded(diffEntryType, diffFormat)
        } catch (ex: Exception) {
            if(ex is MissingDiffEntryException) {
                tabState.refreshData(refreshType = RefreshType.UNCOMMITED_CHANGES)
                _viewDiffResult.value = ViewDiffResult.DiffNotFound
            } else
                ex.printStackTrace()
        }
    }
}

sealed class HistoryState(val filePath: String) {
    class Loading(filePath: String) : HistoryState(filePath)
    class Loaded(filePath: String, val commits: List<RevCommit>) : HistoryState(filePath)
}

