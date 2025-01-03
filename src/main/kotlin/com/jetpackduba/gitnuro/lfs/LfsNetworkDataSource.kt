package com.jetpackduba.gitnuro.lfs

import com.jetpackduba.gitnuro.models.lfs.LfsPrepareUploadObjectBatch
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.util.cio.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.file.Path
import javax.inject.Inject

interface ILfsNetworkDataSource {
    suspend fun requiresAuthForBatchObjects(
        remoteUrl: String,
    ): Boolean

    suspend fun batchObjects(
        remoteUrl: String,
        lfsObjectBatch: LfsPrepareUploadObjectBatch,
        username: String?,
        password: String?,
    )

    suspend fun uploadObject(
        remoteUrl: String,
        oid: String,
        file: Path,
        size: Long,
        username: String?,
        password: String?,
    )
}

class LfsNetworkDataSource @Inject constructor(
    private val client: HttpClient,
) : ILfsNetworkDataSource {
    override suspend fun requiresAuthForBatchObjects(remoteUrl: String): Boolean {
        val response = client.post("$remoteUrl/objects/batch") {
            this.headers {
                this["Accept"] = "application/vnd.git-lfs+json"
            }
        }

        if (response.status != HttpStatusCode.OK && response.status != HttpStatusCode.Unauthorized) {
            throw Exception("Code is not Unauthorized nor OK ${response.status.value}...")
        }

        return response.status == HttpStatusCode.Unauthorized
    }

    override suspend fun batchObjects(
        remoteUrl: String,
        lfsObjectBatch: LfsPrepareUploadObjectBatch,
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


            this.setBody(Json.encodeToString(lfsObjectBatch))
        }

        if (response.status.value != 200) {
            throw Exception("Code is ${response.status.value}...")
        }
    }

    override suspend fun uploadObject(
        remoteUrl: String,
        oid: String,
        file: Path,
        size: Long,
        username: String?,
        password: String?,
    ) {
        val response2 = client.put(
            "$remoteUrl/objects/${oid}"
        ) {
            if (username != null && password != null) {
                basicAuth(username, password)
            }

            this.headers {
                this["Accept"] = "application/vnd.git-lfs"
                this["Content-Length"] = size.toString()
            }

            setBody(file.readChannel())
        }

        if (response2.status != HttpStatusCode.OK) {
            throw Exception("Status is ${response2.status} instead of HTTP OK 200")
        }
    }
}