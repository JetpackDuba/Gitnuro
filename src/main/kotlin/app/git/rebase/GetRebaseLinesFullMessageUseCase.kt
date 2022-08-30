package app.git.rebase

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.errors.AmbiguousObjectException
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.revwalk.RevCommit
import javax.inject.Inject

class GetRebaseLinesFullMessageUseCase @Inject constructor() {
    suspend operator fun invoke(
        git: Git,
        rebaseTodoLines: List<RebaseTodoLine>,
    ): Map<String, String> = withContext(Dispatchers.IO) {
        return@withContext rebaseTodoLines.associate { line ->
            val commit = getCommitFromLine(git, line)
            val fullMessage = commit?.fullMessage ?: line.shortMessage
            line.commit.name() to fullMessage
        }
    }

    private fun getCommitFromLine(git: Git, line: RebaseTodoLine): RevCommit? {
        val resolvedList: List<ObjectId?> = try {
            listOf(git.repository.resolve("${line.commit.name()}^{commit}"))
        } catch (ex: AmbiguousObjectException) {
            ex.candidates.toList()
        }

        if (resolvedList.isEmpty()) {
            println("Commit search failed for line ${line.commit} - ${line.shortMessage}")
            return null
        } else if (resolvedList.count() == 1) {
            val resolvedId = resolvedList.firstOrNull()

            return if (resolvedId == null)
                null
            else
                git.repository.parseCommit(resolvedId)
        } else {
            println("Multiple matching commits for line ${line.commit} - ${line.shortMessage}")
            for (candidateId in resolvedList) {
                val candidateCommit = git.repository.parseCommit(candidateId)
                if (line.shortMessage == candidateCommit.shortMessage)
                    return candidateCommit
            }

            println("None of the matching commits has a matching short message")
            return null
        }
    }
}