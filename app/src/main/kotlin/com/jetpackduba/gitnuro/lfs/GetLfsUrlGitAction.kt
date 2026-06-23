package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.domain.errors.okOrNull
import com.jetpackduba.gitnuro.domain.extensions.isHttpOrHttps
import com.jetpackduba.gitnuro.domain.interfaces.IGetCurrentBranchGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetRemotesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetTrackingBranchGitAction
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileBasedConfig
import java.io.File
import javax.inject.Inject

private const val TAG = "LfsRepository"

class GetLfsUrlGitAction @Inject constructor(
    private val getTrackingBranchGitAction: IGetTrackingBranchGitAction,
    private val getCurrentBranchGitAction: IGetCurrentBranchGitAction,
    private val getRemotesGitAction: IGetRemotesGitAction,
) {
    suspend operator fun invoke(repository: Repository, remoteName: String?): String? {
        val git = Git(repository)
        // TODO This only gets the URL from considering happy path config
        val configFile = File(repository.workTree, ".lfsconfig")

        val config = FileBasedConfig(configFile, repository.fs)
        config.load()

        val lfsConfigUrl = config.getString("lfs", null, "url")

        return lfsConfigUrl ?: getLfsUrlFromRemote(git, remoteName)
    }

    private suspend fun getLfsUrlFromRemote(git: Git, remoteName: String?): String? {
        val repositoryPath = git.repository.directory.absolutePath

        val remotePath = if (remoteName != null) {
            remoteName
        } else {
            val currentBranch = getCurrentBranchGitAction(repositoryPath).okOrNull() // TODO Proper error handling?

            if (currentBranch == null) {
                printError(TAG, "Current branch is null and couldn't obtain tracking branch remote.")
                return null
            }

            val trackingBranch =
                getTrackingBranchGitAction(repositoryPath, currentBranch.name).okOrNull() // TODO Proper error handling?
            val currentBranchTracedRemote = trackingBranch?.remote

            if (currentBranchTracedRemote != null) {
                Constants.R_REMOTES + currentBranchTracedRemote
            } else {
                val remotes = getRemotesGitAction(repositoryPath).okOrNull().orEmpty()

                return if (remotes.count() == 1) {
                    remotes[0].fetchUri
                } else {
                    printError(TAG, "Remote name is null and couldn't obtain tracking branch remote.")
                    null
                }
            }
        }

        val remoteUrl = git
            .remoteList()
            .call()
            .firstOrNull { Constants.R_REMOTES + it.name == remotePath }
            ?.urIs
            ?.firstOrNull()

        return if (remoteUrl != null && remoteUrl.toString().isHttpOrHttps()) {
            "$remoteUrl/info/lfs"
        } else {
            remoteUrl?.toString()
        }
    }
}