package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.StatusEntry
import org.eclipse.jgit.api.Git

interface IStageAllGitAction {
    suspend operator fun invoke(git: Git, entries: List<StatusEntry>?): Unit
}