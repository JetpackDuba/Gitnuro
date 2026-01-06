package com.jetpackduba.gitnuro.git.signers

import Signing
import com.jetpackduba.gitnuro.credentials.CredentialsStateManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.transport.CredentialsProvider
import javax.inject.Inject

class SshSigner @Inject constructor(
    private val credentialsStateManager: CredentialsStateManager,
) : Signer {
    override fun sign(
        repository: Repository?,
        config: GpgConfig?,
        data: ByteArray,
        committer: PersonIdent?,
        signingKey: String?,
        credentialsProvider: CredentialsProvider?
    ): GpgSignature {
        // TODO Do we handle null signing key differently?
        if (signingKey == null) {
            throw CancellationException("Signing key not specified")
        }

        val credentials = runBlocking { // TODO Run blocking perhaps could be replaced?
            credentialsStateManager.requestSshCredentials()
        }
        val result = Signing.signData(data, signingKey, credentials.password)

        return GpgSignature(result.toByteArray(Charsets.UTF_8))
    }

    override fun canLocateSigningKey(
        repository: Repository?,
        config: GpgConfig?,
        committer: PersonIdent?,
        signingKey: String?,
        credentialsProvider: CredentialsProvider?
    ): Boolean {
        return true
    }

}
