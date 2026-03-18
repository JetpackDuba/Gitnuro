package com.jetpackduba.gitnuro.domain.models

data class GraphCommit(
    val lane: Int,
    val commit: Commit,
    val branches: List<Branch>,
    val tags: List<Tag>,
) {
    val hash get() = commit.hash
    val message get() = commit.message
    val committer get() = commit.committer
    val author get() = commit.author
    val date get() = commit.date
}