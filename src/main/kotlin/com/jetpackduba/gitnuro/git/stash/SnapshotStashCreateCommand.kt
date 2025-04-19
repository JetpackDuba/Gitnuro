package com.jetpackduba.gitnuro.git.stash

import org.eclipse.jgit.api.GitCommand
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.api.errors.JGitInternalException
import org.eclipse.jgit.api.errors.NoHeadException
import org.eclipse.jgit.api.errors.UnmergedPathsException
import org.eclipse.jgit.dircache.DirCache
import org.eclipse.jgit.dircache.DirCacheEditor
import org.eclipse.jgit.dircache.DirCacheEntry
import org.eclipse.jgit.dircache.DirCacheIterator
import org.eclipse.jgit.errors.UnmergedPathException
import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.*
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.treewalk.TreeWalk
import org.eclipse.jgit.treewalk.WorkingTreeIterator
import org.eclipse.jgit.treewalk.filter.AndTreeFilter
import org.eclipse.jgit.treewalk.filter.IndexDiffFilter
import org.eclipse.jgit.treewalk.filter.SkipWorkTreeFilter
import java.io.IOException
import java.text.MessageFormat

private const val MSG_INDEX = "index on {0}: {1} {2}"
private const val MSG_UNTRACKED = "untracked files on {0}: {1} {2}"
private const val MSG_WORKING_DIR = "Snapshot WIP on {0}: {1} {2}"

class SnapshotStashCreateCommand(
    repository: Repository,
    private val indexMessage: String = MSG_INDEX,
    private val workingDirectoryMessage: String = MSG_WORKING_DIR,
    private val ref: String = Constants.R_STASH,
    private val person: PersonIdent = PersonIdent(repository),
    private val includeUntracked: Boolean = false,
) : GitCommand<RevCommit?>(repository) {
    @Throws(IOException::class)
    private fun parseCommit(
        reader: ObjectReader,
        headId: ObjectId,
    ): RevCommit {
        RevWalk(reader).use { walk ->
            return walk.parseCommit(headId)
        }
    }

    private fun createBuilder(): CommitBuilder {
        val builder = CommitBuilder()
        builder.author = person
        builder.committer = person
        return builder
    }

    @Throws(IOException::class)
    private fun updateStashRef(
        commitId: ObjectId,
        refLogIdent: PersonIdent,
        refLogMessage: String,
    ) {
        val currentRef = repo.findRef(ref)
        val refUpdate = repo.updateRef(ref)
        refUpdate.setNewObjectId(commitId)
        refUpdate.refLogIdent = refLogIdent
        refUpdate.setRefLogMessage(refLogMessage, false)
        refUpdate.setForceRefLog(true)
        if (currentRef != null) refUpdate.setExpectedOldObjectId(currentRef.objectId)
        else refUpdate.setExpectedOldObjectId(ObjectId.zeroId())
        refUpdate.forceUpdate()
    }

    @get:Throws(GitAPIException::class)
    private val head: Ref
        get() = try {
            val head = repo.exactRef(Constants.HEAD)
            if (head == null || head.objectId == null) throw NoHeadException(JGitText.get().headRequiredToStash)
            head
        } catch (e: IOException) {
            throw JGitInternalException(JGitText.get().stashFailed, e)
        }

    @Throws(GitAPIException::class)
    override fun call(): RevCommit? {
        checkCallable()

        val head = head
        try {
            repo.newObjectReader().use { reader ->
                val headCommit = parseCommit(reader, head.objectId)
                val cache = repo.lockDirCache()

                var commitId: ObjectId
                try {
                    repo.newObjectInserter().use { inserter ->
                        TreeWalk(repo, reader).use { treeWalk ->
                            treeWalk.isRecursive = true
                            treeWalk.addTree(headCommit.tree)
                            treeWalk.addTree(DirCacheIterator(cache))
                            treeWalk.addTree(FileTreeIterator(repo))
                            treeWalk.getTree(2, FileTreeIterator::class.java)
                                .setDirCacheIterator(treeWalk, 1)
                            treeWalk.filter = AndTreeFilter.create(
                                SkipWorkTreeFilter(
                                    1
                                ), IndexDiffFilter(1, 2)
                            )

                            // Return null if no local changes to stash
                            if (!treeWalk.next()) return null

                            val id = MutableObjectId()
                            val wtEdits: MutableList<DirCacheEditor.PathEdit> = ArrayList()
                            val wtDeletes: MutableList<String> = ArrayList()
                            val untracked: MutableList<DirCacheEntry> = ArrayList()
                            var hasChanges: Boolean = false
                            do {
                                val headIter = treeWalk.getTree(0, AbstractTreeIterator::class.java)
                                val indexIterator = treeWalk.getTree(1, DirCacheIterator::class.java)
                                val workingTreeIterator = treeWalk.getTree(2, WorkingTreeIterator::class.java)

                                if (
                                    indexIterator != null &&
                                    !indexIterator.dirCacheEntry.isMerged
                                ) {
                                    throw UnmergedPathsException(UnmergedPathException(indexIterator.dirCacheEntry))
                                }

                                if (workingTreeIterator != null) {
                                    if (indexIterator == null && headIter == null && !includeUntracked) continue
                                    hasChanges = true
                                    if (indexIterator != null && workingTreeIterator.idEqual(indexIterator)) continue
                                    if (headIter != null && workingTreeIterator.idEqual(headIter)) continue
                                    treeWalk.getObjectId(id, 0)
                                    val entry = DirCacheEntry(
                                        treeWalk.rawPath
                                    )
                                    entry.setLength(workingTreeIterator.entryLength)
                                    entry.setLastModified(
                                        workingTreeIterator.entryLastModifiedInstant
                                    )
                                    entry.fileMode = workingTreeIterator.entryFileMode
                                    val contentLength = workingTreeIterator.entryContentLength
                                    workingTreeIterator.openEntryStream().use { `in` ->
                                        entry.setObjectId(
                                            inserter.insert(
                                                Constants.OBJ_BLOB, contentLength, `in`
                                            )
                                        )
                                    }
                                    if (indexIterator == null && headIter == null) untracked.add(entry)
                                    else wtEdits.add(object : DirCacheEditor.PathEdit(entry) {
                                        override fun apply(ent: DirCacheEntry) {
                                            ent.copyMetaData(entry)
                                        }
                                    })
                                }

                                hasChanges = true

                                if (workingTreeIterator == null && headIter != null) {
                                    wtDeletes.add(treeWalk.pathString)
                                }

                            } while (treeWalk.next())

                            if (!hasChanges) return null

                            val branch = Repository.shortenRefName(
                                head.target
                                    .name
                            )

                            // Commit index changes
                            val builder = createBuilder()
                            builder.setParentId(headCommit)
                            builder.setTreeId(cache.writeTree(inserter))
                            builder.message = MessageFormat.format(
                                indexMessage, branch,
                                headCommit.abbreviate(Constants.OBJECT_ID_ABBREV_STRING_LENGTH)
                                    .name(),
                                headCommit.shortMessage
                            )
                            val indexCommit = inserter.insert(builder)

                            // Commit untracked changes
                            var untrackedCommit: ObjectId? = null
                            if (untracked.isNotEmpty()) {
                                val untrackedDirCache = DirCache.newInCore()
                                val untrackedBuilder = untrackedDirCache
                                    .builder()
                                for (entry in untracked) untrackedBuilder.add(entry)
                                untrackedBuilder.finish()

                                builder.setParentIds(*arrayOfNulls(0))
                                builder.setTreeId(untrackedDirCache.writeTree(inserter))
                                builder.message = MessageFormat.format(
                                    MSG_UNTRACKED,
                                    branch,
                                    headCommit
                                        .abbreviate(Constants.OBJECT_ID_ABBREV_STRING_LENGTH)
                                        .name(),
                                    headCommit.shortMessage
                                )
                                untrackedCommit = inserter.insert(builder)
                            }

                            // Commit working tree changes
                            if (wtEdits.isNotEmpty() || wtDeletes.isNotEmpty()) {
                                val editor = cache.editor()
                                for (edit in wtEdits) editor.add(edit)
                                for (path in wtDeletes) editor.add(DirCacheEditor.DeletePath(path))
                                editor.finish()
                            }

                            builder.setParentId(headCommit)
                            builder.addParentId(indexCommit)

                            if (untrackedCommit != null) {
                                builder.addParentId(untrackedCommit)
                            }

                            builder.message = MessageFormat.format(
                                workingDirectoryMessage,
                                branch,
                                headCommit
                                    .abbreviate(Constants.OBJECT_ID_ABBREV_STRING_LENGTH)
                                    .name(),
                                headCommit.shortMessage
                            )

                            builder.setTreeId(cache.writeTree(inserter))
                            commitId = inserter.insert(builder)
                            inserter.flush()

                            updateStashRef(
                                commitId, builder.author,
                                builder.message
                            )
                        }
                    }
                } finally {
                    cache.unlock()
                }

                // Return snapshot commit
                return parseCommit(reader, commitId)
            }
        } catch (e: IOException) {
            throw JGitInternalException(JGitText.get().stashFailed, e)
        }
    }
}
