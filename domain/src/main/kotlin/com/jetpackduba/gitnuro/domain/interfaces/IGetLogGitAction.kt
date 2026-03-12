package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.git.graph.GraphCommitList
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IGetLogGitAction {
    suspend operator fun invoke(
        git: Git,
        currentBranch: Ref?,
        hasUncommittedChanges: Boolean,
        commitsLimit: Int,
        cachedCommitList: GraphCommitList? = null,
    ): GraphCommitList
}