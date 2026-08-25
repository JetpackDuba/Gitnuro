package com.jetpackduba.gitnuro.domain.models

import androidx.compose.runtime.Immutable

@Immutable
data class PersistedCommitMessage(
    val commitMessage: String?,
    val mergeMessage: String?,
    val squashMessage: String?,
)