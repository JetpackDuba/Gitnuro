package aeab13.github.com

import aeab13.github.com.git.LogManager
import aeab13.github.com.git.LogStatus
import aeab13.github.com.git.StageStatus
import aeab13.github.com.git.StatusManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.lib.Constants.HEAD
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.filter.TreeFilter
import java.io.ByteArrayOutputStream
import java.io.File


class GitManager {
    private val preferences = GPreferences()
    private val statusManager = StatusManager()
    private val logManager = LogManager()

    private val managerScope = CoroutineScope(SupervisorJob())

    private val _repositorySelectionStatus = MutableStateFlow<RepositorySelectionStatus>(RepositorySelectionStatus.None)
    val repositorySelectionStatus: StateFlow<RepositorySelectionStatus>
        get() = _repositorySelectionStatus

    private val _processing = MutableStateFlow(false)
    val processing: StateFlow<Boolean>
        get() = _processing

    val stageStatus: StateFlow<StageStatus>
        get() = statusManager.stageStatus

    val logStatus: StateFlow<LogStatus>
        get() = logManager.logStatus

    val latestDirectoryOpened: File?
        get() = File(preferences.latestOpenedRepositoryPath).parentFile

    private var git: Git? = null

    var lastTimeStatusChanged: Long = 0 // TODO Add file watcher to the repository
        private set

    val safeGit: Git
        get() {
            val git = this.git
            if (git == null) {
                _repositorySelectionStatus.value = RepositorySelectionStatus.None
                throw CancellationException()
            } else
                return git
        }

    init {
        val latestOpenedRepositoryPath = preferences.latestOpenedRepositoryPath
        if (latestOpenedRepositoryPath.isNotEmpty()) {
            openRepository(File(latestOpenedRepositoryPath))
        }
    }

    fun openRepository(directory: File) {
        val gitDirectory = if (directory.name == ".git") {
            directory
        } else {
            val gitDir = File(directory, ".git")
            if (gitDir.exists() && gitDir.isDirectory) {
                gitDir
            } else
                directory

        }

        val builder = FileRepositoryBuilder()
        val repository: Repository = builder.setGitDir(gitDirectory)
            .readEnvironment() // scan environment GIT_* variables
            .findGitDir() // scan up the file system tree
            .build()

        try {
            repository.workTree // test if repository is valid
            preferences.latestOpenedRepositoryPath = gitDirectory.path
            _repositorySelectionStatus.value = RepositorySelectionStatus.Open(repository)
            git = Git(repository)

            loadLog()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun loadLog() = managerScope.launch {
        logManager.loadLog(safeGit)
    }

    fun loadStatus() = managerScope.launch {
        statusManager.loadStatus(safeGit)
    }

    fun updateStatus() {
        lastTimeStatusChanged = System.currentTimeMillis()
        loadStatus()
    }

    fun stage(diffEntry: DiffEntry) = managerScope.launch {
        statusManager.stage(safeGit, diffEntry)
    }

    fun unstage(diffEntry: DiffEntry) = managerScope.launch {
        statusManager.unstage(safeGit, diffEntry)
    }

    fun commit(message: String) = managerScope.launch {
        statusManager.commit(safeGit, message)
        logManager.loadLog(safeGit)
    }

    fun hasUncommitedChanges(): Boolean = statusManager.hasUncommitedChanges(safeGit)

    fun diffFormat(diffEntry: DiffEntry): String {
        val byteArrayOutputStream = ByteArrayOutputStream()

        DiffFormatter(byteArrayOutputStream).use { formatter ->
            val repo = safeGit.repository
            formatter.setRepository(repo)

            val oldTree = DirCacheIterator(repo.readDirCache())
            val newTree = FileTreeIterator(repo)

            println(diffEntry)
            formatter.scan(oldTree, newTree)
//            formatter.format(oldTree, newTree)
            formatter.format(diffEntry)
            formatter.flush()
        }

        return byteArrayOutputStream.toString(Charsets.UTF_8)
    }
}


sealed class RepositorySelectionStatus {
    object None : RepositorySelectionStatus()
    object Loading : RepositorySelectionStatus()
    data class Open(val repository: Repository) : RepositorySelectionStatus()
}