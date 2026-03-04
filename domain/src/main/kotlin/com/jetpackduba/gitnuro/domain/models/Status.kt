package com.jetpackduba.gitnuro.domain.models

import com.jetpackduba.gitnuro.domain.git.workspace.StatusEntry

data class Status(
    val staged: List<StatusEntry>,
    val unstaged: List<StatusEntry>,
)