package com.jetpackduba.gitnuro.data.git.stash

import com.jetpackduba.gitnuro.data.mappers.JGitCommitMapper
import com.jetpackduba.gitnuro.domain.interfaces.IGetStashListGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class GetStashListGitAction @Inject constructor(
    private val commitMapper: JGitCommitMapper,
    private val jgitGetStashListGitAction: JGitGetStashListGitAction,
) : IGetStashListGitAction {
    override suspend operator fun invoke(git: Git): List<Commit> = withContext(Dispatchers.IO) {
        return@withContext jgitGetStashListGitAction(git)
            .map { commitMapper.toDomain(it) }
    }
}

class JGitGetStashListGitAction @Inject constructor() {
    suspend operator fun invoke(git: Git): List<RevCommit> = withContext(Dispatchers.IO) {
        return@withContext git
            .stashList()
            .call()
            .toList()
    }
}