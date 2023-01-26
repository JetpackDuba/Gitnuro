package com.jetpackduba.gitnuro.extensions

import org.eclipse.jgit.lib.Ref

/**
 * Predicate for filtering branches
 */
typealias BranchFilter = (ref: Ref) -> Boolean

fun Ref.matchesAll(filters: List<BranchFilter>): Boolean {
    return filters.all { filter -> filter(this) }
}

/**
 * Matches only branches from the specified remote name.
 */
class OriginFilter(
    private val remoteName: String
) : BranchFilter {
    override fun invoke(ref: Ref): Boolean {
        return ref.name.startsWith("refs/remotes/$remoteName")
    }
}

/**
 * Matches only branches that contain a specific keyword in its name.
 */
class BranchNameContainsFilter(
    private val keyword: String
) : BranchFilter {
    override fun invoke(ref: Ref): Boolean {
        return ref.name.contains(keyword)
    }
}