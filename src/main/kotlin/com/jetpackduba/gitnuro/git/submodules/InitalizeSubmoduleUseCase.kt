package com.jetpackduba.gitnuro.git.submodules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class InitializeSubmoduleUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, path: String): Unit = withContext(Dispatchers.IO) {
        git.submoduleInit()
            .addPath(path)
            .call()
    }
}