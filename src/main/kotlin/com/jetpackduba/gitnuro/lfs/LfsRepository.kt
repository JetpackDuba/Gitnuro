package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.git.branches.GetBranchesUseCase
import com.jetpackduba.gitnuro.git.branches.GetCurrentBranchUseCase
import com.jetpackduba.gitnuro.git.branches.GetTrackingBranchUseCase
import com.jetpackduba.gitnuro.logging.printError
import com.jetpackduba.gitnuro.models.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsPrepareUploadObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsRef
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileBasedConfig
import java.io.File
import javax.inject.Inject

private const val TAG = "LfsRepository"

class LfsRepository @Inject constructor(
    private val lfsNetworkDataSource: LfsNetworkDataSource,
) : ILfsNetworkDataSource by lfsNetworkDataSource {
    suspend fun getLfsRepositoryUrl(repository: Repository, remoteName: String?): String? {
        // TODO This only gets the URL from considering happy path config
        val configFile = File(repository.workTree, ".lfsconfig")

        val config = FileBasedConfig(configFile, repository.fs)
        config.load()

        val lfsConfigUrl = config.getString("lfs", null, "url")

        return lfsConfigUrl ?: getLfsUrlFromRemote(repository, remoteName)
    }

    private suspend fun getLfsUrlFromRemote(repository: Repository, remoteName: String?): String? {
        val remotePath = if(remoteName != null) {
            remoteName
        } else {
            val q = GetTrackingBranchUseCase().invoke(Git(repository), GetCurrentBranchUseCase(GetBranchesUseCase()).invoke(Git(repository))!!)
            val currentBranchTracedRemote = q?.remote

            if (currentBranchTracedRemote != null) {
                Constants.R_REMOTES + currentBranchTracedRemote
            } else {
                printError(TAG, "Remote name is null and couldn't obtain tracking branch remote.")
                return null
            }
        }

        val remoteUrl = Git(repository)
            .remoteList()
            .call()
            .firstOrNull { Constants.R_REMOTES + it.name == remotePath }
            ?.urIs
            ?.firstOrNull()

        return if (remoteUrl != null) {
            "$remoteUrl/info/lfs"
        } else {
            null
        }
    }
}

fun createLfsPrepareUploadObjectBatch(
    operation: OperationType,
    algo: String = "sha256",
    branch: String,
    objects: List<LfsObjectBatch>,
): LfsPrepareUploadObjectBatch {
    return LfsPrepareUploadObjectBatch(
        operation = operation.value,
        objects = objects,
        transfers = listOf(
            "basic",
            // TODO Add support for standalone files and SSH once they are stable https://github.com/git-lfs/git-lfs/blob/main/docs/api/README.md
            //"lfs-standalone-file",
            //"ssh",
        ),
        ref = LfsRef(branch),
        hashAlgo = algo,
    )
}

enum class OperationType(val value: String) {
    UPLOAD("upload"),
    DOWNLOAD("download")
}