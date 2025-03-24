package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.NetworkConstants
import com.jetpackduba.gitnuro.Result
import com.jetpackduba.gitnuro.credentials.CredentialsAccepted
import com.jetpackduba.gitnuro.extensions.safeLet
import com.jetpackduba.gitnuro.models.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsObjects
import com.jetpackduba.gitnuro.models.lfs.LfsPrepareUploadObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsRef
import io.ktor.client.request.*
import io.ktor.http.*
import org.eclipse.jgit.lfs.Lfs
import org.eclipse.jgit.lfs.errors.LfsException
import org.eclipse.jgit.lfs.lib.AnyLongObjectId
import javax.inject.Inject

private const val TAG = "LfsRepository"

class LfsRepository @Inject constructor(
    private val lfsNetworkDataSource: LfsNetworkDataSource,
) : ILfsNetworkDataSource by lfsNetworkDataSource {
    suspend fun getLfsObjects(
        lfsServerUrl: String,
        operationType: OperationType,
        branch: String,
        objects: List<LfsObjectBatch>,
        username: String?,
        password: String?,
        headers: Map<String, String>,
    ): Result<LfsObjects, LfsError> {
        return postBatchObjects(
            lfsServerUrl,
            createLfsPrepareUploadObjectBatch(
                operationType,
                branch = branch,
                objects = objects,
            ),
            objHeaders = headers,
            username,
            password,
        )
    }

    private fun createLfsPrepareUploadObjectBatch(
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
                "lfs-standalone-file",
                "ssh",
            ),
            ref = LfsRef(branch),
            hashAlgo = algo,
        )
    }
}


enum class OperationType(val value: String) {
    UPLOAD("upload"),
    DOWNLOAD("download")
}