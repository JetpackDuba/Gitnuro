package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.revwalk.RevCommit

interface IGetCommitFromRebaseLineGitAction {
    operator fun invoke(git: Git, commit: AbbreviatedObjectId, shortMessage: String): RevCommit?
}