package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.exceptions.MissingDiffEntryException
import com.jetpackduba.gitnuro.domain.extensions.filePath
import com.jetpackduba.gitnuro.domain.git.DiffType
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.git.diff.DiffResult
import com.jetpackduba.gitnuro.domain.git.diff.FormatDiffGitAction
import com.jetpackduba.gitnuro.domain.git.diff.GenerateSplitHunkFromDiffResultGitAction
import com.jetpackduba.gitnuro.domain.git.diff.GetCommitDiffEntriesGitAction
import com.jetpackduba.gitnuro.data.repositories.AppSettingsRepository
import com.jetpackduba.gitnuro.domain.models.TextDiffType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val formatDiffGitAction: FormatDiffGitAction,
    private val getCommitDiffEntriesGitAction: GetCommitDiffEntriesGitAction,
    private val settings: AppSettingsRepository,
    private val generateSplitHunkFromDiffResultGitAction: GenerateSplitHunkFromDiffResultGitAction,
    private val tabScope: CoroutineScope,
    private val appSettingsRepository: AppSettingsRepository,
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


    init {
        tabScope.launch {
            settings.textDiffTypeFlow.collect { diffType ->
                if (filePath.isNotBlank()) {
                    updateDiffType(diffType)
                }
            }
        }
    }

    private fun updateDiffType(newDiffType: TextDiffType) {
        val viewDiffResult = this.viewDiffResult.value

        if (viewDiffResult is ViewDiffResult.Loaded) {
            val diffResult = viewDiffResult.diffResult

            if (diffResult is DiffResult.Text && newDiffType == TextDiffType.SPLIT) { // Current is unified and new is split
                val hunksList = generateSplitHunkFromDiffResultGitAction(diffResult)
                _viewDiffResult.value = ViewDiffResult.Loaded(
                    diffType = viewDiffResult.diffType,
                    diffResult = DiffResult.TextSplit(diffResult.diffEntry, hunksList)
                )
            } else if (diffResult is DiffResult.TextSplit && newDiffType == TextDiffType.UNIFIED) { // Current is split and new is unified
                val hunksList = diffResult.hunks.map { it.sourceHunk }

                _viewDiffResult.value = ViewDiffResult.Loaded(
                    diffType = viewDiffResult.diffType,
                    diffResult = DiffResult.Text(diffResult.diffEntry, hunksList)
                )
            }
        }
    }

    fun fileHistory(filePath: String) = tabState.safeProcessing(
        refreshType = RefreshType.NONE,
        title = "History",
        subtitle = "Loading file history",
        taskType = TaskType.HISTORY_FILE,
    ) { git ->
        this@HistoryViewModel.filePath = filePath
        _historyState.value = HistoryState.Loading(filePath)

        val log = git.log()
            .addPath(filePath)
            .call()
            .toList()

        _historyState.value = HistoryState.Loaded(filePath, log)

        null
    }

    fun selectCommit(commit: RevCommit) = tabState.runOperation(
        refreshType = RefreshType.NONE,
        showError = true,
    ) { git ->

        try {
            val diffEntries = getCommitDiffEntriesGitAction(git, commit)
            val diffEntry = diffEntries.firstOrNull { entry ->
                entry.filePath == this.filePath
            }

            if (diffEntry == null) {
                _viewDiffResult.value = ViewDiffResult.DiffNotFound(null)
                return@runOperation
            }

            val diffType = DiffType.CommitDiff(diffEntry)

            val diffResult = formatDiffGitAction(
                git,
                diffType,
                false
            ) // TODO This hardcoded false should be changed when the UI is implemented
            val textDiffType = settings.textDiffType

            val formattedDiffResult = if (textDiffType == TextDiffType.SPLIT && diffResult is DiffResult.Text) {
                DiffResult.TextSplit(diffEntry, generateSplitHunkFromDiffResultGitAction(diffResult))
            } else
                diffResult

            _viewDiffResult.value = ViewDiffResult.Loaded(diffType, formattedDiffResult)
        } catch (ex: Exception) {
            if (ex is MissingDiffEntryException) {
                tabState.refreshData(refreshType = RefreshType.UNCOMMITTED_CHANGES)
                _viewDiffResult.value = ViewDiffResult.DiffNotFound(null)
            } else
                ex.printStackTrace()
        }
    }
}

sealed class HistoryState(val filePath: String) {
    class Loading(filePath: String) : HistoryState(filePath)
    class Loaded(filePath: String, val commits: List<RevCommit>) : HistoryState(filePath)
}

