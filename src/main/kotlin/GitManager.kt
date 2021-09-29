import git.*
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
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import org.eclipse.jgit.treewalk.FileTreeIterator
import java.io.ByteArrayOutputStream
import java.io.File


class GitManager {
    private val preferences = GPreferences()
    private val statusManager = StatusManager()
    private val logManager = LogManager()
    private val remoteOperationsManager = RemoteOperationsManager()
    private val branchesManager = BranchesManager()
    private val stashManager = StashManager()

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

    val branches: StateFlow<List<Ref>>
        get() = branchesManager.branches

    val currentBranch: StateFlow<String>
        get() = branchesManager.currentBranch

    val stashStatus: StateFlow<StashStatus>
        get() = stashManager.stashStatus

    val latestDirectoryOpened: File?
        get() = File(preferences.latestOpenedRepositoryPath).parentFile

    private var git: Git? = null

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

            refreshRepositoryInfo()
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

    val hasUncommitedChanges: StateFlow<Boolean>
        get() = statusManager.hasUncommitedChanges

    fun diffFormat(diffEntryType: DiffEntryType): String {
        val diffEntry = diffEntryType.diffEntry
        val byteArrayOutputStream = ByteArrayOutputStream()

        DiffFormatter(byteArrayOutputStream).use { formatter ->
            val repo = safeGit.repository
            formatter.setRepository(repo)

            val oldTree = DirCacheIterator(repo.readDirCache())
            val newTree = FileTreeIterator(repo)

            if (diffEntryType is DiffEntryType.UnstagedDiff)
                formatter.scan(oldTree, newTree)

            formatter.format(diffEntry)
            formatter.flush()
        }

        return byteArrayOutputStream.toString(Charsets.UTF_8)
    }

    fun pull() = managerScope.launch {
        remoteOperationsManager.pull(safeGit)
    }

    fun push() = managerScope.launch {
        remoteOperationsManager.push(safeGit)
    }

    private fun refreshRepositoryInfo() = managerScope.launch {
        statusManager.loadHasUncommitedChanges(safeGit)
        branchesManager.loadBranches(safeGit)
        stashManager.loadStashList(safeGit)
        loadLog()
    }

    fun stash() = managerScope.launch {
        stashManager.stash(safeGit)
        loadStatus()
    }

    fun popStash() = managerScope.launch {
        stashManager.popStash(safeGit)
        loadStatus()
    }

    fun createBranch(branchName: String) = managerScope.launch {
        branchesManager.createBranch(safeGit, branchName)
    }

    fun deleteBranch(branch: Ref) = managerScope.launch {
        branchesManager.deleteBranch(safeGit, branch)
    }

    fun resetStaged(diffEntry: DiffEntry) = managerScope.launch {
        statusManager.reset(safeGit, diffEntry, staged = true)
    }

    fun resetUnstaged(diffEntry: DiffEntry) = managerScope.launch {
        statusManager.reset(safeGit, diffEntry, staged = false)
    }
}


sealed class RepositorySelectionStatus {
    object None : RepositorySelectionStatus()
    object Loading : RepositorySelectionStatus()
    data class Open(val repository: Repository) : RepositorySelectionStatus()
}