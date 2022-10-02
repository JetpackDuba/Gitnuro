package com.jetpackduba.gitnuro.git

import com.jetpackduba.gitnuro.TempFilesManager
import com.jetpackduba.gitnuro.extensions.systemSeparator
import org.eclipse.jgit.diff.ContentSource
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.errors.BinaryBlobException
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.storage.pack.PackConfig
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.WorkingTreeIterator
import org.eclipse.jgit.util.LfsFactory
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.createTempFile


private const val DEFAULT_BINARY_FILE_THRESHOLD = PackConfig.DEFAULT_BIG_FILE_THRESHOLD
private const val IMAGE_CONTENT_TYPE = "image/"

val animatedImages = arrayOf(
    "image/gif",
    "image/webp"
)

class RawFileManager @Inject constructor(
    private val tempFilesManager: TempFilesManager,
) {
    private fun source(iterator: AbstractTreeIterator, reader: ObjectReader): ContentSource {
        return if (iterator is WorkingTreeIterator)
            ContentSource.create(iterator)
        else
            ContentSource.create(reader)
    }

    fun getRawContent(
        repository: Repository,
        side: DiffEntry.Side,
        entry: DiffEntry,
        oldTreeIterator: AbstractTreeIterator?,
        newTreeIterator: AbstractTreeIterator?,
    ): EntryContent {
        if (entry.getMode(side) === FileMode.MISSING) return EntryContent.Missing
        if (entry.getMode(side).objectType != Constants.OBJ_BLOB) return EntryContent.InvalidObjectBlob

        val reader: ObjectReader = repository.newObjectReader()
        reader.use {
            val source: ContentSource.Pair = if (oldTreeIterator != null && newTreeIterator != null) {
                ContentSource.Pair(source(oldTreeIterator, reader), source(newTreeIterator, reader))
            } else {
                val cs = ContentSource.create(reader)
                ContentSource.Pair(cs, cs)
            }

            val ldr = LfsFactory.getInstance().applySmudgeFilter(
                repository,
                source.open(side, entry), entry.diffAttribute
            )

            return try {
                EntryContent.Text(RawText.load(ldr, DEFAULT_BINARY_FILE_THRESHOLD))
            } catch (ex: BinaryBlobException) {
                if (isImage(entry)) {
                    generateImageBinary(ldr, entry, side)
                } else
                    EntryContent.Binary
            }
        }
    }

    private fun generateImageBinary(
        ldr: ObjectLoader,
        entry: DiffEntry,
        side: DiffEntry.Side
    ): EntryContent.ImageBinary {
        val tempDir = tempFilesManager.tempDir

        val tempFile = createTempFile(tempDir, prefix = "${entry.newPath.replace(systemSeparator, "_")}_${side.name}")
        tempFile.toFile().deleteOnExit()

        val out = FileOutputStream(tempFile.toFile())
        out.use {
            ldr.copyTo(out)
        }

        return EntryContent.ImageBinary(tempFile, Files.probeContentType(Path.of(entry.newPath)).orEmpty())
    }

    private fun isImage(entry: DiffEntry): Boolean {
        val path = entry.newPath
        val contentType = Files.probeContentType(Path.of(path))

        return contentType?.startsWith(IMAGE_CONTENT_TYPE) ?: false
    }
}

sealed class EntryContent {
    object Missing : EntryContent()
    object InvalidObjectBlob : EntryContent()
    data class Text(val rawText: RawText) : EntryContent()
    sealed class BinaryContent : EntryContent()
    data class ImageBinary(val tempFilePath: Path, val contentType: String) : BinaryContent()
    object Binary : BinaryContent()
    object TooLargeEntry : EntryContent()
}