package app

import org.eclipse.jgit.revwalk.RevCommit

class CommitNode(val revCommit: RevCommit) {
    private val children = mutableListOf<CommitNode>()

    fun addChild(node: CommitNode) {
        if (children.find { it.revCommit.id == node.revCommit.id } == null) {
            children.add(node)
        }
    }
}
