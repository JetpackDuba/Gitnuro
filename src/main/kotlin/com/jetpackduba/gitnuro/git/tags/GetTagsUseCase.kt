package com.jetpackduba.gitnuro.git.tags

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import javax.inject.Inject

class GetTagsUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): List<Ref> = withContext(Dispatchers.IO) {
        return@withContext git.tagList().call()
    }
}