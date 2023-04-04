package com.jetpackduba.gitnuro.git.remote_operations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ProgressMonitor
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RefSpec
import javax.inject.Inject

class FetchAllBranchesUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
) {
    suspend operator fun invoke(git: Git) = withContext(Dispatchers.IO) {
        val remotes = git.remoteList().call()

        delay(4000)

        for (remote in remotes) {
            val refSpecs = remote.fetchRefSpecs.ifEmpty {
                listOf(RefSpec("refs/heads/*:refs/remotes/${remote.name}/*"))
            }

            git.fetch()
                .setRemote(remote.name)
                .setRefSpecs(refSpecs)
                .setRemoveDeletedRefs(true)
                .setTransportConfigCallback { handleTransportUseCase(it, git) }
                .setCredentialsProvider(CredentialsProvider.getDefault())
                .setProgressMonitor(object: ProgressMonitor {
                    override fun start(totalTasks: Int) {}

                    override fun beginTask(title: String?, totalWork: Int) {}

                    override fun update(completed: Int) {}

                    override fun endTask() {}

                    override fun isCancelled(): Boolean = isActive
                })
                .call()
        }
    }
}