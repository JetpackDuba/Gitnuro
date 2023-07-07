package com.jetpackduba.gitnuro.git.diff

import com.jetpackduba.gitnuro.git.DiffEntryType
import com.jetpackduba.gitnuro.git.EntryContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import java.io.ByteArrayOutputStream
import java.io.InvalidObjectException
import javax.inject.Inject

class FormatDiffUseCase @Inject constructor(
    private val getDiffEntryForUncommitedDiffUseCase: GetDiffEntryForUncommitedDiffUseCase,
    private val canGenerateTextDiffUseCase: CanGenerateTextDiffUseCase,
    private val getDiffContentUseCase: GetDiffContentUseCase,
    private val formatHunksUseCase: FormatHunksUseCase,
) {
    suspend operator fun invoke(
        git: Git,
        diffEntryType: DiffEntryType,
        isDisplayFullFile: Boolean
    ): DiffResult = withContext(Dispatchers.IO) {
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

        val diffContent = getDiffContentUseCase(repository, diffEntry, oldTree, newTree)
        val fileHeader = diffContent.fileHeader

        val rawOld = diffContent.rawOld
        val rawNew = diffContent.rawNew

        if (rawOld == EntryContent.InvalidObjectBlob || rawNew == EntryContent.InvalidObjectBlob)
            throw InvalidObjectException("Invalid object in diff format")

        var diffResult: DiffResult = DiffResult.Text(diffEntry, emptyList())

        // If we can, generate text diff (if one of the files has never been a binary file)
        val hasGeneratedTextDiff = canGenerateTextDiffUseCase(rawOld, rawNew) { oldRawText, newRawText ->
            diffResult =
                DiffResult.Text(diffEntry, formatHunksUseCase(fileHeader, oldRawText, newRawText, isDisplayFullFile))

        }

        if (!hasGeneratedTextDiff) {
            diffResult = DiffResult.NonText(diffEntry, rawOld, rawNew)
        }

        return@withContext diffResult
    }
}