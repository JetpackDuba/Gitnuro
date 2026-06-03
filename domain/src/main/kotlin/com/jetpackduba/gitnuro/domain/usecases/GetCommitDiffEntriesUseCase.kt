package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.bind
import com.jetpackduba.gitnuro.domain.interfaces.IGetCommitDiffEntriesGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IGetCommitFromHashGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
import org.eclipse.jgit.diff.DiffEntry
import javax.inject.Inject

class GetCommitDiffEntriesUseCase @Inject constructor(
    private val getCommitDiffEntriesGitAction: IGetCommitDiffEntriesGitAction,
    private val getCommitFromHashGitAction: IGetCommitFromHashGitAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(commit: Commit) = useCaseExecutor.execute<List<DiffEntry>>(
    ) { repositoryPath ->
        // TODO Restore stashes change loading. IIRC only stashes have 3 parents, usually.
        val entries = getCommitDiffEntriesGitAction(repositoryPath, commit).bind().toMutableList()

        if (commit.parentCount == 3) {
            var untrackedFilesCommit: Commit? = null

            for (hash in commit.parentsHashes) {
                val parentCommit = getCommitFromHashGitAction(repositoryPath, hash).bind() ?: continue

                if (parentCommit.message.startsWith("untracked files on") && parentCommit.parentCount == 0) {
                    untrackedFilesCommit = parentCommit
                    break
                }
            }

            if (untrackedFilesCommit != null) {
                val untrackedFilesChanges = getCommitDiffEntriesGitAction(repositoryPath, untrackedFilesCommit).bind()

                if (untrackedFilesChanges.all { it.changeType == DiffEntry.ChangeType.ADD }) { // All files should be new
                    entries.addAll(untrackedFilesChanges)
                }
            }
        }

        Either.Ok(entries)
    }
}