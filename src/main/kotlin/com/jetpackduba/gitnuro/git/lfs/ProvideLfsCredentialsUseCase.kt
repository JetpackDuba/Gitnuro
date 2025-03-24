package com.jetpackduba.gitnuro.git.lfs

import com.jetpackduba.gitnuro.Result
import com.jetpackduba.gitnuro.credentials.CredentialsCacheRepository
import com.jetpackduba.gitnuro.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.lfs.LfsError
import io.ktor.http.*
import javax.inject.Inject

class ProvideLfsCredentialsUseCase @Inject constructor(
    private val credentialsCacheRepository: CredentialsCacheRepository,
    private val credentialsStateManager: CredentialsStateManager,
) {
    suspend operator fun <T> invoke(
        url: String,
        callback: suspend (username: String?, password: String?) -> Result<T, LfsError>,
    ): Result<T, LfsError> {
        var res: Result<T, LfsError>


        res = callback(null, null)

        if (!res.isUnauthorizedError()) {
            return res
        }

        val credentialsCached = credentialsCacheRepository.getCachedHttpCredentials(url, isLfs = true)

        if (credentialsCached != null) {
            res = callback(credentialsCached.user, credentialsCached.password)

            if (!res.isUnauthorizedError()) {
                return res
            }
        }

        do {
            val lfsCredentials = credentialsStateManager.requestLfsCredentials()
            res = callback(lfsCredentials.user, lfsCredentials.password)
        } while (res.isUnauthorizedError())

        return res
    }

    private fun <T> Result<T, LfsError>.isUnauthorizedError(): Boolean {
        return this is Result.Err &&
                this.error is LfsError.HttpError &&
                this.error.code == HttpStatusCode.Unauthorized
    }
}