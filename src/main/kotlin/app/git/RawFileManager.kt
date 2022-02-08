package app.git

import app.TempFilesManager
import app.extensions.systemSeparator
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
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
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createTempFile


private const val DEFAULT_BINARY_FILE_THRESHOLD = PackConfig.DEFAULT_BIG_FILE_THRESHOLD

class RawFileManager @AssistedInject constructor(
    @Assisted private val repository: Repository,
    private val tempFilesManager: TempFilesManager,
) : AutoCloseable {
    private var reader: ObjectReader = repository.newObjectReader()
    private var source: ContentSource.Pair

    private val imageFormatsSupported = listOf(
        "png",
        "jpg",
        "jpeg",
        "webp",
    )

    init {
        val cs = ContentSource.create(reader)
        source = ContentSource.Pair(cs, cs)
    }

    fun scan(oldTreeIterator: AbstractTreeIterator, newTreeIterator: AbstractTreeIterator) {
        source = ContentSource.Pair(source(oldTreeIterator), source(newTreeIterator))
    }

    private fun source(iterator: AbstractTreeIterator): ContentSource {
        return if (iterator is WorkingTreeIterator)
            ContentSource.create(iterator)
        else
            ContentSource.create(reader)
    }

    fun getRawContent(side: DiffEntry.Side, entry: DiffEntry): EntryContent {
        if (entry.getMode(side) === FileMode.MISSING) return EntryContent.Missing
        if (entry.getMode(side).objectType != Constants.OBJ_BLOB) return EntryContent.InvalidObjectBlob

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

        return EntryContent.ImageBinary(tempFile)
    }

    // todo check if it's an image checking the binary format, checking the extension is a temporary workaround
    private fun isImage(entry: DiffEntry): Boolean {
        val path = entry.newPath
        val fileExtension = path.split(".").lastOrNull() ?: return false

        return imageFormatsSupported.contains(fileExtension.lowercase())
    }

    override fun close() {
        reader.close()
    }
}

sealed class EntryContent {
    object Missing : EntryContent()
    object InvalidObjectBlob : EntryContent()
    data class Text(val rawText: RawText) : EntryContent()
    sealed class BinaryContent() : EntryContent()
    data class ImageBinary(val tempFilePath: Path) : BinaryContent()
    object Binary : BinaryContent()
    object TooLargeEntry : EntryContent()
}