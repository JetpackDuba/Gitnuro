package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.Result
import com.jetpackduba.gitnuro.credentials.CredentialsAccepted
import com.jetpackduba.gitnuro.credentials.CredentialsCacheRepository
import com.jetpackduba.gitnuro.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.extensions.isHttpOrHttps
import com.jetpackduba.gitnuro.git.lfs.AuthenticateLfsServerWithSshUseCase
import com.jetpackduba.gitnuro.git.lfs.DownloadLfsObjectUseCase
import com.jetpackduba.gitnuro.git.lfs.GetLfsObjectsUseCase
import com.jetpackduba.gitnuro.logging.printLog
import com.jetpackduba.gitnuro.models.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsObjects
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.attributes.FilterCommand
import org.eclipse.jgit.lfs.Lfs
import org.eclipse.jgit.lfs.LfsPointer
import org.eclipse.jgit.lfs.errors.LfsException
import org.eclipse.jgit.lib.Repository
import java.io.*
import java.nio.file.Files

private const val MAX_COPY_BYTES = 1024 * 1024 * 256

@AssistedFactory
interface LfsSmudgeFilterFactory {
    fun create(repository: Repository, input: InputStream, output: OutputStream): LfsSmudgeFilter
}

class LfsSmudgeFilter @AssistedInject constructor(
    @Assisted repository: Repository,
    @Assisted input: InputStream,
    @Assisted output: OutputStream,
    private val getLfsUrlUseCase: GetLfsUrlUseCase,
    private val getLfsObjectsUseCase: GetLfsObjectsUseCase,
    private val authenticateLfsServerWithSshUseCase: AuthenticateLfsServerWithSshUseCase,
    private val downloadLfsObjectUseCase: DownloadLfsObjectUseCase,
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
                    downloadLfsResource(repository, res)
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

    private fun downloadLfsResource(
        repository: Repository,
        lfsPointer: LfsPointer,
    ) = runBlocking {

        val lfsServerUrl = getLfsUrlUseCase(repository, null) ?: throw Exception("LFS Url not found")
        val isHttpUrl = lfsServerUrl.isHttpOrHttps()

        val lfsObjectBatches = listOf(LfsObjectBatch(lfsPointer.oid.name(), lfsPointer.size))

        val lfsObjects: Result<LfsObjects, LfsError>
        val finalServerUrl: String

        if (isHttpUrl) {
            finalServerUrl = lfsServerUrl
            lfsObjects = getLfsObjectsUseCase(
                lfsServerUrl,
                operationType = OperationType.DOWNLOAD,
                lfsObjectBatches = lfsObjectBatches,
                branch = repository.fullBranch,
                headers = emptyMap(),
            )
        } else {
            val lfsServerInfo = authenticateLfsServerWithSshUseCase(
                lfsServerUrl = lfsServerUrl,
                operationType = OperationType.DOWNLOAD
            )

            finalServerUrl = lfsServerInfo.href

            lfsObjects = getLfsObjectsUseCase(
                lfsServerInfo.href,
                operationType = OperationType.DOWNLOAD,
                lfsObjectBatches = lfsObjectBatches,
                branch = repository.fullBranch,
                headers = lfsServerInfo.header,
            )
        }

        when (lfsObjects) {
            is Result.Err -> throw LfsException("Gettings LFS objects failed with error: {lfsObjects.error}")
            is Result.Ok -> {

                val lfsObject = lfsObjects.value.objects.firstOrNull() // There should be only one LFS object

                if (lfsObject != null) {
                    downloadLfsObjectUseCase(
                        repository = repository,
                        lfsServerUrl = finalServerUrl,
                        lfsObject = lfsObject,
                        lfsPointer.oid,
                    )
                }
            }
        }
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