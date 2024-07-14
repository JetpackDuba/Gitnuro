package com.jetpackduba.gitnuro.git.diff

import com.jetpackduba.gitnuro.git.DiffType
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class GetDiffEntryFromDiffTypeUseCase @Inject constructor(
    private val getDiffEntryFromStatusEntryUseCase: GetDiffEntryFromStatusEntryUseCase,
) {
    suspend operator fun invoke(git: Git, diffType: DiffType): DiffEntry {
        val repository = git.repository
        val byteArrayOutputStream = ByteArrayOutputStream()

        return DiffFormatter(byteArrayOutputStream).use { formatter ->
            formatter.setRepository(repository)

            val oldTree = DirCacheIterator(repository.readDirCache())
            val newTree = FileTreeIterator(repository)

            if (diffType is DiffType.UnstagedDiff)
                formatter.scan(oldTree, newTree)

            val diffEntry = when (diffType) {
                is DiffType.CommitDiff -> {
                    diffType.diffEntry
                }

                is DiffType.UncommittedDiff -> {
                    getDiffEntryFromStatusEntryUseCase(git, diffType is DiffType.StagedDiff, diffType.statusEntry)
                }
            }

            formatter.format(diffEntry)
            formatter.flush()

            diffEntry
        }
    }
}