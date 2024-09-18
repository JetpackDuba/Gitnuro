package com.jetpackduba.gitnuro.git.remote_operations

import com.jetpackduba.gitnuro.extensions.remoteName
import com.jetpackduba.gitnuro.extensions.simpleName
import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class PullFromSpecificBranchUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
    private val hasPullResultConflictsUseCase: HasPullResultConflictsUseCase,
    private val appSettingsRepository: AppSettingsRepository,
) {
    suspend operator fun invoke(git: Git, remoteBranch: Ref): PullHasConflicts =
        withContext(Dispatchers.IO) {
            val pullWithRebase = appSettingsRepository.pullRebase

            handleTransportUseCase(git) {
                val pullResult = git
                    .pull()
                    .setTransportConfigCallback { handleTransport(it) }
                    .setRemote(remoteBranch.remoteName)
                    .setRemoteBranchName(remoteBranch.simpleName)
                    .setRebase(pullWithRebase)
                    .setCredentialsProvider(CredentialsProvider.getDefault())
                    .call()

                return@handleTransportUseCase hasPullResultConflictsUseCase(pullWithRebase, pullResult)
            }
        }
}