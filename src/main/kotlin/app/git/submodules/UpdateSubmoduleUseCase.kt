package app.git.submodules

import app.logging.printLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.ProgressMonitor
import javax.inject.Inject

private const val TAG = "UpdateSubmoduleUseCase"

class UpdateSubmoduleUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, path: String) = withContext(Dispatchers.IO) {
        git.submoduleUpdate()
            .addPath(path)
            .setCallback(
                object : CloneCommand.Callback {
                    override fun initializedSubmodules(submodules: MutableCollection<String>?) {

                    }

                    override fun cloningSubmodule(path: String?) {

                    }

                    override fun checkingOut(commit: AnyObjectId?, path: String?) {

                    }

                }
            )
            .setProgressMonitor(object: ProgressMonitor {
                override fun start(totalTasks: Int) {
                    printLog(TAG, "start $totalTasks")
                }

                override fun beginTask(title: String?, totalWork: Int) {
                    printLog(TAG, "being task $title $totalWork")
                }

                override fun update(completed: Int) {
                    printLog(TAG, "Completed $completed")
                }

                override fun endTask() {
                    printLog(TAG, "endtask")
                }

                override fun isCancelled(): Boolean {
                    printLog(TAG, "isCancelled")
                    return false
                }

            })
            .call()
    }
}