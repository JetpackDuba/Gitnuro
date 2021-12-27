package app.git

import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import org.eclipse.jgit.diff.ContentSource
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.RawText
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.pack.PackConfig
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.WorkingTreeIterator
import org.eclipse.jgit.util.LfsFactory

private const val DEFAULT_BINARY_FILE_THRESHOLD = PackConfig.DEFAULT_BIG_FILE_THRESHOLD

class RawFileManager @AssistedInject constructor(
    @Assisted private val repository: Repository
) : AutoCloseable {
    private var reader: ObjectReader = repository.newObjectReader()
    private var source: ContentSource.Pair

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

    fun getRawContent(side: DiffEntry.Side, entry: DiffEntry): RawText {
        if (entry.getMode(side) === FileMode.MISSING) return RawText.EMPTY_TEXT
        if (entry.getMode(side).objectType != Constants.OBJ_BLOB) return RawText.EMPTY_TEXT

        val ldr = LfsFactory.getInstance().applySmudgeFilter(
            repository,
            source.open(side, entry), entry.diffAttribute
        )
        return RawText.load(ldr, DEFAULT_BINARY_FILE_THRESHOLD)
    }

    override fun close() {
        reader.close()
    }
}