package com.jetpackduba.gitnuro.git.lfs

import com.jetpackduba.gitnuro.NetworkConstants
import com.jetpackduba.gitnuro.Result
import com.jetpackduba.gitnuro.lfs.LfsError
import com.jetpackduba.gitnuro.lfs.LfsRepository
import com.jetpackduba.gitnuro.lfs.OperationType
import com.jetpackduba.gitnuro.models.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsObjects
import javax.inject.Inject

class GetLfsObjectsUseCase @Inject constructor(
    private val lfsRepository: LfsRepository,
    private val provideLfsCredentialsUseCase: ProvideLfsCredentialsUseCase,
) {
    suspend operator fun invoke(
        lfsServerUrl: String,
        operationType: OperationType,
        branch: String,
        lfsObjectBatches: List<LfsObjectBatch>,
        headers: Map<String, String>,
    ): Result<LfsObjects, LfsError> {
        return if (headers.containsKey(NetworkConstants.AUTH_HEADER)) {
            lfsRepository.getLfsObjects(
                lfsServerUrl,
                operationType = operationType,
                branch = branch,
                objects = lfsObjectBatches,
                headers = headers,
                username = null,
                password = null,
            )
        } else {
            provideLfsCredentialsUseCase(
                url = lfsServerUrl,
            ) { user, password ->
                lfsRepository.getLfsObjects(
                    lfsServerUrl,
                    operationType = operationType,
                    branch = branch,
                    objects = lfsObjectBatches,
                    headers = headers,
                    username = user,
                    password = password,
                )
            }
        }
    }
}