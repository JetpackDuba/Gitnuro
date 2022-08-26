package app.git.workspace

import org.eclipse.jgit.dircache.DirCacheEditor
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.FileMode
import org.eclipse.jgit.lib.ObjectInserter
import org.eclipse.jgit.lib.Repository
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.time.Instant

class HunkEdit(
    path: String?,
    private val repo: Repository,
    private val content: ByteBuffer,
) : DirCacheEditor.PathEdit(path) {
    override fun apply(ent: DirCacheEntry) {
        val inserter: ObjectInserter = repo.newObjectInserter()
        if (ent.rawMode and FileMode.TYPE_MASK != FileMode.TYPE_FILE) {
            ent.fileMode = FileMode.REGULAR_FILE
        }
        ent.length = content.limit()
        ent.setLastModified(Instant.now())
        try {
            val `in` = ByteArrayInputStream(
                content.array(), 0, content.limit()
            )
            ent.setObjectId(
                inserter.insert(
                    Constants.OBJ_BLOB, content.limit().toLong(),
                    `in`
                )
            )
            inserter.flush()
        } catch (ex: IOException) {
            throw RuntimeException(ex)
        }
    }
}