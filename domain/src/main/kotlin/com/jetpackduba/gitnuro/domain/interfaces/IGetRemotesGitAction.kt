package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Branch
import com.jetpackduba.gitnuro.domain.models.RemoteInfo
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref

interface IGetRemotesGitAction {
    suspend operator fun invoke(git: Git, allRemoteBranches: List<Branch>): List<RemoteInfo>
}