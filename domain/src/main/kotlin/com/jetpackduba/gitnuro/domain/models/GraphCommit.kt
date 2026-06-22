package com.jetpackduba.gitnuro.domain.models

data class GraphCommit(
    val commit: Commit,
    val lane: Int,
    val passingLanes: List<Int>,
    val mergingLanes: List<Int>,
    val forkingOffLanes: List<Int>,
    val childCount: Int,
) {
    val hash get() = commit.hash
    val message get() = commit.message
    val committer get() = commit.committer
    val author get() = commit.author
    val date get() = commit.date
}