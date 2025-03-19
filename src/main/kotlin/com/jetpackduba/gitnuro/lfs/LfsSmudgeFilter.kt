package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.Result
import com.jetpackduba.gitnuro.credentials.CredentialsAccepted
import com.jetpackduba.gitnuro.credentials.CredentialsCacheRepository
import com.jetpackduba.gitnuro.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.logging.printLog
import com.jetpackduba.gitnuro.models.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsObjects
import com.jetpackduba.gitnuro.models.lfs.LfsPrepareUploadObjectBatch
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.attributes.FilterCommand
import org.eclipse.jgit.lfs.Lfs
import org.eclipse.jgit.lfs.LfsPointer
import org.eclipse.jgit.lfs.lib.AnyLongObjectId
import org.eclipse.jgit.lib.Repository
import java.io.*
import java.nio.file.Files

private const val MAX_COPY_BYTES = 1024 * 1024 * 256

class LfsSmudgeFilter(
    private val lfsRepository: LfsRepository,
    private val credentialsStateManager: CredentialsStateManager,
    private val credentialsCacheRepository: CredentialsCacheRepository,
    input: InputStream,
    output: OutputStream,
    repository: Repository,
) : FilterCommand(
    if (input.markSupported()) input else BufferedInputStream(input),
    output,
) {
    init {
        var from: InputStream? = input
        try {
            val res = LfsPointer.parseLfsPointer(from)
            if (res != null) {
                val oid = res.oid
                val lfs = Lfs(repository)
                val mediaFile = lfs.getMediaFile(oid)
                if (!Files.exists(mediaFile)) {
                    downloadLfsResource2(repository, res)
                }
                this.`in` = Files.newInputStream(mediaFile)
            } else {
                // Not swapped; stream was reset, don't close!
                from = null
            }
        } finally {
            from?.close()
        }
    }

    private fun downloadLfsResource2(
        repository: Repository,
        res: LfsPointer,
    ) = runBlocking {
        val lfsServerUrl = lfsRepository.getLfsRepositoryUrl(repository, null) ?: throw Exception("LFS Url not found")

        val hash = res.oid.name()
        val size = res.size

        val lfsPrepareUploadObjectBatch = createLfsPrepareUploadObjectBatch(
            operation = OperationType.DOWNLOAD,
            branch = repository.fullBranch,
            objects = listOf(LfsObjectBatch(hash, size)),
        )

        var credentials: CredentialsAccepted.LfsCredentialsAccepted? = null
        var cachedCredentialsAlreadyRequested = false

        val lfsObjects = getLfsObjects(lfsServerUrl, lfsPrepareUploadObjectBatch) {
            if (!cachedCredentialsAlreadyRequested) {
                val cachedCredentials = credentialsCacheRepository.getCachedHttpCredentials(lfsServerUrl, true)

                if (cachedCredentials != null) {
                    val newCredentials = CredentialsAccepted.LfsCredentialsAccepted(
                        user = cachedCredentials.userName,
                        password = cachedCredentials.password,
                    )

                    credentials = newCredentials

                    cachedCredentialsAlreadyRequested = true

                    return@getLfsObjects newCredentials
                }
            }

            val newCredentials = requestLfsCredentials()
            credentials = newCredentials

            return@getLfsObjects newCredentials
        }

        when (lfsObjects) {
            is Result.Err -> throw Exception("LFS Error ${lfsObjects.error}")
            is Result.Ok -> {
                credentials?.let { safeCredentials ->
                    credentialsCacheRepository.cacheHttpCredentials(
                        lfsServerUrl,
                        safeCredentials.user,
                        safeCredentials.password,
                        isLfs = true,
                    )
                }

                val lfs = Lfs(repository)

                printLog("LFS", "Requesting credentials for objects upload")

                downloadLfsObject(lfs, lfsObjects.value, res.oid) {
                    val safeCredentials = credentials
                    val newCredentials = if (safeCredentials == null) {
                        val newCredentials = requestLfsCredentials()
                        credentials = newCredentials

                        newCredentials
                    } else {
                        safeCredentials
                    }

                    newCredentials
                }
            }
        }
    }

    private suspend fun downloadLfsObject(
        lfs: Lfs,
        lfsObjects: LfsObjects,
        oid: AnyLongObjectId,
        requestCredentials: () -> CredentialsAccepted.LfsCredentialsAccepted,
    ) {
        var username: String? = null
        var password: String? = null

        for (lfsObject in lfsObjects.objects) {
            var requiresAuth: Boolean

            val downloadUrl = lfsObject.actions.download?.href ?: continue

            do {
                val newLfsObjects = lfsRepository.downloadObject(
                    lfsObject,
                    downloadUrl,
                    lfs.getMediaFile(oid),
                    username,
                    password,
                )

                requiresAuth = newLfsObjects is Result.Err &&
                        newLfsObjects.error is LfsError.HttpError &&
                        newLfsObjects.error.code == HttpStatusCode.Unauthorized

                if (requiresAuth) {
                    val credentials = requestCredentials()
                    username = credentials.user
                    password = credentials.password
                }
            } while (requiresAuth)
        }
    }

    private suspend fun getLfsObjects(
        lfsServerUrl: String,
        lfsPrepareUploadObjectBatch: LfsPrepareUploadObjectBatch,
        requestCredentials: () -> CredentialsAccepted.LfsCredentialsAccepted,
    ): Result<LfsObjects, LfsError> {

        var lfsObjects: Result<LfsObjects, LfsError>
        var requiresAuth: Boolean

        var username: String? = null
        var password: String? = null

        do {
            val newLfsObjects = lfsRepository.postBatchObjects(
                lfsServerUrl,
                lfsPrepareUploadObjectBatch,
                username,
                password,
            )

            requiresAuth = newLfsObjects is Result.Err &&
                    newLfsObjects.error is LfsError.HttpError &&
                    newLfsObjects.error.code == HttpStatusCode.Unauthorized

            if (requiresAuth) {
                val credentials = requestCredentials()
                username = credentials.user
                password = credentials.password
            }

            lfsObjects = newLfsObjects
        } while (requiresAuth)

        return lfsObjects
    }

    private fun requestLfsCredentials(): CredentialsAccepted.LfsCredentialsAccepted = runBlocking {
        credentialsStateManager.requestLfsCredentials()
    }

    @Throws(IOException::class)
    override fun run(): Int {
        try {
            var totalRead = 0
            var length = 0
            if (`in` != null) {
                val buf = ByteArray(8192)
                while ((`in`.read(buf).also { length = it }) != -1) {
                    out.write(buf, 0, length)
                    totalRead += length

                    // when threshold reached, loop back to the caller.
                    // otherwise we could only support files up to 2GB (int
                    // return type) properly. we will be called again as long as
                    // we don't return -1 here.
                    if (totalRead >= MAX_COPY_BYTES) {
                        // leave streams open - we need them in the next call.
                        return totalRead
                    }
                }
            }

            if (totalRead == 0 && length == -1) {
                // we're totally done :) cleanup all streams
                `in`.close()
                out.close()
                return length
            }

            return totalRead
        } catch (e: IOException) {
            `in`.close() // clean up - we swapped this stream.
            out.close()
            throw e
        }
    }
}