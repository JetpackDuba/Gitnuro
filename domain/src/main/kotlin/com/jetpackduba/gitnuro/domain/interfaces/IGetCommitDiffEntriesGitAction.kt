package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit

interface IGetCommitDiffEntriesGitAction {
    suspend operator fun invoke(git: Git, commit: Commit): List<DiffEntry>
}