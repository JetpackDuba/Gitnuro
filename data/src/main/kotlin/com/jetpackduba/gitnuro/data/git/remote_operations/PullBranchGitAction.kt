package com.jetpackduba.gitnuro.data.git.remote_operations

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.git.stash.DeleteStashGitAction
import com.jetpackduba.gitnuro.data.git.stash.SnapshotStashCreateCommand
import com.jetpackduba.gitnuro.data.git.workspace.CheckHasUncommittedChangesGitAction
import com.jetpackduba.gitnuro.data.mappers.JGitCommitMapper
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IPullBranchGitAction
import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.Commit
import com.jetpackduba.gitnuro.domain.models.PullType
import org.eclipse.jgit.lib.ConfigConstants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class PullBranchGitAction @Inject constructor(
    private val checkHasUncommittedChangesGitAction: CheckHasUncommittedChangesGitAction,
    private val handleTransportGitAction: HandleTransportGitAction,
    private val hasPullResultConflictsGitAction: HasPullResultConflictsGitAction,
    private val deleteStashGitAction: DeleteStashGitAction,
    private val commitMapper: JGitCommitMapper,
    private val jgit: JGit,
) : IPullBranchGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        pullType: PullType,
        mergeAutoStash: Boolean, // TODO Fix this after refactor
        remoteBranch: Branch?,
    ) = jgit.provide(repositoryPath) { git ->
        useBuiltinLfs(git.repository) {
            val pullWithRebase = when (pullType) {
                PullType.REBASE -> true
                else -> false
            }

            val pullWithMerge = !pullWithRebase
            var backupStash: Commit? = null

            // TODO Move this logic to domain layer like in MergeBranchUseCase
            if (mergeAutoStash && pullWithMerge) {
                val hasUncommitedChanges = checkHasUncommittedChangesGitAction(repositoryPath).bind()
                if (hasUncommitedChanges) {
                    val snapshotStashCreateCommand = SnapshotStashCreateCommand(
                        repository = git.repository,
                        workingDirectoryMessage = "FIX THIS AFTER REFACTOR"/*getString(
                            Res.string.pull_with_merge_automatic_stash_description,
                            git.repository.branch
                        )*/,
                        includeUntracked = true
                    )

                    backupStash = snapshotStashCreateCommand.call()?.let { commitMapper.toDomain(it) }
                }
            }

            val pullHasConflicts = handleTransportGitAction(repositoryPath) {
                val pullResult = git
                    .pull()
                    .setTransportConfigCallback { this.handleTransport(it) }
                    .setRebase(pullWithRebase)
                    .run {
                        if (remoteBranch != null) {
                            this.setRemote(remoteBranch.remoteName)
                                .setRemoteBranchName(remoteBranch.simpleName)
                        } else {
                            this
                        }
                    }
                    .setCredentialsProvider(CredentialsProvider.getDefault())
                    .call()

                return@handleTransportGitAction hasPullResultConflictsGitAction(pullWithRebase, pullResult)
            }.bind()

            if (!pullHasConflicts && backupStash != null) {
                deleteStashGitAction(git.repository.directory.absolutePath, backupStash)
            }

            pullHasConflicts
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
