package app.git

import app.exceptions.UncommitedChangesDetectedException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.api.RebaseCommand
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler
import org.eclipse.jgit.api.RebaseResult
import org.eclipse.jgit.lib.RebaseTodoLine
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevCommitList
import org.eclipse.jgit.revwalk.RevWalk
import javax.inject.Inject

class RebaseManager @Inject constructor(
    private val branchesManager: BranchesManager,
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

    suspend fun rebaseInteractive(git: Git, interactiveHandler: InteractiveHandler, commit: RevCommit) {
        //TODO Check possible rebase errors by checking the result
        git.rebase()
            .runInteractively(interactiveHandler)
            .setOperation(RebaseCommand.Operation.BEGIN)
            .setUpstream(commit)
            .call()
    }

    suspend fun rebaseLinesFullMessage(
        git: Git,
        rebaseTodoLines: List<RebaseTodoLine>,
        commit: RevCommit
    ): Map<String, String> = withContext(Dispatchers.IO) {
        val revWalk = RevWalk(git.repository)
        markCurrentBranchAsStart(revWalk, git)

        val revCommitList = RevCommitList<RevCommit>()
        revCommitList.source(revWalk)
        revCommitList.fillTo(commit, Int.MAX_VALUE)

        val commitsList = revCommitList.toList()

        return@withContext rebaseTodoLines.associate { rebaseLine ->
            val fullMessage = getFullMessage(rebaseLine, commitsList) ?: rebaseLine.shortMessage
            rebaseLine.commit.name() to fullMessage
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
        val currentBranch = branchesManager.currentBranchRef(git) ?: throw Exception("Null current branch")
        val refTarget = revWalk.parseAny(currentBranch.leaf.objectId)

        if (refTarget is RevCommit)
            revWalk.markStart(refTarget)
        else
            throw Exception("Ref target is not a RevCommit")
    }
}