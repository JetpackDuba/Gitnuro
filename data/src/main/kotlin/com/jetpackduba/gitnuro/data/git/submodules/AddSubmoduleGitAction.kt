package com.jetpackduba.gitnuro.data.git.submodules

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.git.remote_operations.HandleTransportGitAction
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IAddSubmoduleGitAction
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class AddSubmoduleGitAction @Inject constructor(
    private val handleTransportGitAction: HandleTransportGitAction,
    private val jgit: JGit,
) : IAddSubmoduleGitAction {
    override suspend operator fun invoke(repositoryPath: String, name: String, path: String, uri: String) =
        jgit.provide(repositoryPath) { git ->
            coroutineScope {
                handleTransportGitAction(repositoryPath) {
                    git
                        .submoduleAdd()
                        .setName(name)
                        .setPath(path)
                        .setURI(uri)
                        .setTransportConfigCallback { handleTransport(it) }
                        .setCredentialsProvider(CredentialsProvider.getDefault())
                        .setProgressMonitor(object : ProgressMonitor {
                            override fun start(totalTasks: Int) {}
                            override fun beginTask(title: String?, totalWork: Int) {}
                            override fun update(completed: Int) {}
                            override fun endTask() {}
                            override fun showDuration(enabled: Boolean) {}

                            override fun isCancelled() = !isActive

                        })
                        .call()

                    Unit
                }.bind()

            }
        }
}