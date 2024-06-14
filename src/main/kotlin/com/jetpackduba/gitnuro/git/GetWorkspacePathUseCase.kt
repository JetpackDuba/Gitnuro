package com.jetpackduba.gitnuro.git

import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetWorkspacePathUseCase @Inject constructor() {
    operator fun invoke(git: Git): String {
        return git.repository.workTree.absolutePath
    }
}