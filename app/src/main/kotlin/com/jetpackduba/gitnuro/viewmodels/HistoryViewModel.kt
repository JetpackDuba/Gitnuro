package com.jetpackduba.gitnuro.viewmodels

import androidx.compose.foundation.lazy.LazyListState
import com.jetpackduba.gitnuro.domain.models.TaskType
import com.jetpackduba.gitnuro.domain.exceptions.MissingDiffEntryException
import com.jetpackduba.gitnuro.domain.extensions.filePath
import com.jetpackduba.gitnuro.domain.models.DiffType
import com.jetpackduba.gitnuro.domain.repositories.RefreshType
import com.jetpackduba.gitnuro.domain.repositories.TabInstanceRepository
import com.jetpackduba.gitnuro.domain.models.DiffResult
import com.jetpackduba.gitnuro.domain.interfaces.IFormatDiffGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGenerateSplitHunkFromDiffResultGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetCommitDiffEntriesGitAction
import com.jetpackduba.gitnuro.data.repositories.configuration.DataStoreAppSettingsRepository
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.DiffTextViewType
import com.jetpackduba.gitnuro.domain.models.ViewDiffResult
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import com.jetpackduba.gitnuro.domain.usecases.GetDiffUseCase
import com.jetpackduba.gitnuro.domain.usecases.GetFileCommitsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

class HistoryViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val formatDiffGitAction: IFormatDiffGitAction,
    private val getCommitDiffEntriesGitAction: IGetCommitDiffEntriesGitAction,
    private val generateSplitHunkFromDiffResultGitAction: IGenerateSplitHunkFromDiffResultGitAction,
    private val settings: AppSettingsService,
    private val tabScope: TabCoroutineScope,
    private val appSettingsRepository: DataStoreAppSettingsRepository,
    private val getFileCommitsUseCase: GetFileCommitsUseCase,
    private val getDiffUseCase: GetDiffUseCase,
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
            settings.diffTextViewType.collect { diffType ->
                if (filePath.isNotBlank()) {
                    updateDiffType(diffType)
                }
            }
        }
    }

    private fun updateDiffType(newDiffType: DiffTextViewType) {
        val viewDiffResult = this.viewDiffResult.value

        if (viewDiffResult is ViewDiffResult.Loaded) {
            val diffResult = viewDiffResult.diffResult

            if (diffResult is DiffResult.Text && newDiffType == DiffTextViewType.Split) { // Current is unified and new is split
                val hunksList = generateSplitHunkFromDiffResultGitAction(diffResult)
                _viewDiffResult.value = ViewDiffResult.Loaded(
                    diffType = viewDiffResult.diffType,
                    diffResult = DiffResult.TextSplit(diffResult.diffEntry, hunksList)
                )
            } else if (diffResult is DiffResult.TextSplit && newDiffType == DiffTextViewType.Unified) { // Current is split and new is unified
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

        val log = getFileCommitsUseCase(git, filePath)

        _historyState.value = HistoryState.Loaded(filePath, log)

        null
    }

    fun selectCommit(commit: Commit) = tabState.runOperation(
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
            _viewDiffResult.value = getDiffUseCase(diffType)
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
    class Loaded(filePath: String, val commits: List<Commit>) : HistoryState(filePath)
}

