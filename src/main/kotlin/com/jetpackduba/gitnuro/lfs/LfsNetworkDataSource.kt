package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.NetworkConstants
import com.jetpackduba.gitnuro.Result
import com.jetpackduba.gitnuro.models.lfs.LfsObjectBatch
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
        headers: Map<String, String>,
        username: String?,
        password: String?,
    ): Result<LfsObjects, LfsError>

    suspend fun uploadObject(
        uploadUrl: String,
        oid: String,
        file: Path,
        size: Long,
        headers: Map<String, String>,
        username: String?,
        password: String?,
    ): Result<Unit, LfsError>

    suspend fun verify(
        url: String,
        oid: String,
        size: Long,
        headers: Map<String, String>,
        username: String?,
        password: String?,
    ): Result<Unit, LfsError>

    suspend fun downloadObject(
        downloadUrl: String,
        outPath: Path,
        headers: Map<String, String>,
        username: String?,
        password: String?,
    ): Result<Unit, LfsError>
}

private const val DEFAULT_ACCEPT_TYPE = "application/vnd.git-lfs+json"

class LfsNetworkDataSource @Inject constructor(
    private val client: HttpClient,
) : ILfsNetworkDataSource {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun postBatchObjects(
        remoteUrl: String,
        lfsPrepareUploadObjectBatch: LfsPrepareUploadObjectBatch,
        headers: Map<String, String>,
        username: String?,
        password: String?,
    ): Result<LfsObjects, LfsError> {
        val response = client.post("${remoteUrl.removeSuffix("/")}/objects/batch") {
            setHeadersAndBasicAuth(headers, username, password)

            this.contentType(ContentType("application", "vnd.git-lfs+json"))

            setBody(json.encodeToString(lfsPrepareUploadObjectBatch))
        }

        return if (response.status != HttpStatusCode.OK) {
            Result.Err(LfsError.HttpError(response.status))
        } else {
            Result.Ok(json.decodeFromString(response.bodyAsText()))
        }
    }

    override suspend fun uploadObject(
        uploadUrl: String,
        oid: String,
        file: Path,
        size: Long,
        headers: Map<String, String>,
        username: String?,
        password: String?,
    ): Result<Unit, LfsError> {
        val response = client.put(uploadUrl) {
            setHeadersAndBasicAuth(headers, username, password)

            this.headers[NetworkConstants.CONTENT_LENGTH_HEADER] = size.toString()

            setBody(file.readChannel())
        }

        return if (response.status != HttpStatusCode.OK) {
            Result.Err(LfsError.HttpError(response.status))
        } else {
            Result.Ok(Unit)
        }
    }

    override suspend fun verify(
        url: String,
        oid: String,
        size: Long,
        headers: Map<String, String>,
        username: String?,
        password: String?,
    ): Result<Unit, LfsError> {
        val response = client.post(url) {
            setHeadersAndBasicAuth(headers, username, password)

            val body = LfsObjectBatch(oid, size)
            setBody(json.encodeToString(body))
        }

        return if (response.status != HttpStatusCode.OK) {
            Result.Err(LfsError.HttpError(response.status))
        } else {
            Result.Ok(Unit)
        }
    }

    override suspend fun downloadObject(
        downloadUrl: String,
        outPath: Path,
        headers: Map<String, String>,
        username: String?,
        password: String?,
    ): Result<Unit, LfsError> {
        val response = client.get(downloadUrl) {
            setHeadersAndBasicAuth(headers, username, password)
        }

        if (response.status != HttpStatusCode.OK) {
            return Result.Err(LfsError.HttpError(response.status))
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

    private fun HttpRequestBuilder.setHeadersAndBasicAuth(
        newHeaders: Map<String, String>,
        user: String?,
        password: String?,
    ) {
        // Some headers should not be included because Ktor already sets them and adding them twice makes it crash
        val excludedHeaders = listOf(
            "Transfer-Encoding"
        )

        val filteredNewHeaders = newHeaders.filter { it.key !in excludedHeaders }

        for (header in filteredNewHeaders) {
            header(header.key, header.value)
        }

        this.headers {
            if (
                !headers.contains(NetworkConstants.AUTH_HEADER) &&
                (user != null && password != null)
            ) {
                basicAuth(user, password)
            }

            if (!this.contains(NetworkConstants.ACCEPT_HEADER)) {
                this[NetworkConstants.ACCEPT_HEADER] = DEFAULT_ACCEPT_TYPE
            }
        }
    }
}