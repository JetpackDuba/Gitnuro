package com.jetpackduba.gitnuro.git.workspace

import com.jetpackduba.gitnuro.system.systemSeparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class UnstageByDirectoryUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, dir: String) = withContext(Dispatchers.IO) {
        git.reset()
            .addPath(dir)
            .call()
    }
}
