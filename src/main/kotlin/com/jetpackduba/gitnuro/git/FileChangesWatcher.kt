package com.jetpackduba.gitnuro.git

import FileWatcher
import WatchDirectoryNotifier
import com.jetpackduba.gitnuro.di.TabScope
import com.jetpackduba.gitnuro.exceptions.WatcherInitException
import com.jetpackduba.gitnuro.git.workspace.GetIgnoreRulesUseCase
import com.jetpackduba.gitnuro.system.systemSeparator
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
    private val getIgnoreRulesUseCase: GetIgnoreRulesUseCase,
    private val tabScope: CoroutineScope,
) : AutoCloseable {
    private val _changesNotifier = MutableSharedFlow<Boolean>()
    val changesNotifier: SharedFlow<Boolean> = _changesNotifier
    private val fileWatcher = FileWatcher.new()
    private var shouldKeepLooping = true

    suspend fun watchDirectoryPath(
        repository: Repository,
    ) = withContext(Dispatchers.IO) {
        val workspacePath = repository.workTree.absolutePath
        val gitRepoPath = repository.directory.absolutePath + systemSeparator

        var ignoreRules = getIgnoreRulesUseCase(repository)
        val gitDirIgnoredFiles = listOf(
            Constants.COMMIT_EDITMSG,
            Constants.MERGE_MSG,
            Constants.SQUASH_MSG,
        )

        val checker = object : WatchDirectoryNotifier {
            override fun shouldKeepLooping(): Boolean = shouldKeepLooping

            override fun detectedChange(paths: Array<String>) {
                tabScope.launch {
                    val hasGitIgnoreChanged = paths.any { it == "$workspacePath$systemSeparator.gitignore" }

                    if (hasGitIgnoreChanged) {
                        ignoreRules = getIgnoreRulesUseCase(repository)
                    }

                    val areAllPathsIgnored = paths.all { path ->
                        val matchesAnyIgnoreRule = ignoreRules.any { rule ->
                            rule.isMatch(path, Files.isDirectory(Paths.get(path)))
                        }

                        val isGitIgnoredFile = gitDirIgnoredFiles.any { ignoredFile ->
                            "$workspacePath$systemSeparator.git$systemSeparator$ignoredFile" == path
                        }

                        matchesAnyIgnoreRule || isGitIgnoredFile
                    }

                    val hasGitDirChanged =
                        paths.any { it.startsWith("$workspacePath$systemSeparator.git$systemSeparator") }

                    if (!areAllPathsIgnored) {
                        println("Emitting changes $hasGitIgnoreChanged")
                        _changesNotifier.emit(hasGitDirChanged)
                    }

                }
            }

            override fun onError(code: Int) {
                throw WatcherInitException(code)
            }
        }

        fileWatcher.watch(workspacePath, gitRepoPath, checker)
    }

    override fun close() {
        shouldKeepLooping = false
        fileWatcher.close()
    }

}