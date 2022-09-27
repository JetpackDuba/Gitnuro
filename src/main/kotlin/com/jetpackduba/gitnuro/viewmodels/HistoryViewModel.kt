package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import com.jetpackduba.gitnuro.exceptions.MissingDiffEntryException
import com.jetpackduba.gitnuro.extensions.filePath
import com.jetpackduba.gitnuro.git.diff.DiffResult
import com.jetpackduba.gitnuro.git.diff.FormatDiffUseCase
import com.jetpackduba.gitnuro.preferences.AppSettings
import com.jetpackduba.gitnuro.git.diff.GenerateSplitHunkFromDiffResultUseCase
import com.jetpackduba.gitnuro.git.diff.GetCommitDiffEntriesUseCase
import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.git.RefreshType
import com.jetpackduba.gitnuro.git.TabState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
    private val tabState: TabState,
    private val formatDiffUseCase: FormatDiffUseCase,
    private val getCommitDiffEntriesUseCase: GetCommitDiffEntriesUseCase,
    private val settings: AppSettings,
    private val generateSplitHunkFromDiffResultUseCase: GenerateSplitHunkFromDiffResultUseCase,
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
        tabState.managerScope.launch {
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
                val hunksList = generateSplitHunkFromDiffResultUseCase(diffResult)
                _viewDiffResult.value = ViewDiffResult.Loaded(
                    diffEntryType = viewDiffResult.diffEntryType,
                    diffResult = DiffResult.TextSplit(diffResult.diffEntry, hunksList)
                )
            } else if (diffResult is DiffResult.TextSplit && newDiffType == TextDiffType.UNIFIED) { // Current is split and new is unified
                val hunksList = diffResult.hunks.map { it.sourceHunk }

                _viewDiffResult.value = ViewDiffResult.Loaded(
                    diffEntryType = viewDiffResult.diffEntryType,
                    diffResult = DiffResult.Text(diffResult.diffEntry, hunksList)
                )
            }
        }
    }

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
            val diffEntries = getCommitDiffEntriesUseCase(git, commit)
            val diffEntry = diffEntries.firstOrNull { entry ->
                entry.filePath == this.filePath
            }

            if (diffEntry == null) {
                _viewDiffResult.value = ViewDiffResult.DiffNotFound
                return@runOperation
            }

            val diffEntryType = DiffEntryType.CommitDiff(diffEntry)

            val diffResult = formatDiffUseCase(git, diffEntryType)
            val textDiffType = settings.textDiffType

            val formattedDiffResult = if (textDiffType == TextDiffType.SPLIT && diffResult is DiffResult.Text) {
                DiffResult.TextSplit(diffEntry, generateSplitHunkFromDiffResultUseCase(diffResult))
            } else
                diffResult

            _viewDiffResult.value = ViewDiffResult.Loaded(diffEntryType, formattedDiffResult)
        } catch (ex: Exception) {
            if (ex is MissingDiffEntryException) {
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

