package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.Result
import com.jetpackduba.gitnuro.credentials.CredentialsAccepted
import com.jetpackduba.gitnuro.credentials.CredentialsCacheRepository
import com.jetpackduba.gitnuro.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.logging.printLog
import com.jetpackduba.gitnuro.models.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsObjects
import com.jetpackduba.gitnuro.models.lfs.LfsPrepareUploadObjectBatch
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

private const val TAG = "GLfsFactory"

@Singleton
class GLfsFactory @Inject constructor(
    private val lfsRepository: LfsRepository,
    private val credentialsStateManager: CredentialsStateManager,
    private val credentialsCacheRepository: CredentialsCacheRepository,
) : LfsFactory() {
    init {
        FilterCommandRegistry.register("jgit://builtin/lfs/smudge") { repository, input, out ->
            LfsSmudgeFilter(
                lfsRepository = lfsRepository,
                credentialsStateManager = credentialsStateManager,
                credentialsCacheRepository = credentialsCacheRepository,
                input = input,
                output = out,
                repository = repository
            )
        }
        FilterCommandRegistry.register("jgit://builtin/lfs/clean") { db, `in`, out ->
            LfsCleanFilter(db, `in`, out)
        }
    }


    fun register() {
        setInstance(this)
    }


    override fun isAvailable(): Boolean {
        return true
    }

    @Throws(IOException::class)
    override fun applySmudgeFilter(db: Repository?, loader: ObjectLoader, attribute: Attribute?): ObjectLoader {
        return if (!this.isEnabled(db) || attribute != null && !this.isEnabled(
                db,
                attribute
            )
        ) loader else LfsBlobFilter.smudgeLfsBlob(db, loader)
    }

    @Throws(IOException::class)
    override fun applyCleanFilter(
        db: Repository,
        input: InputStream?,
        length: Long,
        attribute: Attribute?,
    ): LfsInputStream {
        return if (this.isEnabled(db, attribute)) LfsInputStream(
            LfsBlobFilter.cleanLfsBlob(
                db,
                input
            )
        ) else LfsInputStream(input, length)
    }

    @Nullable
    override fun getPrePushHook(repo: Repository, outputStream: PrintStream?): PrePushHook? {
        return if (this.isEnabled(repo)) GLfsPrePushHook(
            repo,
            outputStream,
            null,
            lfsRepository,
            credentialsStateManager,
            credentialsCacheRepository,
        ) else null
    }

    @Nullable
    override fun getPrePushHook(
        repo: Repository,
        outputStream: PrintStream?,
        errorStream: PrintStream?,
    ): PrePushHook? {
        return if (this.isEnabled(repo)) GLfsPrePushHook(
            repo,
            outputStream,
            errorStream,
            lfsRepository,
            credentialsStateManager,
            credentialsCacheRepository,
        ) else null
    }

    override fun isEnabled(db: Repository?): Boolean {
        return true
    }

    private fun isEnabled(db: Repository?, attribute: Attribute?): Boolean {
        return if (attribute == null) {
            false
        } else {
            isEnabled(db) && "lfs" == attribute.value
        }
    }

    override fun getInstallCommand(): LfsInstallCommand {
        return InstallBuiltinLfsCommand()
    }
}

class GLfsPrePushHook(
    repository: Repository,
    outputStream: PrintStream?,
    errorStream: PrintStream?,
    private val lfsRepository: LfsRepository,
    private val credentialsStateManager: CredentialsStateManager,
    private val credentialsCacheRepository: CredentialsCacheRepository,
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
            val lfsServerUrl = lfsRepository.getLfsRepositoryUrl(repository)

            val lfsPrepareUploadObjectBatch = createLfsPrepareUploadObjectBatch(
                OperationType.UPLOAD,
                branch = repository.fullBranch,
                objects = toPush.map { LfsObjectBatch(it.oid.name(), it.size) },
            )

            var credentials: CredentialsAccepted.LfsCredentialsAccepted? = null
            var credentialsAlreadyRequested = false

            val cachedCredentials = run {
                val cacheHttpCredentials = credentialsCacheRepository.getCachedHttpCredentials(lfsServerUrl, isLfs = true)

                if (cacheHttpCredentials != null) {
                    CredentialsAccepted.LfsCredentialsAccepted.fromCachedCredentials(cacheHttpCredentials)
                } else {
                    null
                }
            }

            val lfsObjects = getLfsObjects(lfsServerUrl, lfsPrepareUploadObjectBatch) {
                if (!credentialsAlreadyRequested && cachedCredentials != null) {
                    credentialsAlreadyRequested = true

                    credentials = cachedCredentials

                    cachedCredentials
                } else {
                    val newCredentials = credentialsStateManager.requestLfsCredentials()

                    credentials = newCredentials

                    newCredentials
                }
            }

            when (lfsObjects) {
                is Result.Err -> {
                    throw Exception("LFS Error ${lfsObjects.error}")
                }

                is Result.Ok -> for (p in toPush) {
                    val safeCredentials = credentials
                    if (cachedCredentials != safeCredentials && safeCredentials != null) {
                        credentialsCacheRepository.cacheHttpCredentials(
                            lfsServerUrl,
                            safeCredentials.user,
                            safeCredentials.password,
                            isLfs = true,
                        )
                    }
                    val lfs = Lfs(repository)

                    printLog("LFS", "Requesting credentials for objects upload")
                    // TODO Items upload could have their own credentials but it's not common
//                    val credentials = credentialsStateManager.requestLfsCredentials()

                    lfsObjects.value.objects.forEach { obj ->
                        val uploadUrl = obj.actions.upload?.href

                        if (uploadUrl != null) {
                            lfsRepository.uploadObject(
                                uploadUrl,
                                p.oid.name(),
                                lfs.getMediaFile(p.oid),
                                p.size,
                                credentials?.user,
                                credentials?.password,
                            )
                        }
                    }
                }
            }
        }

        return@runBlocking ""
    }

    private suspend fun getLfsObjects(
        lfsServerUrl: String,
        lfsPrepareUploadObjectBatch: LfsPrepareUploadObjectBatch,
        onRequestCredentials: suspend () -> CredentialsAccepted.LfsCredentialsAccepted,
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
                val credentials = onRequestCredentials()
                username = credentials.user
                password = credentials.password
            }

            lfsObjects = newLfsObjects
        } while (requiresAuth)

        return lfsObjects
    }
}
