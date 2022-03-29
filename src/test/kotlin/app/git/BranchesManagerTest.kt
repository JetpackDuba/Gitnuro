package app.git

import app.TestUtils.copyDir
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(BeforeRepoAllTestsExtension::class)
class BranchesManagerTest {
    private lateinit var repo: Repository
    private lateinit var git: Git
    private lateinit var branchesManagerTestDir: File

    @BeforeEach
    fun setup() {
        branchesManagerTestDir = File(tempDir, "branches_manager")
        branchesManagerTestDir.mkdir()

        copyDir(repoDir.absolutePath, branchesManagerTestDir.absolutePath)

        repo = RepositoryManager().openRepository(branchesManagerTestDir)
        git = Git(repo)
    }

    @AfterEach
    fun clean() {
        repo.close()
        branchesManagerTestDir.deleteRecursively()
    }

    @org.junit.jupiter.api.Test
    fun currentBranchRef() = runBlocking {
        val branchesManager = BranchesManager()
        val currentBranchRef = branchesManager.currentBranchRef(Git(repo))
        assertEquals(currentBranchRef?.name, "refs/heads/main")
    }

    @org.junit.jupiter.api.Test
    fun getBranches() = runBlocking {
        val branchesManager = BranchesManager()
        val branches = branchesManager.getBranches(git)
        assertEquals(branches.count(), 1)
        val containsMain = branches.any { it.name == "refs/heads/main" }
        assert(containsMain) { println("Error: Branch main does not exist") }
    }

    @org.junit.jupiter.api.Test
    fun createBranch() = runBlocking {
        val branchName = "test"
        val branchesManager = BranchesManager()
        branchesManager.createBranch(git, branchName)

        val branches = branchesManager.getBranches(git)
        assertEquals(branches.count(), 2)
        val containsNewBranch = branches.any { it.name == "refs/heads/$branchName" }

        assert(containsNewBranch) { println("Error: Branch $branchName does not exist") }
    }
//
//    @org.junit.jupiter.api.Test
//    fun createBranchOnCommit() {
//    }
//
//    @org.junit.jupiter.api.Test
//    fun deleteBranch() {
//    }
//
//    @org.junit.jupiter.api.Test
//    fun deleteLocallyRemoteBranches() {
//    }
//
//    @org.junit.jupiter.api.Test
//    fun remoteBranches() {
//    }
//
//    @org.junit.jupiter.api.Test
//    fun checkoutRef() {
//    }
}