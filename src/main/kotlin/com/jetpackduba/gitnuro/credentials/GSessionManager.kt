package com.jetpackduba.gitnuro.credentials

import com.jetpackduba.gitnuro.git.remote_operations.CredentialsCache
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.RemoteSession
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.URIish
import org.eclipse.jgit.util.FS
import javax.inject.Inject
import javax.inject.Provider

class GSessionManager @Inject constructor(
    private val GSshSessionFactory: GSshSessionFactory,
) {
    fun generateSshSessionFactory(): GSshSessionFactory {
        return GSshSessionFactory
    }
}

class GSshSessionFactory @Inject constructor(
    private val sessionProvider: Provider<SshRemoteSession>,
) : SshSessionFactory() {
    override fun getSession(
        uri: URIish,
        credentialsProvider: CredentialsProvider,
        fs: FS?,
        tms: Int,
    ): RemoteSession {
        val remoteSession = sessionProvider.get()
        remoteSession.setup(uri, credentialsProvider as SshCredentialsProvider)

        return remoteSession
    }

    override fun getType(): String {
        return "ssh" //TODO What should be the value of this?
    }
}