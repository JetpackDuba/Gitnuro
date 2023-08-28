package com.jetpackduba.gitnuro.git.rebase

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.AmbiguousObjectException
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class GetCommitFromRebaseLineUseCase @Inject constructor() {
    operator fun invoke(git: Git, commit: AbbreviatedObjectId, shortMessage: String): RevCommit? {
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
            else
                git.repository.parseCommit(resolvedId)
        } else {
            println("Multiple matching commits for line $commit - $shortMessage")
            for (candidateId in resolvedList) {
                val candidateCommit = git.repository.parseCommit(candidateId)
                if (shortMessage == candidateCommit.shortMessage)
                    return candidateCommit
            }

            println("None of the matching commits has a matching short message")
            return null
        }
    }
}