package com.jetpackduba.gitnuro.git.diff

import com.jetpackduba.gitnuro.git.DiffEntryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class FormatDiffUseCase @Inject constructor(
    private val hunkDiffGenerator: HunkDiffGenerator,
    private val getDiffEntryForUncommitedDiffUseCase: GetDiffEntryForUncommitedDiffUseCase,
) {
    suspend operator fun invoke(git: Git, diffEntryType: DiffEntryType): DiffResult = withContext(Dispatchers.IO) {
        val byteArrayOutputStream = ByteArrayOutputStream()
        val repository = git.repository
        val diffEntry: DiffEntry

        DiffFormatter(byteArrayOutputStream).use { formatter ->
            formatter.setRepository(repository)

            val oldTree = DirCacheIterator(repository.readDirCache())
            val newTree = FileTreeIterator(repository)

            if (diffEntryType is DiffEntryType.UnstagedDiff)
                formatter.scan(oldTree, newTree)

            diffEntry = when (diffEntryType) {
                is DiffEntryType.CommitDiff -> {
                    diffEntryType.diffEntry
                }

                is DiffEntryType.UncommitedDiff -> {
                    getDiffEntryForUncommitedDiffUseCase(git, diffEntryType)
                }
            }

            formatter.format(diffEntry)
            formatter.flush()
        }

        val oldTree: DirCacheIterator?
        val newTree: FileTreeIterator?

        if (diffEntryType is DiffEntryType.UnstagedDiff) {
            oldTree = DirCacheIterator(repository.readDirCache())
            newTree = FileTreeIterator(repository)
        } else {
            oldTree = null
            newTree = null
        }

        return@withContext hunkDiffGenerator.format(
            repository,
            diffEntry,
            oldTree,
            newTree,
        )
    }
}