package com.jetpackduba.gitnuro.git.submodules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
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
            .setProgressMonitor(object : ProgressMonitor {
                override fun start(totalTasks: Int) {}
                override fun beginTask(title: String?, totalWork: Int) {}
                override fun update(completed: Int) {}
                override fun endTask() {}
                override fun isCancelled(): Boolean = !isActive
                override fun showDuration(enabled: Boolean) {}
            })
            .call()
    }
}