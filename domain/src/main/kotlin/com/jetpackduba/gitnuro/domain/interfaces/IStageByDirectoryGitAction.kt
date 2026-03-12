package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.dircache.DirCache

interface IStageByDirectoryGitAction {
    suspend operator fun invoke(git: Git, dir: String): DirCache
}