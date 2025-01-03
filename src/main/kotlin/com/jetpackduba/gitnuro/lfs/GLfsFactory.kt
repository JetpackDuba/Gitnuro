package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.credentials.CredentialsAccepted
import com.jetpackduba.gitnuro.credentials.CredentialsRequest
import com.jetpackduba.gitnuro.credentials.CredentialsStateManager
import com.jetpackduba.gitnuro.models.lfs.LfsObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsPrepareUploadObjectBatch
import com.jetpackduba.gitnuro.models.lfs.LfsRef
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.util.cio.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jgit.annotations.Nullable
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.attributes.Attribute
import org.eclipse.jgit.errors.IncorrectObjectTypeException
import org.eclipse.jgit.errors.MissingObjectException
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
import java.security.cert.X509Certificate
import java.util.*
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.X509TrustManager
import javax.swing.text.AbstractDocument.Content


@Singleton
class GLfsFactory @Inject constructor(
    private val lfsRepository: LfsRepository,
    private val credentialsStateManager: CredentialsStateManager,
) : LfsFactory() {
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
        db: Repository?,
        input: InputStream?,
        length: Long,
        attribute: Attribute?
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
            credentialsStateManager
        ) else null
    }

    @Nullable
    override fun getPrePushHook(
        repo: Repository,
        outputStream: PrintStream?,
        errorStream: PrintStream?
    ): PrePushHook? {
        return if (this.isEnabled(repo)) GLfsPrePushHook(
            repo,
            outputStream,
            errorStream,
            lfsRepository,
            credentialsStateManager
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

    fun getLfsRepositoryUrl(repository: Repository): String {
        // TODO Obtain proper url
        return "https://localhost:8080"
    }

    override fun call(): String = runBlocking {
        val toPush = findObjectsToPush()
        if (toPush.isEmpty()) {
            return@runBlocking ""
        }

        val uploadBatch = LfsPrepareUploadObjectBatch(
            operation = "upload",
            objects = toPush.map {
                LfsObjectBatch(it.oid.name(), it.size)
            },
            transfers = listOf(
                "lfs-standalone-file",
                "basic",
                "ssh",
            ),
            ref = LfsRef(repository.fullBranch),
            hashAlgo = "sha256",
        )

        if (!isDryRun) {
            val lfsServerUrl = getLfsRepositoryUrl(repository)
            val requiresAuth = lfsRepository.requiresAuthForBatchObjects(lfsServerUrl)

            var username: String? = null
            var password: String? = null

            if (requiresAuth) {
                credentialsStateManager.requestCredentials(CredentialsRequest.LfsCredentialsRequest)

                var credentials = credentialsStateManager.currentCredentialsState
                while (credentials is CredentialsRequest) {
                    credentials = credentialsStateManager.currentCredentialsState
                }

                if (credentials !is CredentialsAccepted.LfsCredentialsAccepted)
                    throw CancellationException("Credentials cancelled")
                else {
                    username = credentials.user
                    password = credentials.password
                }
            }

            lfsRepository.batchObjects(lfsServerUrl, uploadBatch, username, password)

            for (p in toPush) {
                val lfs = Lfs(repository)

                lfsRepository.uploadObject(
                    lfsServerUrl,
                    p.oid.name(),
                    lfs.getMediaFile(p.oid),
                    p.size,
                    username,
                    password,
                )
            }
        }

        return@runBlocking ""
    }
}
