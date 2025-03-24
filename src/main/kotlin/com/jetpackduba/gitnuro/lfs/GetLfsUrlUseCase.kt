package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.extensions.isHttpOrHttps
import com.jetpackduba.gitnuro.git.branches.GetBranchesUseCase
import com.jetpackduba.gitnuro.git.branches.GetCurrentBranchUseCase
import com.jetpackduba.gitnuro.git.branches.GetTrackingBranchUseCase
import com.jetpackduba.gitnuro.logging.printError
import com.jetpackduba.gitnuro.models.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsPrepareUploadObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsRef
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lfs.internal.LfsConfig
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileBasedConfig
import java.io.File
import javax.inject.Inject

private const val TAG = "LfsRepository"

class GetLfsUrlUseCase @Inject constructor(
    private val getTrackingBranchUseCase: GetTrackingBranchUseCase,
    private val getCurrentBranchUseCase: GetCurrentBranchUseCase,
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
        val remotePath = if (remoteName != null) {
            remoteName
        } else {
            val currentBranch = getCurrentBranchUseCase(git)

            if (currentBranch == null) {
                printError(TAG, "Current branch is null and couldn't obtain tracking branch remote.")
                return null
            }

            val trackingBranch = getTrackingBranchUseCase(git, currentBranch)
            val currentBranchTracedRemote = trackingBranch?.remote

            if (currentBranchTracedRemote != null) {
                Constants.R_REMOTES + currentBranchTracedRemote
            } else {
                printError(TAG, "Remote name is null and couldn't obtain tracking branch remote.")
                return null
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