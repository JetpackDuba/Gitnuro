package com.jetpackduba.gitnuro.domain.models

data class Commit(
    val hash: String,
    val message: String,
    val committer: Identity,
    val author: Identity,
    val date: Long,
    val parentCount: Int,
) {
    val shortHash: String
        get() = this.hash.orEmpty().take(7)

    val shortMessage: String
        get() = this.message
            .trimStart()
            .replace("\r\n", "\n")
            .takeWhile { it != '\n' }
}