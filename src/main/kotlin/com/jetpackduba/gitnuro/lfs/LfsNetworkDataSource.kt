package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.Result
import com.jetpackduba.gitnuro.models.lfs.LfsObjects
import com.jetpackduba.gitnuro.models.lfs.LfsPrepareUploadObjectBatch
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import javax.inject.Inject

interface ILfsNetworkDataSource {
    suspend fun postBatchObjects(
        remoteUrl: String,
        lfsPrepareUploadObjectBatch: LfsPrepareUploadObjectBatch,
        username: String?,
        password: String?,
    ): Result<LfsObjects, LfsError>

    suspend fun uploadBatchObjects(
        remoteUrl: String,
        lfsPrepareUploadObjectBatch: LfsPrepareUploadObjectBatch,
        username: String?,
        password: String?,
    )

    suspend fun uploadObject(
        uploadUrl: String,
        oid: String,
        file: Path,
        size: Long,
        username: String?,
        password: String?,
    )

    suspend fun downloadObject(
        downloadUrl: String,
        outPath: Path,
        username: String?,
        password: String?,
    ): Result<Unit, LfsError>
}

class LfsNetworkDataSource @Inject constructor(
    private val client: HttpClient,
) : ILfsNetworkDataSource {
    override suspend fun postBatchObjects(
        remoteUrl: String,
        lfsPrepareUploadObjectBatch: LfsPrepareUploadObjectBatch,
        username: String?,
        password: String?
    ): Result<LfsObjects, LfsError> {
        val response = client.post("${remoteUrl.removeSuffix("/")}/objects/batch") {
            this.headers {
                this["Accept"] = "application/vnd.git-lfs+json"
                this["Content-Type"] = "application/vnd.git-lfs+json"
            }

            if (username != null && password != null) {
                basicAuth(username, password)
            }

            setBody(Json.encodeToString(lfsPrepareUploadObjectBatch))
        }

        if (response.status != HttpStatusCode.OK) {
            return Result.Err(LfsError.HttpError(response.status))
        }


        return Result.Ok(Json.decodeFromString(response.bodyAsText()))
    }

    override suspend fun uploadBatchObjects(
        remoteUrl: String,
        lfsPrepareUploadObjectBatch: LfsPrepareUploadObjectBatch,
        username: String?,
        password: String?,
    ) {
        val response = client.post("$remoteUrl/objects/batch") {
            if (username != null && password != null) {
                basicAuth(username, password)
            }

            this.headers {
                this["Accept"] = "application/vnd.git-lfs+json"
                this["Content-Type"] = "application/vnd.git-lfs+json"
            }


            this.setBody(Json.encodeToString(lfsPrepareUploadObjectBatch))
        }

        if (response.status.value != 200) {
            throw Exception("Code is ${response.status.value}...")
        }
    }

    override suspend fun uploadObject(
        uploadUrl: String,
        oid: String,
        file: Path,
        size: Long,
        username: String?,
        password: String?,
    ) {
        val response = client.put(uploadUrl) {
            if (username != null && password != null) {
                basicAuth(username, password)
            }

            this.headers {
                this["Accept"] = "application/vnd.git-lfs"
                this["Content-Length"] = size.toString()
            }

            setBody(file.readChannel())
        }

        if (response.status != HttpStatusCode.OK) {
            throw Exception("Status is ${response.status} instead of HTTP OK 200")
        }
    }

    override suspend fun downloadObject(
        downloadUrl: String,
        outPath: Path,
        username: String?,
        password: String?,
    ): Result<Unit, LfsError> {
        val response = client.get(downloadUrl) {
            if (username != null && password != null) {
                basicAuth(username, password)
            }

            this.headers {
                this["Accept"] = "application/vnd.git-lfs"
            }
        }

        if (response.status != HttpStatusCode.OK) {
            return Result.Err(LfsError.HttpError(response.status))
        } else if (response.status != HttpStatusCode.OK) {
            throw Exception("Status is ${response.status} instead of HTTP OK 200")
        }

        val channel: ByteReadChannel = response.body()
        val file = outPath.toFile()
        withContext(Dispatchers.IO) {
            file.parentFile?.mkdirs()
            file.createNewFile()
        }

        while (!channel.isClosedForRead) {
            val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
            while (!packet.exhausted()) {
                val bytes = packet.readByteArray()
                file.appendBytes(bytes)
            }
        }

        return Result.Ok(Unit)
    }
}