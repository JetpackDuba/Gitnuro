package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.StatusEntry
import org.eclipse.jgit.api.Git

interface IDiscardEntriesGitAction {
    suspend operator fun invoke(git: Git, statusEntries: List<StatusEntry>, staged: Boolean): Unit
}