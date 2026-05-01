package com.jetpackduba.gitnuro.data.git.submodules

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.git.remote_operations.HandleTransportGitAction
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IUpdateSubmoduleGitAction
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import org.eclipse.jgit.api.CloneCommand
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.ProgressMonitor
import javax.inject.Inject

private const val TAG = "UpdateSubmoduleGitAction"

class UpdateSubmoduleGitAction @Inject constructor(
    private val handleTransportGitAction: HandleTransportGitAction,
    private val jgit: JGit,
) : IUpdateSubmoduleGitAction {
    override suspend operator fun invoke(repositoryPath: String, path: String) = jgit.provide(repositoryPath) { git ->
        coroutineScope {
            handleTransportGitAction(repositoryPath) {
                git
                    .submoduleUpdate()
                    .addPath(path)
                    .setCallback(
                        object : CloneCommand.Callback {
                            override fun initializedSubmodules(submodules: MutableCollection<String>?) {}
                            override fun cloningSubmodule(path: String?) {}
                            override fun checkingOut(commit: AnyObjectId?, path: String?) {}
                        }
                    )
                    .setTransportConfigCallback { handleTransport(it) }
                    .setProgressMonitor(object : ProgressMonitor {
                        override fun start(totalTasks: Int) {}
                        override fun beginTask(title: String?, totalWork: Int) {}
                        override fun update(completed: Int) {}
                        override fun endTask() {}
                        override fun isCancelled(): Boolean = !isActive
                        override fun showDuration(enabled: Boolean) {}
                    })
                    .call()

                Unit
            }.bind()
        }
    }
}