package com.jetpackduba.gitnuro.data.git.log

import com.jetpackduba.gitnuro.common.printError
import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.data.mappers.JGitCommitMapper
import com.jetpackduba.gitnuro.domain.GraphRevWalker
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.lib.MutableObjectId
import org.eclipse.jgit.revwalk.RevWalk
import javax.inject.Inject

private const val TAG = "JGitGraphRevWalker"

class JGitGraphRevWalker @Inject constructor(
    private val jgit: JGit,
    private val commitMapper: JGitCommitMapper,
) : GraphRevWalker {
    private var revWalk: RevWalk? = null

    override suspend fun prepare(repository: String, startingCommits: List<String>): Either<Unit, GitError> {
        val initResult = jgit.provide(repository) { git ->
            val revWalk = RevWalk(git.repository)
            this@JGitGraphRevWalker.revWalk = revWalk

            for (commit in startingCommits) {
                val oid = MutableObjectId().apply {
                    fromString(commit)
                }

                try {
                revWalk.markStart(revWalk.lookupCommit(oid))
                } catch (e: Exception) {
                    printError(TAG, "Could not mark commit $commit as start: ${e.message}", e)
                }
            }
        }

        if (initResult is Either.Err) {
            revWalk?.close()
        }

        return initResult
    }


    override fun iterator(): Iterator<Commit> {
        val revWalk = checkNotNull(revWalk) { "RevWalk can not be null, make sure to call `prepare`." }
        return object : Iterator<Commit> {
            var next: Commit? = revWalk.next()?.let { commitMapper.toDomain(it) }

            override fun next(): Commit {
                val commit = checkNotNull(next) { "Next should never be null" }
                next = revWalk.next()?.let { commitMapper.toDomain(it) }

                return commit
            }

            override fun hasNext(): Boolean = next != null
        }
    }

    override fun close() {
        revWalk?.close()
    }
}