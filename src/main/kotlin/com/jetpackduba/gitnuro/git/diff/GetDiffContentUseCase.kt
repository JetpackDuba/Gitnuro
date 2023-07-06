package com.jetpackduba.gitnuro.git.diff

import com.jetpackduba.gitnuro.git.EntryContent
import com.jetpackduba.gitnuro.git.RawFileManager
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import java.io.ByteArrayOutputStream
import java.io.InvalidObjectException
import javax.inject.Inject

class GetDiffContentUseCase @Inject constructor(
    private val rawFileManager: RawFileManager,
) {
    operator fun invoke(
        repository: Repository,
        diffEntry: DiffEntry,
        oldTreeIterator: AbstractTreeIterator?,
        newTreeIterator: AbstractTreeIterator?,
    ): DiffContent {
        val outputStream = ByteArrayOutputStream() // Dummy output stream used for the diff formatter
        outputStream.use {
            val diffFormatter = DiffFormatter(outputStream).apply {
                setRepository(repository)
            }

            if (oldTreeIterator != null && newTreeIterator != null) {
                diffFormatter.scan(oldTreeIterator, newTreeIterator)
            }

            val fileHeader = diffFormatter.toFileHeader(diffEntry)

            val rawOld = rawFileManager.getRawContent(
                repository,
                DiffEntry.Side.OLD,
                diffEntry,
                oldTreeIterator,
                newTreeIterator
            )
            val rawNew = rawFileManager.getRawContent(
                repository,
                DiffEntry.Side.NEW,
                diffEntry,
                oldTreeIterator,
                newTreeIterator
            )

            if (rawOld == EntryContent.InvalidObjectBlob || rawNew == EntryContent.InvalidObjectBlob)
                throw InvalidObjectException("Invalid object in diff format")

            return DiffContent(fileHeader, rawOld, rawNew)
        }
    }
}