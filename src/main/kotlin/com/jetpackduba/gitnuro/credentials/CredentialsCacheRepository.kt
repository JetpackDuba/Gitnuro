package com.jetpackduba.gitnuro.credentials

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.eclipse.jgit.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

private const val KEY_LENGTH = 16

@Singleton
class CredentialsCacheRepository @Inject constructor() {
    private val credentialsCached = mutableListOf<CredentialsCacheType>()
    private val credentialsLock = Mutex(false)

    // having a random key to encrypt the password may help in case of a memory dump attack
    private val encryptionKey = getRandomKey()

    fun getCachedHttpCredentials(url: String, isLfs: Boolean): CredentialsCacheType.HttpCredentialsCache? {
        val credentials = credentialsCached.filterIsInstance<CredentialsCacheType.HttpCredentialsCache>().firstOrNull {
            it.url == url && it.isLfs == isLfs
        }

        return credentials?.copy(password = credentials.password.cipherDecrypt())
    }

    fun getCachedSshCredentials(url: String): CredentialsCacheType.SshCredentialsCache? {
        val credentials = credentialsCached.filterIsInstance<CredentialsCacheType.SshCredentialsCache>().firstOrNull {
            it.url == url
        }

        return credentials?.copy(password = credentials.password.cipherDecrypt())
    }

    suspend fun cacheHttpCredentials(credentials: CredentialsCacheType.HttpCredentialsCache) {
        cacheHttpCredentials(credentials.url, credentials.user, credentials.password, credentials.isLfs)
    }

    suspend fun cacheHttpCredentials(url: String, userName: String, password: String, isLfs: Boolean) {
        val passwordEncrypted = password.cipherEncrypt()

        credentialsLock.withLock {
            val previouslyCached = credentialsCached.any {
                it is CredentialsCacheType.HttpCredentialsCache && it.url == url
            }

            if (!previouslyCached) {
                val credentials = CredentialsCacheType.HttpCredentialsCache(url, userName, passwordEncrypted, isLfs)
                credentialsCached.add(credentials)
            }
        }
    }

    suspend fun cacheSshCredentials(url: String, password: String) {
        val passwordEncrypted = password.cipherEncrypt()

        credentialsLock.withLock {
            val previouslyCached = credentialsCached.any {
                it is CredentialsCacheType.SshCredentialsCache && it.url == url
            }

            if (!previouslyCached) {
                val credentials = CredentialsCacheType.SshCredentialsCache(url, passwordEncrypted)
                credentialsCached.add(credentials)
            }
        }
    }

    private fun String.cipherEncrypt(): String {
        val secretKeySpec = SecretKeySpec(encryptionKey, "AES")
        val ivParameterSpec = IvParameterSpec(encryptionKey)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, ivParameterSpec)

        val encryptedValue = cipher.doFinal(this.toByteArray())
        return Base64.encodeBytes(encryptedValue)
    }

    private fun String.cipherDecrypt(): String {
        val secretKeySpec = SecretKeySpec(encryptionKey, "AES")
        val ivParameterSpec = IvParameterSpec(encryptionKey)

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, ivParameterSpec)

        val decodedValue = Base64.decode(this)
        val decryptedValue = cipher.doFinal(decodedValue)
        return String(decryptedValue)
    }

    private fun getRandomKey(): ByteArray {
        val byteArray = ByteArray(KEY_LENGTH)
        Random.Default.nextBytes(byteArray)

        return byteArray
    }
}

sealed interface CredentialsCacheType {
    data class SshCredentialsCache(
        val url: String,
        val password: String,
    ) : CredentialsCacheType

    data class HttpCredentialsCache(
        val url: String,
        val user: String,
        val password: String,
        val isLfs: Boolean,
    ) : CredentialsCacheType
}
