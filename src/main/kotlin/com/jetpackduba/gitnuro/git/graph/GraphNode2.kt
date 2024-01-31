package com.jetpackduba.gitnuro.git.graph

import org.eclipse.jgit.lib.AnyObjectId
import org.eclipse.jgit.lib.PersonIdent
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.revwalk.RevCommit


data class GraphNode2(
    val name: String,
    val message: String,
    val fullMessage: String,
    val authorIdent: PersonIdent,
    val committerIdent: PersonIdent,
    val parentCount: Int,
    val isStash: Boolean,
    val lane: Int,
    val passingLanes: List<Int>,
    val forkingLanes: List<Int>,
    val mergingLanes: List<Int>,
)