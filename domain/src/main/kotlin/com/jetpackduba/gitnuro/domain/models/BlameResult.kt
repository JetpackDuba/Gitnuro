package com.jetpackduba.gitnuro.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class BlameResult(
    val path: String,
    val lines: List<BlameLine>,
)

@Immutable
data class BlameLine(
    val identity: Identity?,
    val commit: Commit?,
    val content: String,
)