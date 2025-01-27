package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.models.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsPrepareUploadObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsRef
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileBasedConfig
import java.io.File
import javax.inject.Inject

class LfsRepository @Inject constructor(
    private val lfsNetworkDataSource: LfsNetworkDataSource,
) : ILfsNetworkDataSource by lfsNetworkDataSource {
    fun getLfsRepositoryUrl(repository: Repository): String {
        // TODO This only gets the URL from considering happy path config
        val configFile = File(repository.workTree, ".lfsconfig")

        val config = FileBasedConfig(configFile, repository.fs)
        config.load()

        return config.getString("lfs", null, "url")
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