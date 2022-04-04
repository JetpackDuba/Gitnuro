package app.updates

import kotlinx.serialization.Serializable

@Serializable
data class Update(
    val appVersion: String,
    val appCode: Int,
    val downloadUrl: String,
)
