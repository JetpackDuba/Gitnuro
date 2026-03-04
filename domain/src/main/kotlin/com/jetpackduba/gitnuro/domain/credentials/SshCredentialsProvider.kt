package com.jetpackduba.gitnuro.domain.credentials

import com.jetpackduba.gitnuro.domain.git.remote_operations.CredentialsCache
import com.jetpackduba.gitnuro.domain.models.CredentialsType
import com.jetpackduba.gitnuro.domain.repositories.CredentialsRepository
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.transport.CredentialItem
import org.eclipse.jgit.transport.CredentialItem.Password
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.URIish
import javax.inject.Inject

class SshCredentialsProvider @Inject constructor(
    private val credentialsStateManager: CredentialsStateManager,
    private val credentialsCacheRepository: CredentialsRepository,
    //private val appSettingsRepository: AppSettingsRepository,
) : CredentialsProvider(), CredentialsCache {
    private var credentialsCached: CredentialsType.SshCredentials? = null

    override fun isInteractive() = true

    override fun supports(vararg items: CredentialItem): Boolean {
        return items.size == 1 && items.first() is Password
    }

    override fun get(uri: URIish?, vararg items: CredentialItem): Boolean {
        // Should be safe to get without null check, as it's already done in [SshCredentialsProvider::supports]
        val passwordItem = items
            .filterIsInstance<Password>()
            .first()

        val cachedCredentials = credentialsCacheRepository.getCachedSshCredentials(uri.toString())
        val cacheCredentialsInMemory = true// TODO After refactor fix appSettingsRepository.cacheCredentialsInMemory

        if (cachedCredentials == null || !cacheCredentialsInMemory) {
            val sshCredentials = runBlocking {
                credentialsStateManager.requestSshCredentials()
            }

            passwordItem.value = sshCredentials.password.toCharArray()

            if (cacheCredentialsInMemory) {
                credentialsCached = CredentialsType.SshCredentials(
                    url = uri.toString(),
                    password = sshCredentials.password,
                )
            }
        } else {
            passwordItem.value = cachedCredentials.password.toCharArray()
        }

        return true
    }

    override suspend fun cacheCredentialsIfNeeded() {
        credentialsCached?.let { cached ->
            credentialsCacheRepository.cacheSshCredentials(cached.url, cached.password)
        }
    }
}