package com.jetpackduba.gitnuro.domain.git.submodules

import com.jetpackduba.gitnuro.domain.git.remote_operations.HandleTransportGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class AddSubmoduleGitAction @Inject constructor(
    private val handleTransportGitAction: HandleTransportGitAction,
) {
    suspend operator fun invoke(git: Git, name: String, path: String, uri: String): Unit = withContext(Dispatchers.IO) {
        handleTransportGitAction(git) {
            git.submoduleAdd()
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
        }
    }
}