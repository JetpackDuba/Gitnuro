package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.DiffResult
import com.jetpackduba.gitnuro.domain.models.DiffType
import org.eclipse.jgit.api.Git

interface IFormatDiffGitAction {
    suspend operator fun invoke(
        git: Git,
        diffType: DiffType,
        isDisplayFullFile: Boolean,
    ): DiffResult
}