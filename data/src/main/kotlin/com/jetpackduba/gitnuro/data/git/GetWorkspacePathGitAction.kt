package com.jetpackduba.gitnuro.data.git

import com.jetpackduba.gitnuro.domain.interfaces.IGetWorkspacePathGitAction
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetWorkspacePathGitAction @Inject constructor() : IGetWorkspacePathGitAction {
    override operator fun invoke(git: Git): String {
        return git.repository.workTree.absolutePath
    }
}