package com.jetpackduba.gitnuro.data.git.tags

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.ICreateTagGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import javax.inject.Inject


class CreateTagGitAction @Inject constructor(
    private val jgit: JGit,
) : ICreateTagGitAction {
    override suspend operator fun invoke(repositoryPath: String, tag: String, commit: Commit) = jgit.provide(repositoryPath) { git ->
        val commitId = ObjectId.fromString(commit.hash) // TODO Should this be used instead of "git.repository.resolve(revCommit.hash) ?: throw Exception("Commit ${revCommit.hash} not found")" used in other places?
        val commit: RevCommit? = RevWalk(git.repository).use { revWalk ->
            revWalk.parseCommit(commitId)
        }

        git
            .tag()
            .setAnnotated(true)
            .setName(tag)
            .setObjectId(commit)
            .call()

        Unit
    }
}