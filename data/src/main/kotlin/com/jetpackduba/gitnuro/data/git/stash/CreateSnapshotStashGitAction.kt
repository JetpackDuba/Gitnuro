package com.jetpackduba.gitnuro.data.git.stash

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.mappers.JGitCommitMapper
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.interfaces.ICreateSnapshotStashGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import javax.inject.Inject

class CreateSnapshotStashGitAction @Inject constructor(
    private val commitMapper: JGitCommitMapper,
    private val jgit: JGit,
) : ICreateSnapshotStashGitAction {
    override suspend fun invoke(
        repositoryPath: String,
        message: String,
        includeUntracked: Boolean
    ): Either<Commit?, GitError> = jgit.provide(repositoryPath) { git ->
        SnapshotStashCreateCommand(
            repository = git.repository,
            // TODO Fix this
            workingDirectoryMessage = "TMP MESSAGE"/* TODO getString(
                        Res.string.merge_automatic_stash_description,
                        branch.simpleName,
                        git.repository.branch
                    )*/,
            includeUntracked = true
        )
            .call()
            ?.let { commitMapper.toDomain(it) }
    }
}