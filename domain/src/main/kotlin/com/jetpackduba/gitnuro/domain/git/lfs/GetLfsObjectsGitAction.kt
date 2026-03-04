package com.jetpackduba.gitnuro.domain.git.lfs

import com.jetpackduba.gitnuro.common.Either
import com.jetpackduba.gitnuro.domain.errors.LfsError
import com.jetpackduba.gitnuro.domain.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.domain.lfs.LfsObjects
import com.jetpackduba.gitnuro.domain.models.OperationType
import com.jetpackduba.gitnuro.domain.network.NetworkConstants
import com.jetpackduba.gitnuro.domain.repositories.LfsRepository
import javax.inject.Inject

class GetLfsObjectsGitAction @Inject constructor(
    private val lfsRepository: LfsRepository,
    private val provideLfsCredentialsGitAction: ProvideLfsCredentialsGitAction,
) {
    suspend operator fun invoke(
        lfsServerUrl: String,
        operationType: OperationType,
        branch: String,
        lfsObjectBatches: List<LfsObjectBatch>,
        headers: Map<String, String>,
    ): Either<LfsObjects, LfsError> {
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
            provideLfsCredentialsGitAction(
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