package com.jetpackduba.gitnuro.domain.git

import FileWatcher
import WatchDirectoryNotifier
import com.jetpackduba.gitnuro.common.TabScope
import com.jetpackduba.gitnuro.common.systemSeparator
import com.jetpackduba.gitnuro.domain.git.workspace.GetIgnoreRulesGitAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject

private const val TAG = "FileChangesWatcher"

@TabScope
class FileChangesWatcher @Inject constructor(
    private val getIgnoreRulesGitAction: GetIgnoreRulesGitAction,
    private val tabScope: CoroutineScope,
) : AutoCloseable {
    private val _changesNotifier = MutableSharedFlow<WatcherEvent>()
    val changesNotifier: SharedFlow<WatcherEvent> = _changesNotifier
    private val fileWatcher = FileWatcher.new()
    private var shouldKeepLooping = true

    suspend fun watchDirectoryPath(
        repository: Repository,
    ) = withContext(Dispatchers.IO) {
        val workspacePath = repository.workTree.absolutePath
        val gitRepoPath = repository.directory.absolutePath + systemSeparator

        var ignoreRules = getIgnoreRulesGitAction(repository)
        val gitDirIgnoredFiles = listOf(
            Constants.COMMIT_EDITMSG,
            Constants.MERGE_MSG,
            Constants.SQUASH_MSG,
        )

        val checker = object : WatchDirectoryNotifier {
            override fun shouldKeepLooping(): Boolean = shouldKeepLooping

            override fun detectedChange(paths: Array<String>) {
                tabScope.launch {
                    val normalizedPaths = paths.map {
                        it.removePrefix(workspacePath)
                    }
                    val hasGitIgnoreChanged = normalizedPaths.any { it == "$systemSeparator.gitignore" }

                    if (hasGitIgnoreChanged) {
                        ignoreRules = getIgnoreRulesGitAction(repository)
                    }

                    val areAllPathsIgnored = normalizedPaths.all { path ->
                        val matchesAnyIgnoreRule = ignoreRules.any { rule ->
                            rule.isMatch(path, Files.isDirectory(Paths.get(path)))
                        }

                        val isGitIgnoredFile = gitDirIgnoredFiles.any { ignoredFile ->
                            "$systemSeparator.git$systemSeparator$ignoredFile" == path
                        }

                        matchesAnyIgnoreRule || isGitIgnoredFile
                    }

                    val hasGitDirChanged =
                        normalizedPaths.any { it.startsWith("$systemSeparator.git$systemSeparator") }

                    if (!areAllPathsIgnored) {
                        println("Emitting changes $hasGitIgnoreChanged")
                        _changesNotifier.emit(WatcherEvent.RepositoryChanged(hasGitDirChanged))
                    }

                }
            }

            override fun onError(code: Int) {
                tabScope.launch {
                    _changesNotifier.emit(WatcherEvent.WatchInitError(code))
                }
            }
        }

        fileWatcher.watch(workspacePath, gitRepoPath, checker)
    }

    override fun close() {
        shouldKeepLooping = false
        fileWatcher.close()
    }

}

sealed interface WatcherEvent {
    data class RepositoryChanged(val hasGitDirChanged: Boolean) : WatcherEvent
    data class WatchInitError(val code: Int) : WatcherEvent
}