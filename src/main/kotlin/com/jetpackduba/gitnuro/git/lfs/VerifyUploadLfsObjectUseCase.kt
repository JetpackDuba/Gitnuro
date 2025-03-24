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


class VerifyUploadLfsObjectUseCase @Inject constructor(
    private val lfsRepository: LfsRepository,
    private val provideLfsCredentialsUseCase: ProvideLfsCredentialsUseCase,
) {
    suspend operator fun invoke(
        lfsServerUrl: String,
        lfsObject: LfsObject,
        oid: AnyLongObjectId,
    ): Result<Unit, LfsError> {
        val verifyUrl = lfsObject.actions?.verify?.href

        if (verifyUrl != null) {
            val verifyHeaders = lfsObject.actions.verify.header.orEmpty()
            return if (verifyHeaders.containsKey(NetworkConstants.AUTH_HEADER)) {
                lfsRepository.verify(
                    verifyUrl,
                    oid.name(),
                    lfsObject.size,
                    verifyHeaders,
                    null,
                    null,
                )
            } else {
                provideLfsCredentialsUseCase(
                    url = lfsServerUrl,
                ) { user, password ->
                    lfsRepository.verify(
                        verifyUrl,
                        oid.name(),
                        lfsObject.size,
                        verifyHeaders,
                        user,
                        password,
                    )
                }
            }
        }

        return Result.Ok(Unit)
    }
}