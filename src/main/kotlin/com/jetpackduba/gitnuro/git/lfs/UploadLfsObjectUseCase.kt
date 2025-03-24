package com.jetpackduba.gitnuro.git.lfs

import com.jetpackduba.gitnuro.NetworkConstants
import com.jetpackduba.gitnuro.Result
import com.jetpackduba.gitnuro.lfs.LfsError
import com.jetpackduba.gitnuro.lfs.LfsRepository
import com.jetpackduba.gitnuro.models.lfs.LfsObject
import org.eclipse.jgit.lfs.Lfs
import org.eclipse.jgit.lfs.lib.AnyLongObjectId
import org.eclipse.jgit.lib.Repository
import javax.inject.Inject

class UploadLfsObjectUseCase @Inject constructor(
    private val lfsRepository: LfsRepository,
    private val provideLfsCredentialsUseCase: ProvideLfsCredentialsUseCase,
) {
    suspend operator fun invoke(
        lfsServerUrl: String,
        lfsObject: LfsObject,
        repository: Repository,
        oid: AnyLongObjectId,
    ): Result<Unit, LfsError> {
        val uploadUrl = lfsObject.actions?.upload?.href ?: return Result.Ok(Unit)

        val lfs = Lfs(repository)
        val uploadHeaders = lfsObject.actions.upload.header.orEmpty()

        return if (uploadHeaders.containsKey(NetworkConstants.AUTH_HEADER)) {
            lfsRepository.uploadObject(
                uploadUrl,
                oid.name(),
                lfs.getMediaFile(oid),
                lfsObject.size,
                uploadHeaders,
                null,
                null,
            )
        } else {
            provideLfsCredentialsUseCase(
                url = lfsServerUrl,
            ) { user, password ->
                lfsRepository.uploadObject(
                    uploadUrl,
                    oid.name(),
                    lfs.getMediaFile(oid),
                    lfsObject.size,
                    uploadHeaders,
                    user,
                    password,
                )
            }
        }
    }
}