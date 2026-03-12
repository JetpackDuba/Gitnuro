package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.StatusEntry
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.dircache.DirCache

interface IStageEntryGitAction {
    suspend operator fun invoke(git: Git, statusEntry: StatusEntry): DirCache
}