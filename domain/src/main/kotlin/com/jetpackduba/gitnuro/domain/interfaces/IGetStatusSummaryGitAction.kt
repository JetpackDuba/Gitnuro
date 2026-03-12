package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.StatusSummary
import org.eclipse.jgit.api.Git

interface IGetStatusSummaryGitAction {
    suspend operator fun invoke(git: Git): StatusSummary
}