package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.common.Either
import com.jetpackduba.gitnuro.domain.errors.LfsError
import com.jetpackduba.gitnuro.domain.extensions.isHttpOrHttps
import com.jetpackduba.gitnuro.domain.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.domain.lfs.LfsObjects
import com.jetpackduba.gitnuro.domain.models.OperationType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.attributes.FilterCommand
import org.eclipse.jgit.lfs.Lfs
import org.eclipse.jgit.lfs.LfsPointer
import org.eclipse.jgit.lfs.errors.LfsException
import org.eclipse.jgit.lib.Repository
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
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
    private val getLfsUrlGitAction: GetLfsUrlGitAction,
    private val getLfsObjectsGitAction: com.jetpackduba.gitnuro.domain.git.lfs.GetLfsObjectsGitAction,
    private val authenticateLfsServerWithSshGitAction: com.jetpackduba.gitnuro.domain.git.lfs.AuthenticateLfsServerWithSshGitAction,
    private val downloadLfsObjectGitAction: com.jetpackduba.gitnuro.domain.git.lfs.DownloadLfsObjectGitAction,
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

        val lfsServerUrl = getLfsUrlGitAction(repository, null) ?: throw Exception("LFS Url not found")
        val isHttpUrl = lfsServerUrl.isHttpOrHttps()

        val lfsObjectBatches = listOf(LfsObjectBatch(lfsPointer.oid.name(), lfsPointer.size))

        val lfsObjects: Either<LfsObjects, LfsError>
        val finalServerUrl: String

        if (isHttpUrl) {
            finalServerUrl = lfsServerUrl
            lfsObjects = getLfsObjectsGitAction(
                lfsServerUrl,
                operationType = OperationType.DOWNLOAD,
                lfsObjectBatches = lfsObjectBatches,
                branch = repository.fullBranch,
                headers = emptyMap(),
            )
        } else {
            val lfsServerInfo = authenticateLfsServerWithSshGitAction(
                lfsServerUrl = lfsServerUrl,
                operationType = OperationType.DOWNLOAD
            )

            finalServerUrl = lfsServerInfo.href

            lfsObjects = getLfsObjectsGitAction(
                lfsServerInfo.href,
                operationType = OperationType.DOWNLOAD,
                lfsObjectBatches = lfsObjectBatches,
                branch = repository.fullBranch,
                headers = lfsServerInfo.header,
            )
        }

        when (lfsObjects) {
            is Either.Err -> throw LfsException("Gettings LFS objects failed with error: ${lfsObjects.error}")
            is Either.Ok -> {

                val lfsObject = lfsObjects.value.objects.firstOrNull() // There should be only one LFS object

                if (lfsObject != null) {
                    downloadLfsObjectGitAction(
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