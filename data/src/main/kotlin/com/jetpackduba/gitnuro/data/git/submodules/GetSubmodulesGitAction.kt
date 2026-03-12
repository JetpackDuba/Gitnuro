package com.jetpackduba.gitnuro.data.git.submodules

import com.jetpackduba.gitnuro.domain.interfaces.IGetSubmodulesGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.submodule.SubmoduleStatus
import javax.inject.Inject

class GetSubmodulesGitAction @Inject constructor() : IGetSubmodulesGitAction {
    override suspend operator fun invoke(git: Git): Map<String, SubmoduleStatus> = withContext(Dispatchers.IO) {
        return@withContext git
            .submoduleStatus()
            .call()
    }
}