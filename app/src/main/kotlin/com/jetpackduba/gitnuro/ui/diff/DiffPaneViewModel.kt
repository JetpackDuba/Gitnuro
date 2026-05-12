package com.jetpackduba.gitnuro.ui.diff

import androidx.compose.foundation.lazy.LazyListState
import com.jetpackduba.gitnuro.SharedRepositoryStateManager
import com.jetpackduba.gitnuro.TabViewModel
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.interfaces.IFormatDiffGitAction
import com.jetpackduba.gitnuro.domain.models.*
import com.jetpackduba.gitnuro.domain.repositories.*
import com.jetpackduba.gitnuro.domain.services.AppSettingsService
import com.jetpackduba.gitnuro.domain.usecases.*
import com.jetpackduba.gitnuro.extensions.stateIn
import com.jetpackduba.gitnuro.system.OpenFileInExternalAppGitAction
import com.jetpackduba.gitnuro.ui.TabsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.RepositoryState
import javax.inject.Inject

private const val DIFF_MIN_TIME_IN_MS_TO_SHOW_LOAD = 200L

class DiffViewModel @Inject constructor(
    private val tabState: TabInstanceRepository,
    private val formatDiffGitAction: IFormatDiffGitAction,
    private val stageHunkUseCase: StageHunkUseCase,
    private val unstageHunkUseCase: UnstageHunkUseCase,
    private val stageHunkLineUseCase: StageHunkLineUseCase,
    private val unstageHunkLineUseCase: UnstageHunkLineUseCase,
    private val resetHunkUseCase: ResetHunkUseCase,
    private val discardHunkLineUseCase: DiscardHunkLineUseCase,
    private val statusStageUseCase: StatusStageUseCase,
    private val statusUnstageUseCase: StatusUnstageUseCase,
    private val openFileInExternalAppGitAction: OpenFileInExternalAppGitAction,
    private val settings: AppSettingsService,
    private val tabScope: TabCoroutineScope,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val removeSelectedDiffUseCase: RemoveSelectedDiffUseCase,
    private val tabsManager: TabsManager,
    private val getDiffUseCase: GetDiffUseCase,
    private val sharedRepositoryStateManager: SharedRepositoryStateManager,
    private val repositoryStateRepository: RepositoryStateRepository,
) : TabViewModel() {
    private val _diffResult = MutableStateFlow<ViewDiffResult>(ViewDiffResult.None)
    private val refreshDiffFlow = repositoryStateRepository
        .completedTasks
        .map { tasks ->
            tasks.filter { task ->
                task is CompletedTask.Success && (
                        task.taskType is TaskType.StageFile ||
                                task.taskType is TaskType.DoCommit ||
                                task.taskType is TaskType.StageAllFiles ||
                                task.taskType is TaskType.StageHunk ||
                                task.taskType is TaskType.StageLine ||
                                task.taskType is TaskType.StageDir ||
                                task.taskType is TaskType.UnstageAllFiles ||
                                task.taskType is TaskType.UnstageFile ||
                                task.taskType is TaskType.UnstageHunk ||
                                task.taskType is TaskType.UnstageDir ||
                                task.taskType is TaskType.UnstageLine
                        )
            }
        }
        .distinctUntilChanged()

    val diffResult: StateFlow<ViewDiffResult?> = combine(
        repositoryDataRepository.diffSelected,
        refreshDiffFlow,
    ) { diffSelected, _ ->
        if (diffSelected?.entries?.count() == 1) {
            val diff = loadDiff(diffSelected.entries.first())

            if (diff is ViewDiffResult.Loaded) {
                addToCloseables()
            }

            diff
        } else {
            ViewDiffResult.DiffNotFound(null)
        }
    }.stateIn(initialValue = null as ViewDiffResult?)

    val closeViewFlow = tabState.closeViewFlow

    val diffTypeFlow = settings.diffTextViewType
    val isDisplayFullFile = settings.diffDisplayFullFile

    val isRepositoryInSafeState = sharedRepositoryStateManager.repositoryState
        .map { it == RepositoryState.SAFE }

    private var diffJob: Job? = null

    val lazyListState = MutableStateFlow(
        LazyListState(
            0,
            0
        )
    )

    private suspend fun loadDiff(diffType: DiffType): ViewDiffResult {
        return getDiffUseCase(diffType)
    }

    fun stageHunk(diffEntry: DiffEntry, hunk: Hunk) = stageHunkUseCase(diffEntry, hunk)

    fun resetHunk(diffEntry: DiffEntry, hunk: Hunk) = resetHunkUseCase(diffEntry, hunk)

    fun unstageHunk(diffEntry: DiffEntry, hunk: Hunk) = unstageHunkUseCase(diffEntry, hunk)

    fun stageFile(statusEntry: StatusEntry) = statusStageUseCase(statusEntry)

    fun unstageFile(statusEntry: StatusEntry) = statusUnstageUseCase(statusEntry)

    fun cancelRunningJobs() {
        diffJob?.cancel()
    }

    fun changeTextDiffType(newDiffType: DiffTextViewType) = tabScope.launch {
        settings.setConfiguration(AppConfig.DiffTextViewType(newDiffType))
    }

    fun changeDisplayFullFile(isDisplayFullFile: Boolean) = tabScope.launch {
        settings.setConfiguration(AppConfig.DiffDisplayFullFile(isDisplayFullFile))
    }

    fun stageHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = stageHunkLineUseCase(entry, hunk, line)

    fun unstageHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = unstageHunkLineUseCase(entry, hunk, line)

    fun openFileWithExternalApp(path: String) {
        openFileInExternalAppGitAction(path)
    }

    fun discardHunkLine(entry: DiffEntry, hunk: Hunk, line: Line) = discardHunkLineUseCase(entry, hunk, line)

    fun openSubmodule(path: String) {
        val repositoryPath = repositoryDataRepository.repositoryPath

        // TODO RepositoryPath point to .git dir instead of worktree? Fix if so
        if (repositoryPath != null) {
            tabsManager.addNewTabFromPath("$repositoryPath/$path", true)
        }
    }

    fun addToCloseables() = tabScope.launch {
        tabState.addCloseableView(CloseableView.DIFF)
    }

    private fun removeFromCloseables() = tabScope.launch {
        tabState.removeCloseableView(CloseableView.DIFF)
    }

    fun reset() {
        cancelRunningJobs()
        removeFromCloseables()
        _diffResult.value = ViewDiffResult.None
    }

    fun clearDiff() {
        val diff = when (val state = diffResult.value) {
            is ViewDiffResult.DiffNotFound -> state.diffType
            is ViewDiffResult.Loaded -> state.diffType
            is ViewDiffResult.Loading -> state.diffType
            else -> null
        }

        if (diff != null) {
            when (diff) {
                is DiffType.CommitDiff -> removeSelectedDiffUseCase(setOf(diff))
                is DiffType.UncommittedDiff -> removeSelectedDiffUseCase(
                    setOf(diff),
                    diff.entryType,
                )
            }
        }
    }
}

