package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.common.printDebug
import com.jetpackduba.gitnuro.common.systemSeparator
import com.jetpackduba.gitnuro.domain.TabCoroutineScope
import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.interfaces.IFileChangesWatcher
import com.jetpackduba.gitnuro.domain.models.WatcherEvent
import com.jetpackduba.gitnuro.domain.repositories.RepositoryDataRepository
import com.jetpackduba.gitnuro.domain.repositories.RepositoryStateRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ObserveRepositoryToRefreshUseCase"

private const val REFRESH_TIME_SINCE_LAST_OPERATION = 5_000L // 5 seconds

class ObserveRepositoryToRefreshUseCase @Inject constructor(
    private val tabCoroutineScope: TabCoroutineScope,
    private val fileChangesWatcher: IFileChangesWatcher,
    private val repositoryDataRepository: RepositoryDataRepository,
    private val getWorktreeUseCase: GetWorktreeUseCase,
    private val refreshAllUseCase: RefreshAllUseCase,
    private val refreshStatusUseCase: RefreshStatusUseCase,
    private val refreshLogUseCase: RefreshLogUseCase,
    private val repositoryStateRepository: RepositoryStateRepository,
) {
    /**
     * Sometimes external apps can run filesystem multiple operations in a fraction of a second.
     * To prevent excessive updates, we add a slight delay between updates emission to prevent slowing down
     * the app by constantly running "git status" or even full refreshes.
     *
     */
    operator fun invoke() {
        // TODO add some logging?
        val repositoryPath = repositoryDataRepository.repositoryPath ?: return
        tabCoroutineScope.launch {
            val worktreeDir = getWorktreeUseCase().okOrNull() ?: return@launch
            // The Rust code for the watch used to exclude files that started with .probe- as they were generate by JGit
            // We may need to filter some of that stuff in this use case or provide some regex filtering to the rust side
            //             let probe_prefix = format!("{git_dir_path}.probe-");
            launch {
                fileChangesWatcher
                    .observeEvents()
                    .collect { event ->
                        when (event) {
                            is WatcherEvent.ChangesDetected -> {
                                printDebug(TAG, "Changes detected: ${event.changes.toList()}")

                                val timeDiffInMs =
                                    System.currentTimeMillis() - repositoryStateRepository.lastOperationTimestamp.first()

                                if (timeDiffInMs > REFRESH_TIME_SINCE_LAST_OPERATION) {
                                    val hasGitDirChanged = event.changes.any { it.startsWith(repositoryPath) }

                                    if (hasGitDirChanged) {
                                        refreshAllUseCase()
                                    } else {
                                        refreshStatusUseCase()
                                        refreshLogUseCase()
                                    }
                                }
                            }

                            is WatcherEvent.WatchInitError -> {
                                printDebug(TAG, "Watch init error: ${event.code}")
                            }
                        }
                    }
            }

            fileChangesWatcher.addPathToWatch(worktreeDir, false)
            fileChangesWatcher.addPathToWatch(repositoryPath, false)
            fileChangesWatcher.addPathToWatch("$repositoryPath${systemSeparator}refs", true)
            fileChangesWatcher.addPathToWatch("$repositoryPath${systemSeparator}modules", true)
        }.invokeOnCompletion {
            fileChangesWatcher.close()
        }
    }
}
