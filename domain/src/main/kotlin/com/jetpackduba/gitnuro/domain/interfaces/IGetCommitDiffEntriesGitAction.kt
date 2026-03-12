package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.revwalk.RevCommit

interface IGetCommitDiffEntriesGitAction {
    suspend operator fun invoke(git: Git, commit: RevCommit): List<DiffEntry>
}