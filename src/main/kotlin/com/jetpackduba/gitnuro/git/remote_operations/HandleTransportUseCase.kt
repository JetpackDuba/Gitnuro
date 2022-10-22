package com.jetpackduba.gitnuro.git.remote_operations

import com.jetpackduba.gitnuro.credentials.GSessionManager
import com.jetpackduba.gitnuro.di.factories.HttpCredentialsFactory
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.HttpTransport
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import javax.inject.Inject

class HandleTransportUseCase @Inject constructor(
    private val sessionManager: GSessionManager,
    private val httpCredentialsProvider: HttpCredentialsFactory,
) {
    operator fun invoke(transport: Transport?, git: Git?) {
        if (transport is SshTransport) {
            transport.sshSessionFactory = sessionManager.generateSshSessionFactory()
        } else if (transport is HttpTransport) {
            transport.credentialsProvider = httpCredentialsProvider.create(git)
        }
    }
}