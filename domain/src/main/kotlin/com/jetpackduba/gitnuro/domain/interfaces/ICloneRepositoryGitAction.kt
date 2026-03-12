package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.CloneState
import kotlinx.coroutines.flow.Flow
import java.io.File

interface ICloneRepositoryGitAction {
    operator fun invoke(directory: File, url: String, cloneSubmodules: Boolean): Flow<CloneState>
}