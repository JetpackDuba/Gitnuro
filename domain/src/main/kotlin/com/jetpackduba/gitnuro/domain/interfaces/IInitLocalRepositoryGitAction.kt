package com.jetpackduba.gitnuro.domain.interfaces

import java.io.File

interface IInitLocalRepositoryGitAction {
    suspend operator fun invoke(repoDir: File): Unit
}