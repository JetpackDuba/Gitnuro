package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.LfsError
import com.jetpackduba.gitnuro.domain.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.domain.lfs.LfsObjects
import com.jetpackduba.gitnuro.domain.lfs.LfsPrepareUploadObjectBatch
import com.jetpackduba.gitnuro.domain.lfs.LfsRef
import com.jetpackduba.gitnuro.domain.models.OperationType
import com.jetpackduba.gitnuro.domain.repositories.LfsRepository
import javax.inject.Inject

private const val TAG = "LfsRepository"

class NetworkLfsRepository @Inject constructor(
    private val lfsNetworkDataSource: LfsNetworkDataSource,
) : LfsRepository by lfsNetworkDataSource {
    override suspend fun getLfsObjects(
        lfsServerUrl: String,
        operationType: OperationType,
        branch: String,
        objects: List<LfsObjectBatch>,
        username: String?,
        password: String?,
        headers: Map<String, String>,
    ): Either<LfsObjects, LfsError> {
        return postBatchObjects(
            lfsServerUrl,
            createLfsPrepareUploadObjectBatch(
                operationType,
                branch = branch,
                objects = objects,
            ),
            headers = headers,
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


