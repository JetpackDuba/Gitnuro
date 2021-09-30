package git

import DiffEntryType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import java.io.ByteArrayOutputStream

class DiffManager {
    suspend fun diffFormat(git: Git, diffEntryType: DiffEntryType): List<String> = withContext(Dispatchers.IO) {
        val diffEntry = diffEntryType.diffEntry
        val byteArrayOutputStream = ByteArrayOutputStream()

        DiffFormatter(byteArrayOutputStream).use { formatter ->
            val repo = git.repository
            formatter.setRepository(repo)

            val oldTree = DirCacheIterator(repo.readDirCache())
            val newTree = FileTreeIterator(repo)

            if (diffEntryType is DiffEntryType.UnstagedDiff)
                formatter.scan(oldTree, newTree)

            formatter.format(diffEntry)
            formatter.flush()
        }

        val diff = byteArrayOutputStream.toString(Charsets.UTF_8)

        // TODO This is just a workaround, try to find properly which lines have to be displayed by using a custom diff

        return@withContext diff.split("\n", "\r\n").filterNot {
            it.startsWith("diff --git")
        }

    }
}