package com.jetpackduba.gitnuro.git

import com.jetpackduba.gitnuro.credentials.GpgCredentialsProvider
import org.bouncycastle.openpgp.PGPException
import org.eclipse.jgit.api.errors.CanceledException
import org.eclipse.jgit.gpg.bc.internal.BouncyCastleGpgSigner
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject
import javax.inject.Provider

private const val INVALID_PASSWORD_MESSAGE = "Is the entered passphrase correct?"

class AppGpgSigner @Inject constructor(
    private val gpgCredentials: GpgCredentialsProvider,
) : BouncyCastleGpgSigner() {

    override fun sign(
        repository: Repository?,
        config: GpgConfig?,
        data: ByteArray?,
        committer: PersonIdent?,
        signingKey: String?,
        credentialsProvider: CredentialsProvider?
    ): GpgSignature {
        return try {
            var gpgSignature: GpgSignature? = null
            retryIfWrongPassphrase { isRetry ->
                gpgCredentials.isRetry = isRetry
                gpgSignature = super.sign(repository, config, data, committer, signingKey, gpgCredentials)
                gpgCredentials.savePasswordInMemory()
            }

            gpgSignature!!
        } catch (ex: CanceledException) {
            println("Signing cancelled")
            throw ex
        }

    }

    override fun canLocateSigningKey(
        repository: Repository?,
        config: GpgConfig?,
        committer: PersonIdent?,
        signingKey: String?,
        credentialsProvider: CredentialsProvider?
    ): Boolean {
        return super.canLocateSigningKey(repository, config, committer, signingKey, gpgCredentials)
    }

    override fun signObject(
        repository: Repository?,
        config: GpgConfig?,
        `object`: ObjectBuilder?,
        committer: PersonIdent?,
        signingKey: String?,
        credentialsProvider: CredentialsProvider?
    ) {
        val gpgCredentialsProvider = gpgCredentials

        try {
            retryIfWrongPassphrase { isRetry ->
                gpgCredentialsProvider.isRetry = isRetry
                super.signObject(repository, config, `object`, committer, signingKey, credentialsProvider)
                gpgCredentialsProvider.savePasswordInMemory()
            }
        } catch (ex: CanceledException) {
            println("Signing cancelled")
        }
    }

    private fun retryIfWrongPassphrase(block: (isRetry: Boolean) -> Unit) {
        var isPasswordCorrect = false
        var retries = 0

        while (!isPasswordCorrect) {
            isPasswordCorrect = true

            try {
                block(retries > 0)
            } catch (ex: Exception) {
                ex.printStackTrace()

                val pgpException = getPgpExceptionTypeOrNull(ex)
                val pgpMessage = pgpException?.message

                if (pgpMessage != null && pgpMessage.contains(INVALID_PASSWORD_MESSAGE)) {
                    isPasswordCorrect = false // Only set it to false if we've got this specific message
                    retries++
                } else
                    throw ex
            }
        }
    }

    private fun getPgpExceptionTypeOrNull(ex: Throwable?): PGPException? {
        var currentException: Throwable? = ex

        while (currentException != null) {
            if (currentException is PGPException) {
                return currentException
            } else {
                currentException = when (currentException.cause) {
                    currentException -> null // Cause can be the same as current exception, so just return null
                    else -> currentException.cause
                }
            }
        }

        return null
    }
}