package app.git

import app.exceptions.UncommitedChangesDetectedException
import app.git.branches.GetCurrentBranchUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.errors.AmbiguousObjectException
import org.eclipse.jgit.lib.ObjectId
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import javax.inject.Inject

class RebaseManager @Inject constructor(
    private val getCurrentBranchUseCase: GetCurrentBranchUseCase,
) {

    suspend fun rebaseBranch(git: Git, ref: Ref) = withContext(Dispatchers.IO) {
        val rebaseResult = git.rebase()
            .setOperation(RebaseCommand.Operation.BEGIN)
            .setUpstream(ref.objectId)
            .call()

        if (rebaseResult.status == RebaseResult.Status.UNCOMMITTED_CHANGES) {
            throw UncommitedChangesDetectedException("Rebase failed, the repository contains uncommited changes.")
        }
    }

    suspend fun continueRebase(git: Git) = withContext(Dispatchers.IO) {
        git.rebase()
            .setOperation(RebaseCommand.Operation.CONTINUE)
            .call()
    }

    suspend fun abortRebase(git: Git) = withContext(Dispatchers.IO) {
        git.rebase()
            .setOperation(RebaseCommand.Operation.ABORT)
            .call()
    }

    suspend fun skipRebase(git: Git) = withContext(Dispatchers.IO) {
        git.rebase()
            .setOperation(RebaseCommand.Operation.SKIP)
            .call()
    }

    suspend fun rebaseInteractive(git: Git, interactiveHandler: InteractiveHandler, commit: RevCommit) =
        withContext(Dispatchers.IO) {
            val rebaseResult = git.rebase()
                .runInteractively(interactiveHandler)
                .setOperation(RebaseCommand.Operation.BEGIN)
                .setUpstream(commit)
                .call()

            if (rebaseResult.status == RebaseResult.Status.FAILED) {
                throw UncommitedChangesDetectedException("Rebase interactive failed.")
            }
        }

    suspend fun resumeRebase(git: Git, interactiveHandler: InteractiveHandler) = withContext(Dispatchers.IO) {
        val rebaseResult = git.rebase()
            .runInteractively(interactiveHandler)
            .setOperation(RebaseCommand.Operation.PROCESS_STEPS)
            .call()

        if (rebaseResult.status == RebaseResult.Status.FAILED) {
            throw UncommitedChangesDetectedException("Rebase interactive failed.")
        }
    }

    suspend fun rebaseLinesFullMessage(
        git: Git,
        rebaseTodoLines: List<RebaseTodoLine>,
    ): Map<String, String> = withContext(Dispatchers.IO) {

        return@withContext rebaseTodoLines.map { line ->
            val commit = getCommitFromLine(git, line)
            val fullMessage = commit?.fullMessage ?: line.shortMessage
            line.commit.name() to fullMessage
        }.toMap()
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


    private fun getFullMessage(
        rebaseTodoLine: RebaseTodoLine,
        commitsList: List<RevCommit>
    ): String? {
        val abbreviatedIdLength = rebaseTodoLine.commit.name().count()

        return commitsList.firstOrNull {
            it.abbreviate(abbreviatedIdLength).name() == rebaseTodoLine.commit.name()
        }?.fullMessage
    }

    private suspend fun markCurrentBranchAsStart(revWalk: RevWalk, git: Git) {
        val currentBranch = getCurrentBranchUseCase(git) ?: throw Exception("Null current branch")
        val refTarget = revWalk.parseAny(currentBranch.leaf.objectId)

        if (refTarget is RevCommit)
            revWalk.markStart(refTarget)
        else
            throw Exception("Ref target is not a RevCommit")
    }
}