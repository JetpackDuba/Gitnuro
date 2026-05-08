package com.jetpackduba.gitnuro.data.log

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.mappers.JGitCommitMapper
import com.jetpackduba.gitnuro.domain.interfaces.IGetFileCommitsAction
import javax.inject.Inject

class GetFileCommitsAction @Inject constructor(
    private val commitMapper: JGitCommitMapper,
    private val jgit: JGit,
) : IGetFileCommitsAction {
    override suspend fun invoke(
        repositoryPath: String,
        filePath: String
    ) = jgit.provide(repositoryPath) { git ->
        git.log()
            .addPath(filePath)
            .call()
            .toList()
            .map { commitMapper.toDomain(it) }
    }
}