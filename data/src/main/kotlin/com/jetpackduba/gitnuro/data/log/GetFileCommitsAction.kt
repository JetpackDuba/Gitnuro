package com.jetpackduba.gitnuro.data.log

import com.jetpackduba.gitnuro.data.mappers.JGitCommitMapper
import com.jetpackduba.gitnuro.domain.interfaces.IGetFileCommitsAction
import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetFileCommitsAction @Inject constructor(
    private val commitMapper: JGitCommitMapper,
) : IGetFileCommitsAction {
    override suspend fun invoke(
        git: Git,
        filePath: String
    ): List<Commit> = git.log()
        .addPath(filePath)
        .call()
        .toList()
        .map { commitMapper.toDomain(it) }

}