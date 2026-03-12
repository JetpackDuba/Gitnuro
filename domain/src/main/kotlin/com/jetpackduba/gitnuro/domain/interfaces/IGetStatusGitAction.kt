package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Status

interface IGetStatusGitAction {
    suspend operator fun invoke(repository: String): Status
}