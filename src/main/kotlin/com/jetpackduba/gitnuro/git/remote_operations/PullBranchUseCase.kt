package com.jetpackduba.gitnuro.git.remote_operations

import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class PullBranchUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
    private val appSettingsRepository: AppSettingsRepository,
) {
    suspend operator fun invoke(git: Git, pullType: PullType) = withContext(Dispatchers.IO) {
        val pullWithRebase = when (pullType) {
            PullType.REBASE -> true
            PullType.MERGE -> false
            PullType.DEFAULT -> appSettingsRepository.pullRebase
        }

        handleTransportUseCase(git) {
            val pullResult = git
                .pull()
                .setTransportConfigCallback { this.handleTransport(it) }
                .setRebase(pullWithRebase)
                .setCredentialsProvider(CredentialsProvider.getDefault())
                .call()

            if (!pullResult.isSuccessful) {
                var message = "Pull failed"

                if (pullWithRebase) {
                    message = when (pullResult.rebaseResult.status) {
                        RebaseResult.Status.UNCOMMITTED_CHANGES -> "The pull with rebase has failed because you have got uncommitted changes"
                        RebaseResult.Status.CONFLICTS -> "Pull with rebase has conflicts, fix them to continue"
                        else -> message
                    }
                }

                throw Exception(message)
            }
        }
    }
}


enum class PullType {
    REBASE,
    MERGE,
    DEFAULT
}