package com.jetpackduba.gitnuro.domain.models

data class Commit(
    val hash: String,
    val message: String,
    val committer: Identity,
    val author: Identity,
    val date: Long,
)