package com.jetpackduba.gitnuro.git

import com.jetpackduba.gitnuro.credentials.GpgCredentialsProvider
import org.bouncycastle.openpgp.PGPException
import org.eclipse.jgit.api.errors.CanceledException
import org.eclipse.jgit.gpg.bc.internal.BouncyCastleGpgSigner
import org.eclipse.jgit.lib.CommitBuilder
import org.eclipse.jgit.lib.GpgConfig
import org.eclipse.jgit.lib.ObjectBuilder
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject
import javax.inject.Provider

private const val INVALID_PASSWORD_MESSAGE = "Is the entered passphrase correct?"

class AppGpgSigner @Inject constructor(
    private val gpgCredentialsProvider: Provider<GpgCredentialsProvider>,
) : BouncyCastleGpgSigner() {
    override fun sign(
        commit: CommitBuilder,
        gpgSigningKey: String,
        committer: PersonIdent,
        credentialsProvider: CredentialsProvider?
    ) {
        super.sign(commit, gpgSigningKey, committer, gpgCredentialsProvider.get())
    }

    override fun canLocateSigningKey(
        gpgSigningKey: String,
        committer: PersonIdent,
        credentialsProvider: CredentialsProvider?
    ): Boolean {
        return super.canLocateSigningKey(gpgSigningKey, committer, gpgCredentialsProvider.get())
    }

    override fun canLocateSigningKey(
        gpgSigningKey: String,
        committer: PersonIdent,
        credentialsProvider: CredentialsProvider?,
        config: GpgConfig?
    ): Boolean {
        return super.canLocateSigningKey(gpgSigningKey, committer, gpgCredentialsProvider.get(), config)
    }

    override fun signObject(
        `object`: ObjectBuilder,
        gpgSigningKey: String?,
        committer: PersonIdent,
        credentialsProvider: CredentialsProvider?,
        config: GpgConfig?
    ) {
        val gpgCredentialsProvider = gpgCredentialsProvider.get()

        try {
            retryIfWrongPassphrase { isRetry ->
                gpgCredentialsProvider.isRetry = isRetry
                super.signObject(`object`, gpgSigningKey, committer, gpgCredentialsProvider, config)
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