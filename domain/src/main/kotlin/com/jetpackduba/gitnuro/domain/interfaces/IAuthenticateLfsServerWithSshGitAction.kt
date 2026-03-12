package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.lfs.LfsSshAuthenticateResult
import com.jetpackduba.gitnuro.domain.models.OperationType

interface IAuthenticateLfsServerWithSshGitAction {
    suspend operator fun invoke(
        lfsServerUrl: String,
        operationType: OperationType,
    ): LfsSshAuthenticateResult
}