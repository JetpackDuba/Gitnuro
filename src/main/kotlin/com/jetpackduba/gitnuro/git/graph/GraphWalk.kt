package com.jetpackduba.gitnuro.git.graph

import org.eclipse.jgit.errors.MissingObjectException
import org.eclipse.jgit.internal.JGitText
import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.Constants
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.revwalk.*
import java.io.IOException

/**
 * Specialized RevWalk to be used for load data in a structured way to be displayed in a graph of commits.
 */
class GraphWalk(private var repository: Repository?) : RevWalk(repository) {
    private var additionalRefMap: MutableMap<AnyObjectId, Set<Ref>>? = HashMap()
    private var reverseRefMap: MutableMap<AnyObjectId, Set<Ref>>? = null
    private var stashes = emptySet<AnyObjectId>()

    init {
        super.sort(RevSort.TOPO, true)
    }

    override fun dispose() {
        super.dispose()
        if (reverseRefMap != null) {
            reverseRefMap?.clear()
            reverseRefMap = null
        }
        if (additionalRefMap != null) {
            additionalRefMap?.clear()
            additionalRefMap = null
        }
        repository = null
    }

    override fun sort(revSort: RevSort, use: Boolean) {
        require(!(revSort == RevSort.TOPO && !use)) {
            JGitText.get().topologicalSortRequired
        }

        super.sort(revSort, use)
    }

    override fun createCommit(id: AnyObjectId): RevCommit {
        return GraphNode(id)
    }

    override fun next(): GraphNode? {
        val graphNode = super.next() as GraphNode?

        if (graphNode != null) {
            val refs = getRefs(graphNode)
            graphNode.refs = refs

            graphNode.isStash = stashes.contains(graphNode)
        }

        return graphNode

    }

    private fun getRefs(commitId: AnyObjectId): List<Ref> {
        val repository = this.repository
        var reverseRefMap = this.reverseRefMap
        var additionalRefMap = this.additionalRefMap
        if (reverseRefMap == null && repository != null && additionalRefMap != null) {

            reverseRefMap = repository.allRefsByPeeledObjectId
            this.reverseRefMap = reverseRefMap

            for (entry in additionalRefMap.entries) {
                val refsSet = reverseRefMap[entry.key]
                var additional = entry.value.toMutableSet()

                if (refsSet != null) {
                    if (additional.size == 1) {
                        // It's an unmodifiable singleton set...
                        additional = HashSet(additional)
                    }
                    additional.addAll(refsSet)
                }
                reverseRefMap[entry.key] = additional
            }

            additionalRefMap.clear()
            additionalRefMap = null

            this.additionalRefMap = additionalRefMap
        }

        requireNotNull(reverseRefMap) // This should never be null

        val refsSet = reverseRefMap[commitId]
            ?: return NO_REFS
        val tags = refsSet.toList()

        tags.sortedWith(GraphRefComparator())

        return tags
    }

    fun markStartAllRefs(prefix: String) {
        repository?.let { repo ->
            for (ref in repo.refDatabase.getRefsByPrefix(prefix)) {
                if (ref.isSymbolic) continue
                markStartObjectId(ref.leaf.objectId)
            }
        }
    }

    private fun markStartObjectId(objectId: AnyObjectId) {
        try {
            val refTarget = parseAny(objectId)

            when (refTarget) {
                is RevCommit -> markStart(refTarget)
                // RevTag case handles commits without branches but only tags.
                is RevTag -> {
                    if (refTarget.`object` is RevCommit) {
                        val commit = lookupCommit(refTarget.`object`)
                        markStart(commit)
                    } else {
                        println("Tag ${refTarget.tagName} is pointing to ${refTarget.`object`::class.simpleName}")
                    }
                }
            }
        } catch (e: MissingObjectException) {
            // Ignore missing Refs
        }
    }

    fun markStartStashes(stashList: List<RevCommit>) {
        for (stash in stashList) {
            markStartObjectId(stash)
        }

        this.stashes = stashList
            .asSequence()
            .map { it.toObjectId() }
            .toSet()
    }

    internal inner class GraphRefComparator : Comparator<Ref> {
        override fun compare(o1: Ref, o2: Ref): Int {
            try {
                val obj1 = parseAny(o1.objectId)
                val obj2 = parseAny(o2.objectId)
                val t1 = timeOf(obj1)
                val t2 = timeOf(obj2)
                if (t1 > t2) return -1
                if (t1 < t2) return 1
            } catch (e: IOException) {
                // ignore
            }

            var cmp = kind(o1) - kind(o2)

            if (cmp == 0)
                cmp = o1.name.compareTo(o2.name)

            return cmp
        }

        private fun timeOf(revObject: RevObject): Long {
            if (revObject is RevCommit) return revObject.commitTime.toLong()
            if (revObject is RevTag) {
                try {
                    parseBody(revObject)
                } catch (e: IOException) {
                    return 0
                }
                val who = revObject.taggerIdent
                return who?.whenAsInstant?.toEpochMilli() ?: 0
            }
            return 0
        }

        private fun kind(r: Ref): Int {
            if (r.name.startsWith(Constants.R_TAGS)) return 0
            if (r.name.startsWith(Constants.R_HEADS)) return 1
            return if (r.name.startsWith(Constants.R_REMOTES)) 2 else 3
        }
    }
}