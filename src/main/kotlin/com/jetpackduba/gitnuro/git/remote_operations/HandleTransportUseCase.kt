package com.jetpackduba.gitnuro.git.remote_operations

import com.jetpackduba.gitnuro.credentials.GSessionManager
import com.jetpackduba.gitnuro.credentials.HttpCredentialsProvider
import org.eclipse.jgit.transport.HttpTransport
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import javax.inject.Inject
import javax.inject.Provider

class HandleTransportUseCase @Inject constructor(
    private val sessionManager: GSessionManager,
    private val httpCredentialsProvider: Provider<HttpCredentialsProvider>,
) {
    operator fun invoke(transport: Transport?) {
        if (transport is SshTransport) {
            transport.sshSessionFactory = sessionManager.generateSshSessionFactory()
        } else if (transport is HttpTransport) {
            transport.credentialsProvider = httpCredentialsProvider.get()
        }
    }
}