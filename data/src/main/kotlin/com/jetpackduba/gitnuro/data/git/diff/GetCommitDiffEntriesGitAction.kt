package com.jetpackduba.gitnuro.data.git.diff

import com.jetpackduba.gitnuro.domain.extensions.filePath
import com.jetpackduba.gitnuro.domain.extensions.fullData
import com.jetpackduba.gitnuro.domain.interfaces.IGetCommitDiffEntriesGitAction
import com.jetpackduba.gitnuro.domain.models.Commit
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

class GetCommitDiffEntriesGitAction @Inject constructor() : IGetCommitDiffEntriesGitAction {
    override suspend operator fun invoke(git: Git, commit: Commit): List<DiffEntry> = withContext(Dispatchers.IO) {
        val repository = git.repository

        val base = repository
            .resolve(commit.hash) ?: throw Exception("Commit ${commit.hash} not found")

        val fullCommit = git.repository.parseCommit(base) ?: return@withContext emptyList()

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