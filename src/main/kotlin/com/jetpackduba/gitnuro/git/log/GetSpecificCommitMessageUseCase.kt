package com.jetpackduba.gitnuro.git.log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetSpecificCommitMessageUseCase @Inject constructor(
    private val findCommitUseCase: FindCommitUseCase,
) {
    suspend operator fun invoke(git: Git, commitId: String): String = withContext(Dispatchers.IO) {
        return@withContext findCommitUseCase(git, commitId)?.fullMessage.orEmpty()
    }
}