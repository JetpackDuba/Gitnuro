package com.jetpackduba.gitnuro.git.submodules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class InitializeAllSubmodulesUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): Unit = withContext(Dispatchers.IO) {
        git.submoduleInit()
            .call()
    }
}