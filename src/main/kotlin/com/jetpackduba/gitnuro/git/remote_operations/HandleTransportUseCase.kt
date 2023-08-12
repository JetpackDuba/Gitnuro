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
    suspend operator fun invoke(git: Git?, block: suspend CredentialsHandler.() -> Unit) {
        var cache: CredentialsCache? = null

        val credentialsHandler = object: CredentialsHandler {
            override fun handleTransport(transport: Transport?) {
                cache = when (transport) {
                    is SshTransport -> {
                        val sshSessionFactory = sessionManager.generateSshSessionFactory()
                        transport.sshSessionFactory = sshSessionFactory
                        sshSessionFactory
                    }

                    is HttpTransport -> {
                        val httpCredentials = httpCredentialsProvider.create(git)
                        transport.credentialsProvider = httpCredentials
                        httpCredentials
                    }

                    else -> {
                        null
                    }
                }
            }
        }

        credentialsHandler.block()
        cache?.cacheCredentialsIfNeeded()
    }
}
interface CredentialsCache {
    suspend fun cacheCredentialsIfNeeded()
}

interface CredentialsHandler {
    fun handleTransport(transport: Transport?)
}