package com.jetpackduba.gitnuro.git.remote_operations

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class FetchAllBranchesUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
) {
    suspend operator fun invoke(git: Git) = withContext(Dispatchers.IO) {
        val remotes = git.remoteList().call()

        for (remote in remotes) {
            git.fetch()
                .setRemote(remote.name)
                .setRefSpecs(remote.fetchRefSpecs)
                .setTransportConfigCallback { handleTransportUseCase(it, git) }
                .setCredentialsProvider(CredentialsProvider.getDefault())
                .call()
        }
    }
}