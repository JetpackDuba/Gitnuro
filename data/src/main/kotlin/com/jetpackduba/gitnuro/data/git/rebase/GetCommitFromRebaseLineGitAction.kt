package com.jetpackduba.gitnuro.data.git.rebase

import com.jetpackduba.gitnuro.data.mappers.JGitCommitMapper
import com.jetpackduba.gitnuro.domain.interfaces.IGetCommitFromRebaseLineGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.AmbiguousObjectException
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class GetCommitFromRebaseLineGitAction @Inject constructor(
    private val commitMapper: JGitCommitMapper,
) : IGetCommitFromRebaseLineGitAction {
    override operator fun invoke(git: Git, commit: AbbreviatedObjectId, shortMessage: String): Commit? {
        val resolvedList: List<ObjectId?> = try {
            listOf(git.repository.resolve("${commit.name()}^{commit}"))
        } catch (ex: AmbiguousObjectException) {
            ex.candidates.toList()
        }

        if (resolvedList.isEmpty()) {
            println("Commit search failed for line $commit - $shortMessage")
            return null
        } else if (resolvedList.count() == 1) {
            val resolvedId = resolvedList.firstOrNull()

            return if (resolvedId == null)
                null
            else {
                git.repository.parseCommit(resolvedId)?.let {
                    commitMapper.toDomain(it)
                }

            }
        } else {
            println("Multiple matching commits for line $commit - $shortMessage")
            for (candidateId in resolvedList) {
                val candidateCommit = git.repository.parseCommit(candidateId)
                if (candidateCommit != null && shortMessage == candidateCommit.shortMessage)
                    return commitMapper.toDomain(candidateCommit)
            }

            println("None of the matching commits has a matching short message")
            return null
        }
    }
}