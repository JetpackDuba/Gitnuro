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
    @SerialName("size") var size: Int,
    @SerialName("actions") var actions: Actions,
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

//@Serializable
//data class Header(
//    @SerialName("Accept") var accept: String? = null,
//    @SerialName("Authorization") var authorization: String? = null,
//)