package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit

interface ICreateTagGitAction {
    suspend operator fun invoke(git: Git, tag: String, revCommit: RevCommit): Unit
}