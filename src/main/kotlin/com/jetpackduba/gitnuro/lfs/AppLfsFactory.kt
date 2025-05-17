package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.Result
import com.jetpackduba.gitnuro.credentials.*
import com.jetpackduba.gitnuro.extensions.isHttpOrHttps
import com.jetpackduba.gitnuro.git.lfs.AuthenticateLfsServerWithSshUseCase
import com.jetpackduba.gitnuro.git.lfs.GetLfsObjectsUseCase
import com.jetpackduba.gitnuro.git.lfs.UploadLfsObjectUseCase
import com.jetpackduba.gitnuro.git.lfs.VerifyUploadLfsObjectUseCase
import com.jetpackduba.gitnuro.models.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsObjects
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.cio.*
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.annotations.Nullable
import org.eclipse.jgit.attributes.Attribute
import org.eclipse.jgit.attributes.FilterCommandRegistry
import org.eclipse.jgit.hooks.PrePushHook
import org.eclipse.jgit.lfs.*
import org.eclipse.jgit.lfs.errors.LfsException
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.ObjectWalk
import org.eclipse.jgit.revwalk.RevObject
import org.eclipse.jgit.transport.RemoteRefUpdate
import org.eclipse.jgit.util.LfsFactory
import java.io.IOException
import java.io.InputStream
import java.io.PrintStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "AppLfsFactory"

@Singleton
class AppLfsFactory @Inject constructor(
    private val lfsPrePushHookFactory: LfsPrePushHookFactory,
    private val lfsSmudgeFilterFactory: LfsSmudgeFilterFactory,
    private val lfsCleanFilterFactory: LfsCleanFilterFactory,
) : LfsFactory() {
    init {
        FilterCommandRegistry.register("jgit://builtin/lfs/smudge") { repository, input, output ->
            lfsSmudgeFilterFactory.create(
                repository = repository,
                input = input,
                output = output
            )
        }
        FilterCommandRegistry.register("jgit://builtin/lfs/clean") { repository, input, output ->
            lfsCleanFilterFactory.create(repository, input, output)
        }
    }

    override fun isAvailable(): Boolean {
        return true
    }

    @Throws(IOException::class)
    override fun applySmudgeFilter(
        repository: Repository,
        loader: ObjectLoader,
        attribute: Attribute?,
    ): ObjectLoader {
        return if (
            !this.isEnabled(repository) ||
            attribute != null && !this.isEnabled(repository, attribute)
        ) {
            loader
        } else {
            LfsBlobFilter.smudgeLfsBlob(repository, loader)
        }
    }

    @Throws(IOException::class)
    override fun applyCleanFilter(
        repository: Repository,
        input: InputStream,
        length: Long,
        attribute: Attribute?,
    ): LfsInputStream {
        return if (this.isEnabled(repository, attribute)) {
            LfsInputStream(
                LfsBlobFilter.cleanLfsBlob(
                    repository,
                    input
                )
            )
        } else {
            LfsInputStream(input, length)
        }
    }

    @Nullable
    override fun getPrePushHook(repo: Repository, outputStream: PrintStream?): PrePushHook? {
        return getPrePushHook(
            repository = repo,
            outputStream = outputStream,
            errorStream = null,
        )
    }

    @Nullable
    override fun getPrePushHook(
        repository: Repository,
        outputStream: PrintStream?,
        errorStream: PrintStream?,
    ): PrePushHook? {
        return if (this.isEnabled(repository)) {
            lfsPrePushHookFactory.create(
                repository,
                outputStream,
                errorStream,
            )
        } else {
            null
        }
    }

    override fun isEnabled(repository: Repository): Boolean {
        return true
    }

    // TODO Do we need to check for attributes? Probably not
    private fun isEnabled(repository: Repository, attribute: Attribute?): Boolean {
        return if (attribute == null) {
            false
        } else {
            isEnabled(repository) && "lfs" == attribute.value
        }
    }

    override fun getInstallCommand(): LfsInstallCommand {
        return InstallBuiltinLfsCommand()
    }
}

@AssistedFactory
interface LfsPrePushHookFactory {
    fun create(
        repository: Repository,
        @Assisted("outputStream") outputStream: PrintStream?,
        @Assisted("errorStream") errorStream: PrintStream?,
    ): LfsPrePushHook
}

class LfsPrePushHook @AssistedInject constructor(
    @Assisted repository: Repository,
    @Assisted("outputStream") outputStream: PrintStream?,
    @Assisted("errorStream") errorStream: PrintStream?,
    private val getLfsUrlUseCase: GetLfsUrlUseCase,
    private val getLfsObjectsUseCase: GetLfsObjectsUseCase,
    private val uploadLfsObjectUseCase: UploadLfsObjectUseCase,
    private val verifyUploadLfsObjectUseCase: VerifyUploadLfsObjectUseCase,
    private val authenticateLfsServerWithSshUseCase: AuthenticateLfsServerWithSshUseCase,
) : PrePushHook(repository, outputStream, errorStream) {
    private var refs: Collection<RemoteRefUpdate> = emptyList()

    override fun setRefs(toRefs: Collection<RemoteRefUpdate>) {
        this.refs = toRefs
    }

    private fun findObjectsToPush(): Set<LfsPointer> {
        val toPush: MutableSet<LfsPointer> = TreeSet()

        ObjectWalk(repository).use { walk ->
            for (up in refs) {
                if (up.isDelete) {
                    continue
                }
                walk.setRewriteParents(false)
                excludeRemoteRefs(walk)
                walk.markStart(walk.parseCommit(up.newObjectId))
                while (walk.next() != null) {
                    // walk all commits to populate objects
                }
                findLfsPointers(toPush, walk)
            }
        }
        return toPush
    }

    private fun findLfsPointers(toPush: MutableSet<LfsPointer>, walk: ObjectWalk) {
        var obj: RevObject
        val r = walk.objectReader
        while ((walk.nextObject().also { obj = it }) != null) {
            if (obj.type == Constants.OBJ_BLOB
                && getObjectSize(r, obj) < LfsPointer.SIZE_THRESHOLD
            ) {
                val ptr = loadLfsPointer(r, obj)
                if (ptr != null) {
                    toPush.add(ptr)
                }
            }
        }
    }

    private fun getObjectSize(r: ObjectReader, obj: RevObject): Long {
        return r.getObjectSize(obj.id, Constants.OBJ_BLOB)
    }

    private fun loadLfsPointer(r: ObjectReader, obj: AnyObjectId): LfsPointer? {
        r.open(obj, Constants.OBJ_BLOB).openStream().use { `is` ->
            return LfsPointer.parseLfsPointer(`is`)
        }
    }

    private fun excludeRemoteRefs(walk: ObjectWalk) {
        val refDatabase = repository.refDatabase
        val remoteRefs = refDatabase.getRefsByPrefix(remote())
        for (r in remoteRefs) {
            var oid = r.peeledObjectId

            if (oid == null) {
                oid = r.objectId
            }

            if (oid == null) {
                // ignore (e.g. symbolic, ...)
                continue
            }

            val o = walk.parseAny(oid)
            if (o.type == Constants.OBJ_COMMIT
                || o.type == Constants.OBJ_TAG
            ) {
                walk.markUninteresting(o)
            }
        }
    }

    private fun remote(): String {
        val remoteName = if (remoteName == null)
            Constants.DEFAULT_REMOTE_NAME
        else
            remoteName

        return Constants.R_REMOTES + remoteName
    }

    override fun call(): String = runBlocking {
        val toPush = findObjectsToPush()
        if (toPush.isEmpty()) {
            return@runBlocking ""
        }

        if (!isDryRun) {
            val lfsServerUrl = getLfsUrlUseCase(repository, remote()) ?: throw Exception("LFS Url not found")
            val isHttpUrl = lfsServerUrl.isHttpOrHttps()

            val lfsObjectBatches = toPush.map { LfsObjectBatch(it.oid.name(), it.size) }

            val lfsObjects: Result<LfsObjects, LfsError>
            val finalServerUrl: String

            if (isHttpUrl) {
                finalServerUrl = lfsServerUrl
                lfsObjects = getLfsObjectsUseCase(
                    lfsServerUrl,
                    operationType = OperationType.UPLOAD,
                    lfsObjectBatches = lfsObjectBatches,
                    branch = repository.fullBranch,
                    headers = emptyMap(),
                )
            } else {
                val lfsServerInfo =
                    authenticateLfsServerWithSshUseCase(lfsServerUrl, operationType = OperationType.UPLOAD)
                finalServerUrl = lfsServerInfo.href

                lfsObjects = getLfsObjectsUseCase(
                    lfsServerInfo.href,
                    operationType = OperationType.UPLOAD,
                    lfsObjectBatches = lfsObjectBatches,
                    branch = repository.fullBranch,
                    headers = lfsServerInfo.header,
                )
            }

            when (lfsObjects) {
                is Result.Err -> throw LfsException("Gettings LFS objects failed with error: ${lfsObjects.error}")
                is Result.Ok -> {
                    for (p in toPush) {
                        val lfsObject = lfsObjects.value.objects.firstOrNull { it.oid == p.oid.name() }

                        if (lfsObject != null) {
                            val uploadResult = uploadLfsObjectUseCase(
                                lfsServerUrl = finalServerUrl,
                                lfsObject = lfsObject,
                                repository = repository,
                                oid = p.oid,
                            )

                            if (uploadResult is Result.Err) {
                                throw LfsException("Objects upload failed: ${uploadResult.error}")
                            }

                            val verifyResult = verifyUploadLfsObjectUseCase(
                                lfsServerUrl = finalServerUrl,
                                lfsObject = lfsObject,
                                oid = p.oid,
                            )

                            if (verifyResult is Result.Err) {
                                throw LfsException("Objects verification failed: ${verifyResult.error}")
                            }
                        }
                    }
                }
            }
        }

        return@runBlocking ""
    }
}
