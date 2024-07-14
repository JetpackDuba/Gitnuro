package com.jetpackduba.gitnuro.git.diff

import com.jetpackduba.gitnuro.extensions.filePath
import com.jetpackduba.gitnuro.git.DiffType
import com.jetpackduba.gitnuro.git.EntryContent
import com.jetpackduba.gitnuro.git.submodules.GetSubmodulesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import java.io.InvalidObjectException
import javax.inject.Inject

class FormatDiffUseCase @Inject constructor(
    private val formatHunksUseCase: FormatHunksUseCase,
    private val getDiffContentUseCase: GetDiffContentUseCase,
    private val canGenerateTextDiffUseCase: CanGenerateTextDiffUseCase,
    private val getDiffEntryFromDiffTypeUseCase: GetDiffEntryFromDiffTypeUseCase,
    private val getSubmodulesUseCase: GetSubmodulesUseCase,
) {
    suspend operator fun invoke(
        git: Git,
        diffType: DiffType,
        isDisplayFullFile: Boolean
    ): DiffResult = withContext(Dispatchers.IO) {
        val repository = git.repository
        val submodules = getSubmodulesUseCase(git)

        val diffEntry = getDiffEntryFromDiffTypeUseCase(git, diffType)

        var diffResult: DiffResult
        val submoduleStatus = submodules[diffEntry.filePath]

        if (submoduleStatus != null) {
            diffResult = DiffResult.Submodule(diffEntry, submoduleStatus)
        } else {
            val oldTree: DirCacheIterator?
            val newTree: FileTreeIterator?

            if (diffType is DiffType.UnstagedDiff) {
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

            if (rawOld == EntryContent.InvalidObjectBlob || rawNew == EntryContent.InvalidObjectBlob) {
                throw InvalidObjectException("Invalid object in diff format")
            } else if (rawOld == EntryContent.Submodule || rawNew == EntryContent.Submodule) {
                diffResult = DiffResult.Submodule(diffEntry, null)
            } else {
                diffResult = DiffResult.Text(diffEntry, emptyList())

                // If we can, generate text diff (if one of the files has never been a binary file)
                val hasGeneratedTextDiff = canGenerateTextDiffUseCase(rawOld, rawNew) { oldRawText, newRawText ->
                    diffResult =
                        DiffResult.Text(
                            diffEntry,
                            formatHunksUseCase(fileHeader, oldRawText, newRawText, isDisplayFullFile)
                        )
                }

                if (!hasGeneratedTextDiff) {
                    diffResult = DiffResult.NonText(diffEntry, rawOld, rawNew)
                }
            }
        }

        return@withContext diffResult
    }
}