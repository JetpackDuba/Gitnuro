package com.jetpackduba.gitnuro.git.diff

import com.jetpackduba.gitnuro.git.FileDiffType
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
    suspend operator fun invoke(git: Git, fileDiffType: FileDiffType): DiffEntry {
        val repository = git.repository
        val byteArrayOutputStream = ByteArrayOutputStream()

        return DiffFormatter(byteArrayOutputStream).use { formatter ->
            formatter.setRepository(repository)

            val oldTree = DirCacheIterator(repository.readDirCache())
            val newTree = FileTreeIterator(repository)

            if (fileDiffType is FileDiffType.UnstagedFileDiff)
                formatter.scan(oldTree, newTree)

            val diffEntry = when (fileDiffType) {
                is FileDiffType.CommitFileDiff -> {
                    fileDiffType.diffEntry
                }

                is FileDiffType.UncommittedFileDiff -> {
                    getDiffEntryFromStatusEntryUseCase(git, fileDiffType is FileDiffType.StagedFileDiff, fileDiffType.statusEntry)
                }
            }

            formatter.format(diffEntry)
            formatter.flush()

            diffEntry
        }
    }
}