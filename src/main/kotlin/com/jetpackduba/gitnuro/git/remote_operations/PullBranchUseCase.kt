package com.jetpackduba.gitnuro.git.remote_operations

import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class PullBranchUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
    private val appSettingsRepository: AppSettingsRepository,
    private val hasPullResultConflictsUseCase: HasPullResultConflictsUseCase,
) {
    suspend operator fun invoke(git: Git, pullType: PullType): PullHasConflicts = withContext(Dispatchers.IO) {
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

            return@handleTransportUseCase hasPullResultConflictsUseCase(pullWithRebase, pullResult)
        }
    }
}


enum class PullType {
    REBASE,
    MERGE,
    DEFAULT
}