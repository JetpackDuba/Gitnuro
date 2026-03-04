package com.jetpackduba.gitnuro.domain.git.diff

import com.jetpackduba.gitnuro.domain.git.DiffType
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class GetDiffEntryFromDiffTypeGitAction @Inject constructor(
    private val getDiffEntryFromStatusEntryGitAction: GetDiffEntryFromStatusEntryGitAction,
) {
    suspend operator fun invoke(git: Git, diffType: DiffType): DiffEntry {
        val repository = git.repository
        val byteArrayOutputStream = ByteArrayOutputStream()

        return DiffFormatter(byteArrayOutputStream).use { formatter ->
            formatter.setRepository(repository)
            formatter.pathFilter = PathFilter.create(diffType.filePath)

            val oldTree = DirCacheIterator(repository.readDirCache())
            val newTree = FileTreeIterator(repository)

            if (diffType.isUnstagedDiff)
                formatter.scan(oldTree, newTree)

            val diffEntry = when (diffType) {
                is DiffType.CommitDiff -> {
                    diffType.diffEntry
                }

                is DiffType.UncommittedDiff -> {
                    getDiffEntryFromStatusEntryGitAction(git, diffType.isStagedDiff, diffType.statusEntry)
                }
            }

            formatter.format(diffEntry)
            formatter.flush()

            diffEntry
        }
    }
}