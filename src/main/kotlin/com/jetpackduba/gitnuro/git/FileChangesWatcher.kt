package com.jetpackduba.gitnuro.git

import com.jetpackduba.gitnuro.git.workspace.GetIgnoreRulesUseCase
import com.jetpackduba.gitnuro.system.systemSeparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import uniffi.gitnuro.WatchDirectoryNotifier
import uniffi.gitnuro.watchDirectory
import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Inject

private const val TAG = "FileChangesWatcher"

class FileChangesWatcher @Inject constructor(
    private val getIgnoreRulesUseCase: GetIgnoreRulesUseCase,
) {
    private val _changesNotifier = MutableSharedFlow<Boolean>()
    val changesNotifier: SharedFlow<Boolean> = _changesNotifier

    suspend fun watchDirectoryPath(
        repository: Repository,
        pathStr: String
    ) = withContext(Dispatchers.IO) {
        var ignoreRules = getIgnoreRulesUseCase(repository)
        val gitDirIgnoredFiles = listOf(
            Constants.COMMIT_EDITMSG,
            Constants.MERGE_MSG,
            Constants.SQUASH_MSG,
        )

        val checker = object : WatchDirectoryNotifier {
            override fun shouldKeepLooping(): Boolean {
                return isActive
            }

            override fun detectedChange(paths: List<String>) = runBlocking {
                val hasGitIgnoreChanged = paths.any { it == "$pathStr$systemSeparator.gitignore" }

                if (hasGitIgnoreChanged) {
                    ignoreRules = getIgnoreRulesUseCase(repository)
                }

                val areAllPathsIgnored = paths.all { path ->
                    val matchesAnyIgnoreRule = ignoreRules.any { rule ->
                        rule.isMatch(path, Files.isDirectory(Paths.get(path)))
                    }

                    val isGitIgnoredFile = gitDirIgnoredFiles.any { ignoredFile ->
                        "$pathStr$systemSeparator.git$systemSeparator$ignoredFile" == path
                    }

                    // JGit may create .probe-UUID files for its internal stuff, we should not care about it
                    val onlyProbeFiles = paths.all { it.contains("$systemSeparator.git$systemSeparator.probe-") }

                    matchesAnyIgnoreRule || isGitIgnoredFile || onlyProbeFiles
                }

                val hasGitDirChanged = paths.any { it.startsWith("$pathStr$systemSeparator.git$systemSeparator") }

                if (!areAllPathsIgnored) {
                    _changesNotifier.emit(hasGitDirChanged)
                }
            }
        }

        watchDirectory(pathStr, checker)
    }
}