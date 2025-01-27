package com.jetpackduba.gitnuro.git.remote_operations

import com.jetpackduba.gitnuro.repositories.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class PullBranchUseCase @Inject constructor(
    private val handleTransportUseCase: HandleTransportUseCase,
    private val appSettingsRepository: AppSettingsRepository,
    private val hasPullResultConflictsUseCase: HasPullResultConflictsUseCase,
) {
    suspend operator fun invoke(git: Git, pullType: PullType): PullHasConflicts = withContext(Dispatchers.IO) {
        useBuiltinLfs(git.repository) {
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
}

inline fun <R> useBuiltinLfs(
    repository: Repository,
    callback: () -> R,
): R {
    val lfsSubsection = "lfs"

    val names = repository.config.getNames(
        ConfigConstants.CONFIG_FILTER_SECTION,
        lfsSubsection,
    )

    // Check if it was set before (if using egit) to restore its value later
    val hadBuiltinLfsOriginalValueSet = names.contains(ConfigConstants.CONFIG_KEY_USEJGITBUILTIN)

    val builtinLfsOriginalValue = repository.config.getBoolean(
        ConfigConstants.CONFIG_FILTER_SECTION,
        lfsSubsection,
        ConfigConstants.CONFIG_KEY_USEJGITBUILTIN,
        false,
    )

    repository.config.setBoolean(
        ConfigConstants.CONFIG_FILTER_SECTION,
        lfsSubsection,
        ConfigConstants.CONFIG_KEY_USEJGITBUILTIN,
        true,
    )

    val result = callback()

    if (hadBuiltinLfsOriginalValueSet) {
        repository.config.setBoolean(
            ConfigConstants.CONFIG_FILTER_SECTION,
            lfsSubsection,
            ConfigConstants.CONFIG_KEY_USEJGITBUILTIN,
            builtinLfsOriginalValue,
        )
    } else {
        repository.config.unset(
            ConfigConstants.CONFIG_FILTER_SECTION,
            lfsSubsection,
            ConfigConstants.CONFIG_KEY_USEJGITBUILTIN,
        )
    }

    return result
}


enum class PullType {
    REBASE,
    MERGE,
    DEFAULT
}