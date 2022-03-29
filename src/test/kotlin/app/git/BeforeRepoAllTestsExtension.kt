package app.git

import app.credentials.GProcess
import app.credentials.GRemoteSession
import app.credentials.GSessionManager
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL
import java.io.File
import kotlin.io.path.createTempDirectory

private const val REPO_URL = "https://github.com/JetpackDuba/Gitnuro_TestsRepo.git"
private val tempDirPath = createTempDirectory("gitnuro_")
val tempDir: File = tempDirPath.toFile()
lateinit var repoDir: File

class BeforeRepoAllTestsExtension : BeforeAllCallback, AfterAllCallback {
    private var started = false

    override fun beforeAll(context: ExtensionContext) = runBlocking {
        if (!started) {
            repoDir = File(tempDir, "repo")

            started = true
            // Your "before all tests" startup logic goes here
            // The following line registers a callback hook when the root test context is shut down
            context.root.getStore(GLOBAL).put("any unique name", this)

            val remoteOperationsManager = RemoteOperationsManager(GSessionManager { GRemoteSession { GProcess() } })
            remoteOperationsManager.clone(repoDir, REPO_URL)
        }
    }

    override fun afterAll(context: ExtensionContext?) {
        // Your "after all tests" logic goes here

        tempDir.deleteRecursively()
    }
}
