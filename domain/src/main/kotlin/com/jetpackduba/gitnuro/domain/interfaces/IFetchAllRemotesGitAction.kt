package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.models.Remote
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.RemoteConfig

interface IFetchAllRemotesGitAction {
    suspend operator fun invoke(git: Git, specificRemote: Remote? = null)
}