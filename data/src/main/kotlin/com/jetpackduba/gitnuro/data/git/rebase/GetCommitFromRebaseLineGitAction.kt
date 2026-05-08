package com.jetpackduba.gitnuro.data.git.rebase

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.mappers.JGitCommitMapper
import com.jetpackduba.gitnuro.domain.interfaces.IGetCommitFromRebaseLineGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.errors.AmbiguousObjectException
import org.eclipse.jgit.lib.AbbreviatedObjectId
import org.eclipse.jgit.lib.ObjectId
import javax.inject.Inject

class GetCommitFromRebaseLineGitAction @Inject constructor(
    private val commitMapper: JGitCommitMapper,
    private val jgit: JGit,
) : IGetCommitFromRebaseLineGitAction {
    // TODO Why is the short message needed? When would a commit search fail or have multiple candidates for a single hash?
    //  Probably because using the short hash could return multiple objects? Can we use the full hash?
    //  I can't recall... Add a comment once discovered why
    override suspend operator fun invoke(repositoryPath: String, commitHash: String, shortMessage: String) =
        jgit.provide(repositoryPath) { git ->
            val resolvedList: List<ObjectId?> = try {
                listOf(git.repository.resolve("${commitHash}^{commit}"))
            } catch (ex: AmbiguousObjectException) {
                ex.candidates.toList()
            }

            if (resolvedList.isEmpty()) {
                println("Commit search failed for line $commitHash - $shortMessage")
                return@provide null
            } else if (resolvedList.count() == 1) {
                val resolvedId = resolvedList.firstOrNull()

                return@provide if (resolvedId == null)
                    null
                else {
                    git.repository.parseCommit(resolvedId)?.let {
                        commitMapper.toDomain(it)
                    }

                }
            } else {
                println("Multiple matching commits for line $commitHash - $shortMessage")
                for (candidateId in resolvedList) {
                    val candidateCommit = git.repository.parseCommit(candidateId)
                    if (candidateCommit != null && shortMessage == candidateCommit.shortMessage)
                        return@provide commitMapper.toDomain(candidateCommit)
                }

                println("None of the matching commits has a matching short message")
                return@provide null
            }
        }
}