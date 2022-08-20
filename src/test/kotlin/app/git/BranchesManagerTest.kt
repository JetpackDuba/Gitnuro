//package app.git
//
//import app.TestUtils.copyDir
//import kotlinx.coroutines.runBlocking
//import org.eclipse.jgit.api.Git
//import org.eclipse.jgit.lib.ObjectId
//import org.eclipse.jgit.lib.Repository
//import org.junit.jupiter.api.AfterEach
//import org.junit.jupiter.api.Assertions.assertEquals
//import org.junit.jupiter.api.Assertions.assertNotNull
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.extension.ExtendWith
//import java.io.File
//
//private const val DEFAULT_REMOTE = "origin"
//private const val DEFAULT_PRIMARY_BRANCH = "main"
//private const val DEFAULT_SECONDARY_BRANCH = "TestBranch1"
//
//private const val LOCAL_PREFIX = "refs/heads"
//private const val DEFAULT_PRIMARY_BRANCH_FULL_NAME = "$LOCAL_PREFIX/$DEFAULT_PRIMARY_BRANCH"
//private const val DEFAULT_SECONDARY_BRANCH_FULL_NAME = "$LOCAL_PREFIX/$DEFAULT_SECONDARY_BRANCH"
//
//private const val INITIAL_LOCAL_BRANCH_COUNT = 1
//private const val INITIAL_REMOTE_BRANCH_COUNT = 2
//
//private const val REMOTE_PREFIX = "refs/remotes/$DEFAULT_REMOTE"
//
//private val initialRemoteBranches = listOf(
//    "$REMOTE_PREFIX/$DEFAULT_PRIMARY_BRANCH",
//    "$REMOTE_PREFIX/$DEFAULT_SECONDARY_BRANCH",
//)
//
//@ExtendWith(BeforeRepoAllTestsExtension::class)
//class BranchesManagerTest {
//    private lateinit var repo: Repository
//    private lateinit var git: Git
//    private lateinit var branchesManagerTestDir: File
//    private val branchesManager = BranchesManager()
//
//    @BeforeEach
//    fun setUp() {
//        branchesManagerTestDir = File(tempDir, "branches_manager")
//        branchesManagerTestDir.mkdir()
//
//        copyDir(repoDir.absolutePath, branchesManagerTestDir.absolutePath)
//
//        repo = RepositoryManager().openRepository(branchesManagerTestDir)
//        git = Git(repo)
//    }
//
//    @AfterEach
//    fun tearDown() {
//        repo.close()
//        branchesManagerTestDir.deleteRecursively()
//    }
//
//    @org.junit.jupiter.api.Test
//    fun currentBranchRef() = runBlocking {
//        val currentBranchRef = branchesManager.currentBranchRef(Git(repo))
//        assertEquals(currentBranchRef?.name, "refs/heads/$DEFAULT_PRIMARY_BRANCH")
//    }
//
//    @org.junit.jupiter.api.Test
//    fun getBranches() = runBlocking {
//        val branchesManager = BranchesManager()
//        val branches = branchesManager.getBranches(git)
//        assertEquals(branches.count(), INITIAL_LOCAL_BRANCH_COUNT)
//        val containsMain = branches.any { it.name == "refs/heads/$DEFAULT_PRIMARY_BRANCH" }
//        assert(containsMain) { println("Error: Branch main does not exist") }
//    }
//
//    @org.junit.jupiter.api.Test
//    fun checkoutRef() = runBlocking {
//        val remoteBranchToCheckout = "$REMOTE_PREFIX/$DEFAULT_SECONDARY_BRANCH"
//
//        var currentBranch = branchesManager.currentBranchRef(git)
//        assertEquals(currentBranch?.name, DEFAULT_PRIMARY_BRANCH_FULL_NAME)
//
//        // Checkout a remote branch
//        var branchToCheckout = branchesManager.remoteBranches(git).first { it.name == remoteBranchToCheckout }
//        branchesManager.checkoutRef(git, branchToCheckout)
//
//        currentBranch = branchesManager.currentBranchRef(git)
//        assertEquals(DEFAULT_SECONDARY_BRANCH_FULL_NAME, currentBranch?.name)
//
//        // Checkout a local branch
//        branchToCheckout = branchesManager.getBranches(git).first { it.name == DEFAULT_PRIMARY_BRANCH_FULL_NAME }
//        branchesManager.checkoutRef(git, branchToCheckout)
//        currentBranch = branchesManager.currentBranchRef(git)
//
//        assertEquals(DEFAULT_PRIMARY_BRANCH_FULL_NAME, currentBranch?.name)
//    }
//
//    @org.junit.jupiter.api.Test
//    fun createBranch() = runBlocking {
//        val branchName = "test"
//        branchesManager.createBranch(git, branchName)
//
//        val branches = branchesManager.getBranches(git)
//        assertEquals(INITIAL_LOCAL_BRANCH_COUNT + 1, branches.count())
//        val containsNewBranch = branches.any { it.name == "refs/heads/$branchName" }
//
//        assert(containsNewBranch) { println("Error: Branch $branchName does not exist") }
//    }
//
//    @org.junit.jupiter.api.Test
//    fun createBranchOnCommit() = runBlocking {
//        val branchName = "test"
//        val commitId = "f66757e23dc5c43eccbe84d02c58245406c8f8f4"
//
//        val objectId = ObjectId.fromString(commitId)
//        val revCommit = repo.parseCommit(objectId)
//        branchesManager.createBranchOnCommit(git, branchName, revCommit)
//
//        val branches = branchesManager.getBranches(git)
//        assertEquals(INITIAL_LOCAL_BRANCH_COUNT + 1, branches.count())
//        val newBranch = branches.firstOrNull { it.name == "refs/heads/$branchName" }
//
//        assertNotNull(newBranch)
//        assertEquals(commitId, newBranch?.objectId?.name())
//    }
//
//    @org.junit.jupiter.api.Test
//    fun deleteBranch() = runBlocking {
//        val branchToDeleteName = "branch_to_delete"
//        val currentBranch = branchesManager.currentBranchRef(git) // should be "main"
//        assertNotNull(currentBranch)
//
//        val newBranch = branchesManager.createBranch(git, branchToDeleteName)
//        branchesManager.checkoutRef(git, currentBranch!!)
//
//        branchesManager.deleteBranch(git, newBranch)
//
//        val branches = branchesManager.getBranches(git)
//        assertEquals(INITIAL_LOCAL_BRANCH_COUNT, branches.count())
//    }
//
//    @org.junit.jupiter.api.Test
//    fun remoteBranches() = runBlocking {
//        val remoteBranches = branchesManager.remoteBranches(git)
//        assertEquals(remoteBranches.count(), INITIAL_REMOTE_BRANCH_COUNT)
//        remoteBranches.forEach { ref ->
//            assert(initialRemoteBranches.contains(ref.name))
//        }
//    }
//
//    @org.junit.jupiter.api.Test
//    fun deleteLocallyRemoteBranches() = runBlocking {
//        branchesManager.deleteLocallyRemoteBranches(git, initialRemoteBranches)
//
//        val branches = branchesManager.remoteBranches(git)
//        assertEquals(0, branches.count())
//    }
//}