package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.revwalk.RevCommit

interface IDoCommitGitAction {
    suspend operator fun invoke(
        git: Git,
        message: String,
        amend: Boolean,
        author: PersonIdent?,
    ): RevCommit
}