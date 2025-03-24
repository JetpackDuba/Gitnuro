package com.jetpackduba.gitnuro.models.lfs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LfsPrepareUploadObjectBatch(
    @SerialName("operation") val operation: String,
    @SerialName("objects") val objects: List<LfsObjectBatch>,
    @SerialName("transfers") val transfers: List<String>,
    @SerialName("ref") val ref: LfsRef?,
    @SerialName("hash_algo") val hashAlgo: String? = null,
)

@Serializable
data class LfsObjectBatch(
    @SerialName("oid") val oid: String,
    @SerialName("size") val size: Long,
)

@Serializable
data class LfsRef(
    @SerialName("name") val name: String?,
)