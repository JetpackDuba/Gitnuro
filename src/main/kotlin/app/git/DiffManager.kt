package app.git

import app.di.HunkDiffGeneratorFactory
import app.di.RawFileManagerFactory
import app.exceptions.MissingDiffEntryException
import app.extensions.fullData
import app.extensions.isMerging
import app.git.diff.DiffResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.EmptyTreeIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.filter.PathFilter
import java.io.ByteArrayOutputStream
import javax.inject.Inject


class DiffManager @Inject constructor(
    private val rawFileManagerFactory: RawFileManagerFactory,
    private val hunkDiffGeneratorFactory: HunkDiffGeneratorFactory,
    private val branchesManager: BranchesManager,
    private val repositoryManager: RepositoryManager,
) {
    suspend fun diffFormat(git: Git, diffEntryType: DiffEntryType): DiffResult = withContext(Dispatchers.IO) {
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
                    val statusEntry = diffEntryType.statusEntry
                    val cached = diffEntryType is DiffEntryType.StagedDiff
                    val firstDiffEntry = git.diff()
                        .setPathFilter(PathFilter.create(statusEntry.filePath))
                        .setCached(cached).apply {
                            val repositoryState = repositoryManager.getRepositoryState(git)
                            if (
                                branchesManager.currentBranchRef(git) == null &&
                                !repositoryState.isMerging &&
                                !repositoryState.isRebasing &&
                                cached
                            ) {
                                setOldTree(EmptyTreeIterator()) // Required if the repository is empty
                            }
                        }
                        .call()
                        .firstOrNull()
                        ?: throw MissingDiffEntryException("Diff entry not found")

                    firstDiffEntry
                }
            }

            formatter.format(diffEntry)

            formatter.flush()
        }

        val rawFileManager = rawFileManagerFactory.create(repository)
        val hunkDiffGenerator = hunkDiffGeneratorFactory.create(repository, rawFileManager)

        var diffResult: DiffResult

        hunkDiffGenerator.use {
            if (diffEntryType is DiffEntryType.UnstagedDiff) {
                val oldTree = DirCacheIterator(repository.readDirCache())
                val newTree = FileTreeIterator(repository)
                hunkDiffGenerator.scan(oldTree, newTree)
            }

            diffResult = hunkDiffGenerator.format(diffEntry)
        }

        return@withContext diffResult
    }

    suspend fun commitDiffEntries(git: Git, commit: RevCommit): List<DiffEntry> = withContext(Dispatchers.IO) {
        val fullCommit = commit.fullData(git.repository) ?: return@withContext emptyList()

        val repository = git.repository

        val parent = if (fullCommit.parentCount == 0) {
            null
        } else
            fullCommit.parents.first().fullData(git.repository)

        val oldTreeParser = if (parent != null)
            prepareTreeParser(repository, parent)
        else {
            CanonicalTreeParser()
        }

        val newTreeParser = prepareTreeParser(repository, fullCommit)

        return@withContext git.diff()
            .setNewTree(newTreeParser)
            .setOldTree(oldTreeParser)
            .call()
    }
}

fun prepareTreeParser(repository: Repository, commit: RevCommit): AbstractTreeIterator {
    // from the commit we can build the tree which allows us to construct the TreeParser
    RevWalk(repository).use { walk ->
        val tree: RevTree = walk.parseTree(commit.tree.id)
        val treeParser = CanonicalTreeParser()
        repository.newObjectReader().use { reader -> treeParser.reset(reader, tree.id) }

        return treeParser
    }
}