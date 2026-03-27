package com.jetpackduba.gitnuro.git.diff

import com.jetpackduba.gitnuro.extensions.filePath
import com.jetpackduba.gitnuro.extensions.fullData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffEntry
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevTree
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import javax.inject.Inject

class GetCommitDiffEntriesUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git, commit: RevCommit): List<DiffEntry> = withContext(Dispatchers.IO) {
        val fullCommit = commit.fullData(git.repository) ?: return@withContext emptyList()

        val parent = if (fullCommit.parentCount == 0) {
            null
        } else {
            fullCommit.parents.first().fullData(git.repository)
        }

        return@withContext getDiffEntriesBetweenCommits(git, oldCommit = parent, newCommit = fullCommit)
    }

    suspend fun getDiffEntriesBetweenCommits(
        git: Git,
        oldCommit: RevCommit?,
        newCommit: RevCommit,
    ): List<DiffEntry> = withContext(Dispatchers.IO) {
        val repository = git.repository

        val oldTreeParser = if (oldCommit != null) {
            prepareTreeParser(repository, oldCommit)
        } else {
            CanonicalTreeParser()
        }

        val newTreeParser = prepareTreeParser(repository, newCommit)

        return@withContext git.diff()
            .setNewTree(newTreeParser)
            .setOldTree(oldTreeParser)
            .call()
            .sortedBy { it.filePath }
    }
}

private fun prepareTreeParser(repository: Repository, commit: RevCommit): AbstractTreeIterator {
    // from the commit we can build the tree which allows us to construct the TreeParser
    RevWalk(repository).use { walk ->
        val tree: RevTree = walk.parseTree(commit.tree.id)
        val treeParser = CanonicalTreeParser()
        repository.newObjectReader().use { reader -> treeParser.reset(reader, tree.id) }

        return treeParser
    }
}
