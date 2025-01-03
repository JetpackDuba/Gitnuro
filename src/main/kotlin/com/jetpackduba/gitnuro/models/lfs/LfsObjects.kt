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
    @SerialName("download") var download: Download?,
    @SerialName("upload") var upload: Upload?,
)

@Serializable
data class Download(
    @SerialName("href") var href: String? = null,
    @SerialName("header") var header: Header? = Header(),
    @SerialName("expires_at") var expiresAt: String? = null
)

@Serializable
data class Upload(
    @SerialName("href") var href: String?,
    @SerialName("header") var header: Header?,
    @SerialName("expires_at") var expiresAt: String?,
)

@Serializable
data class Header(
    @SerialName("Accept") var accept: String? = null
)