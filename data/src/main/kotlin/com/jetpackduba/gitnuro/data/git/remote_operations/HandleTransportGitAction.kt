package com.jetpackduba.gitnuro.data.git.remote_operations

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.git.credentials.CredentialsCache
import com.jetpackduba.gitnuro.data.git.credentials.CredentialsHandler
import com.jetpackduba.gitnuro.data.git.credentials.GSessionManager
import com.jetpackduba.gitnuro.data.git.credentials.HttpCredentialsFactory
import com.jetpackduba.gitnuro.data.git.credentials.SshCredentialsProvider
import com.jetpackduba.gitnuro.domain.credentials.*
import org.eclipse.jgit.transport.HttpTransport
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import javax.inject.Inject
import javax.inject.Provider

class HandleTransportGitAction @Inject constructor(
    private val sessionManager: GSessionManager,
    private val httpCredentialsProvider: HttpCredentialsFactory,
    private val sshCredentialsProvider: Provider<SshCredentialsProvider>,
    private val jgit: JGit,
) {
    suspend operator fun <R> invoke(repositoryPath: String?, block: suspend CredentialsHandler.() -> R) =
        jgit.provideOptional(repositoryPath) { git ->
            var cache: CredentialsCache? = null
            val credentialsHandler = object : CredentialsHandler {
                override fun handleTransport(transport: Transport?) {
                    cache = when (transport) {
                        is SshTransport -> {
                            val sshCredentialsProvider = sshCredentialsProvider.get()
                            val sshSessionFactory = sessionManager.generateSshSessionFactory()
                            transport.sshSessionFactory = sshSessionFactory
                            transport.credentialsProvider = sshCredentialsProvider

                            sshCredentialsProvider
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

            val result = credentialsHandler.block()
            cache?.cacheCredentialsIfNeeded()

            result
        }
}