package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Hunk
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry

interface IUnstageHunkGitAction {
    suspend operator fun invoke(git: Git, diffEntry: DiffEntry, hunk: Hunk)
}