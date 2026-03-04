package com.jetpackduba.gitnuro.domain.git

import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetWorkspacePathGitAction @Inject constructor() {
    operator fun invoke(git: Git): String {
        return git.repository.workTree.absolutePath
    }
}