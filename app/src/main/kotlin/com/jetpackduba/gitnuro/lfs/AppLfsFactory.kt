package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.data.git.lfs.AuthenticateLfsServerWithSshGitAction
import com.jetpackduba.gitnuro.data.git.lfs.GetLfsObjectsGitAction
import com.jetpackduba.gitnuro.data.git.lfs.UploadLfsObjectGitAction
import com.jetpackduba.gitnuro.data.git.lfs.VerifyUploadLfsObjectGitAction
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.LfsError
import com.jetpackduba.gitnuro.domain.extensions.isHttpOrHttps
import com.jetpackduba.gitnuro.domain.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.domain.lfs.LfsObjects
import com.jetpackduba.gitnuro.domain.models.OperationType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import org.eclipse.jgit.annotations.Nullable
import org.eclipse.jgit.attributes.Attribute
import org.eclipse.jgit.attributes.FilterCommandRegistry
import org.eclipse.jgit.hooks.PrePushHook
import org.eclipse.jgit.lfs.InstallBuiltinLfsCommand
import org.eclipse.jgit.lfs.LfsBlobFilter
import org.eclipse.jgit.lfs.LfsPointer
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
    private val getLfsUrlGitAction: GetLfsUrlGitAction,
    private val getLfsObjectsGitAction: GetLfsObjectsGitAction,
    private val uploadLfsObjectGitAction: UploadLfsObjectGitAction,
    private val verifyUploadLfsObjectGitAction: VerifyUploadLfsObjectGitAction,
    private val authenticateLfsServerWithSshGitAction: AuthenticateLfsServerWithSshGitAction,
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
        var obj: RevObject?
        val r = walk.objectReader

        obj = walk.nextObject()

        while (obj != null) {
            if (obj.type == Constants.OBJ_BLOB && getObjectSize(r, obj) < LfsPointer.SIZE_THRESHOLD) {
                val ptr = loadLfsPointer(r, obj)
                if (ptr != null) {
                    toPush.add(ptr)
                }
            }

            obj = walk.nextObject()
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
            val lfsServerUrl = getLfsUrlGitAction(repository, remote()) ?: throw Exception("LFS Url not found")
            val isHttpUrl = lfsServerUrl.isHttpOrHttps()

            val lfsObjectBatches = toPush.map { LfsObjectBatch(it.oid.name(), it.size) }

            val lfsObjects: Either<LfsObjects, LfsError>
            val finalServerUrl: String

            if (isHttpUrl) {
                finalServerUrl = lfsServerUrl
                lfsObjects = getLfsObjectsGitAction(
                    lfsServerUrl,
                    operationType = OperationType.UPLOAD,
                    lfsObjectBatches = lfsObjectBatches,
                    branch = repository.fullBranch,
                    headers = emptyMap(),
                )
            } else {
                val lfsServerInfo =
                    authenticateLfsServerWithSshGitAction(lfsServerUrl, operationType = OperationType.UPLOAD)
                finalServerUrl = lfsServerInfo.href

                lfsObjects = getLfsObjectsGitAction(
                    lfsServerInfo.href,
                    operationType = OperationType.UPLOAD,
                    lfsObjectBatches = lfsObjectBatches,
                    branch = repository.fullBranch,
                    headers = lfsServerInfo.header,
                )
            }

            when (lfsObjects) {
                is Either.Err -> throw LfsException("Gettings LFS objects failed with error: ${lfsObjects.error}")
                is Either.Ok -> {
                    for (p in toPush) {
                        val lfsObject = lfsObjects.value.objects.firstOrNull { it.oid == p.oid.name() }

                        if (lfsObject != null) {
                            val uploadEither = uploadLfsObjectGitAction(
                                lfsServerUrl = finalServerUrl,
                                lfsObject = lfsObject,
                                repository = repository,
                                oid = p.oid,
                            )

                            if (uploadEither is Either.Err) {
                                throw LfsException("Objects upload failed: ${uploadEither.error}")
                            }

                            val verifyEither = verifyUploadLfsObjectGitAction(
                                lfsServerUrl = finalServerUrl,
                                lfsObject = lfsObject,
                                oid = p.oid,
                            )

                            if (verifyEither is Either.Err) {
                                throw LfsException("Objects verification failed: ${verifyEither.error}")
                            }
                        }
                    }
                }
            }
        }

        return@runBlocking ""
    }
}
