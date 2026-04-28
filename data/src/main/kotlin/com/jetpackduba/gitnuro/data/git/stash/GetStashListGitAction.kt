package com.jetpackduba.gitnuro.data.git.stash

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.data.mappers.JGitCommitMapper
import com.jetpackduba.gitnuro.domain.interfaces.IGetStashListGitAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class GetStashListGitAction @Inject constructor(
    private val commitMapper: JGitCommitMapper,
) : IGetStashListGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit(repositoryPath) { git ->
        invoke(git)
            .map { commitMapper.toDomain(it) }
    }

    suspend operator fun invoke(git: Git): List<RevCommit> = withContext(Dispatchers.IO) {
        return@withContext git
            .stashList()
            .call()
            .toList()
    }
}
