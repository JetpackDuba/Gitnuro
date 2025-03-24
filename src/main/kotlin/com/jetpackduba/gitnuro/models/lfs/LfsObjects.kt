package com.jetpackduba.gitnuro.models.lfs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
data class LfsObjects(
    @SerialName("objects") val objects: ArrayList<LfsObject>,
)

@Serializable
data class LfsObject(
    @SerialName("oid") val oid: String,
    @SerialName("size") val size: Long,
    @SerialName("actions") val actions: Actions? = null,
)

@Serializable
data class Actions(
    @SerialName("download") val download: RemoteObjectAccessInfo? = null,
    @SerialName("upload") val upload: RemoteObjectAccessInfo? = null,
    @SerialName("verify") val verify: RemoteObjectAccessInfo? = null,
)

@Serializable
data class RemoteObjectAccessInfo(
    @SerialName("href") val href: String,
    @SerialName("header") val header: Map<String, String>? = null,
    @SerialName("expires_at") val expiresAt: String? = null,
)
