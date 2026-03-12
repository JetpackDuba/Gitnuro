package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.StatusEntry
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry

interface IGetDiffEntryFromStatusEntryGitAction {
    suspend operator fun invoke(
        git: Git,
        isCached: Boolean,
        statusEntry: StatusEntry,
    ): DiffEntry
}