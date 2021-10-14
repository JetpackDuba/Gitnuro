package app.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.diff.EditList
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.patch.HunkHeader
import org.eclipse.jgit.revplot.PlotCommit
import org.eclipse.jgit.revplot.PlotCommitList
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class DiffManager @Inject constructor() {
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

        val containsWindowsNewLine = diff.contains("\r\n")

        return@withContext diff.split("\n", "\r\n").filterNot {
            it.startsWith("diff --git")
        }.map {
            if (containsWindowsNewLine)
                "$it\r\n"
            else
                "$it\n"
        }
    }



    suspend fun commitDiffEntries(git: Git, commit: RevCommit): List<DiffEntry> = withContext(Dispatchers.IO) {
        val repository = git.repository
        val parent = if (commit.parentCount == 0) {
            null
        } else
            commit.parents.first()

        val oldTreeParser = if (parent != null)
            prepareTreeParser(repository, parent)
        else {
            CanonicalTreeParser()
        }

        val newTreeParser = prepareTreeParser(repository, commit)

        return@withContext git.diff()
            .setNewTree(newTreeParser)
            .setOldTree(oldTreeParser)
            .call()
    }
}

fun prepareTreeParser(repository: Repository, commit: RevCommit): AbstractTreeIterator? {
    // from the commit we can build the tree which allows us to construct the TreeParser
    RevWalk(repository).use { walk ->
        val tree: RevTree = walk.parseTree(commit.tree.id)
        val treeParser = CanonicalTreeParser()
        repository.newObjectReader().use { reader -> treeParser.reset(reader, tree.id) }
        walk.dispose()
        return treeParser
    }
}