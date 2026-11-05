package com.jetpackduba.gitnuro.data.git.diff

import com.jetpackduba.gitnuro.domain.interfaces.FileVersion
import com.jetpackduba.gitnuro.domain.models.DiffType
import com.jetpackduba.gitnuro.domain.models.EntryType
import com.jetpackduba.gitnuro.domain.models.StatusType
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.RenameDetector
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter
import java.io.ByteArrayOutputStream
import javax.inject.Inject

interface IGetDiffEntryFromDiffTypeGitAction {
    suspend operator fun invoke(git: Git, diffType: DiffType): DiffEntry
}

class GetDiffEntryFromDiffTypeGitAction @Inject constructor(
    private val getDiffEntryFromStatusEntryGitAction: GetDiffEntryFromStatusEntryGitAction,
) : IGetDiffEntryFromDiffTypeGitAction {
    override suspend operator fun invoke(git: Git, diffType: DiffType): DiffEntry {
        val repository = git.repository
        val byteArrayOutputStream = ByteArrayOutputStream()

        return DiffFormatter(byteArrayOutputStream).use { formatter ->
            formatter.setRepository(repository)

            val diffEntry = getDiffEntry(diffType, git) ?: throw IllegalStateException("Diff entry not found")

            formatter.pathFilter = PathFilter.create(diffType.filePath)

            val oldTree = DirCacheIterator(repository.readDirCache())
            val newTree = FileTreeIterator(repository)

            if (diffType.isUnstagedDiff)
                formatter.scan(oldTree, newTree)

            formatter.format(diffEntry)
            formatter.flush()

            diffEntry
        }
    }

    private suspend fun getDiffEntry(
        diffType: DiffType,
        git: Git,
    ): DiffEntry? {
        return when (diffType) {
            is DiffType.CommitDiff -> {
                diffType.diffEntry
            }
            is DiffType.UncommittedDiff -> {
                if (diffType.statusType == StatusType.RENAMED) {
                    val oldEntry = getDiffEntryFromStatusEntryGitAction(
                        git,
                        isCached = diffType.entryType == EntryType.STAGED,
                        diffType.statusEntry,
                        FileVersion.OLD
                    )

                    val newEntry = getDiffEntryFromStatusEntryGitAction(
                        git,
                        isCached = diffType.entryType == EntryType.STAGED,
                        diffType.statusEntry,
                        FileVersion.NEW
                    )

                    val renameDetector = RenameDetector(git.repository)
                    renameDetector.addAll(listOf(oldEntry, newEntry))
                    val diffEntries = renameDetector.compute()

                    diffEntries.firstOrNull()
                } else {
                    getDiffEntryFromStatusEntryGitAction(
                        git,
                        diffType.isStagedDiff,
                        diffType.statusEntry,
                        fileVersion = when (diffType.statusEntry.statusType) {
                            StatusType.ADDED -> FileVersion.NEW
                            else -> FileVersion.OLD
                        },
                    )
                }
            }
        }
    }
}