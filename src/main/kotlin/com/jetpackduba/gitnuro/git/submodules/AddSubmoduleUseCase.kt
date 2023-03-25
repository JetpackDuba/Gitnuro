package com.jetpackduba.gitnuro.git.submodules

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class AddSubmoduleUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, name: String, path: String, uri: String): Unit = withContext(Dispatchers.IO) {
        git.submoduleAdd()
            .setName(name)
            .setPath(path)
            .setURI(uri)
            .call()
    }
}