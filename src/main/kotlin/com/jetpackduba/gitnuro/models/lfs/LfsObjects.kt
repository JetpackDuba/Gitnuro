package com.jetpackduba.gitnuro.models.lfs

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


@Serializable
data class LfsObjects(
    @SerialName("objects") var objects: ArrayList<LfsObject>,
)

@Serializable
data class LfsObject(
    @SerialName("oid") var oid: String,
    @SerialName("size") var size: Long,
    @SerialName("actions") var actions: Actions? = null,
)

@Serializable
data class Actions(
    @SerialName("download") var download: RemoteObjectAccessInfo? = null,
    @SerialName("upload") var upload: RemoteObjectAccessInfo? = null,
    @SerialName("verify") var verify: RemoteObjectAccessInfo? = null,
)

@Serializable
data class RemoteObjectAccessInfo(
    @SerialName("href") var href: String,
    @SerialName("header") var header: Map<String, String>? = null,
    @SerialName("expires_at") var expiresAt: String? = null,
)
