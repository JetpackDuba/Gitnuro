package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.api.Git

interface IGetFileCommitsAction {
    suspend operator fun invoke(
        git: Git,
        filePath: String
    ): List<Commit>
}