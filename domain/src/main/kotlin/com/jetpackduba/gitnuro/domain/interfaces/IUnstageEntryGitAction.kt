package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.StatusEntry
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IUnstageEntryGitAction {
    suspend operator fun invoke(git: Git, statusEntry: StatusEntry): Ref
}