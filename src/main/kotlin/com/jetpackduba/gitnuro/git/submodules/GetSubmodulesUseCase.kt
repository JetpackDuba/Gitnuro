package com.jetpackduba.gitnuro.git.submodules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.submodule.SubmoduleStatus
import javax.inject.Inject

class GetSubmodulesUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): Map<String, SubmoduleStatus> = withContext(Dispatchers.IO) {
        return@withContext git
            .submoduleStatus()
            .call()
    }
}