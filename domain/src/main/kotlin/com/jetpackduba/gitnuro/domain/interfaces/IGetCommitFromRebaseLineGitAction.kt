package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.revwalk.RevCommit

interface IGetCommitFromRebaseLineGitAction {
    operator fun invoke(git: Git, commit: AbbreviatedObjectId, shortMessage: String): Commit?
}