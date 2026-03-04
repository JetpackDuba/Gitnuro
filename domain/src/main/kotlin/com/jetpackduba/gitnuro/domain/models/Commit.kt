package com.jetpackduba.gitnuro.domain.models

data class Commit(
    val hash: String,
    val message: String,
    val comitter: Identity,
    val author: Identity,
    val date: Long,
)