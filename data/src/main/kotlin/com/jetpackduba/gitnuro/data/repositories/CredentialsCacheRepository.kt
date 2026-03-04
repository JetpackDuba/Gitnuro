package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.domain.models.CredentialsType
import com.jetpackduba.gitnuro.domain.repositories.CredentialsRepository
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
class CredentialsCacheRepository @Inject constructor() : CredentialsRepository {
    private val credentialsCached = mutableListOf<CredentialsType>()
    private val credentialsLock = Mutex(false)

    // having a random key to encrypt the password may help in case of a memory dump attack
    private val encryptionKey = getRandomKey()

    override fun getCachedHttpCredentials(url: String, isLfs: Boolean): CredentialsType.HttpCredentials? {
        val credentials = credentialsCached.filterIsInstance<CredentialsType.HttpCredentials>().firstOrNull {
            it.url == url && it.isLfs == isLfs
        }

        return credentials?.copy(password = credentials.password.cipherDecrypt())
    }

    override fun getCachedSshCredentials(url: String): CredentialsType.SshCredentials? {
        val credentials = credentialsCached.filterIsInstance<CredentialsType.SshCredentials>().firstOrNull {
            it.url == url
        }

        return credentials?.copy(password = credentials.password.cipherDecrypt())
    }

    override suspend fun cacheHttpCredentials(credentials: CredentialsType.HttpCredentials) {
        cacheHttpCredentials(credentials.url, credentials.user, credentials.password, credentials.isLfs)
    }

    override suspend fun cacheHttpCredentials(url: String, userName: String, password: String, isLfs: Boolean) {
        val passwordEncrypted = password.cipherEncrypt()

        credentialsLock.withLock {
            val previouslyCached = credentialsCached.any {
                it is CredentialsType.HttpCredentials && it.url == url
            }

            if (!previouslyCached) {
                val credentials = CredentialsType.HttpCredentials(url, userName, passwordEncrypted, isLfs)
                credentialsCached.add(credentials)
            }
        }
    }

    override suspend fun cacheSshCredentials(url: String, password: String) {
        val passwordEncrypted = password.cipherEncrypt()

        credentialsLock.withLock {
            val previouslyCached = credentialsCached.any {
                it is CredentialsType.SshCredentials && it.url == url
            }

            if (!previouslyCached) {
                val credentials = CredentialsType.SshCredentials(url, passwordEncrypted)
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

