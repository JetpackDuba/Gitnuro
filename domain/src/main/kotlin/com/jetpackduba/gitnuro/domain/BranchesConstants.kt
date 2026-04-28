package com.jetpackduba.gitnuro.domain

object BranchesConstants {
    /**
     * Prefix added before the upstream branch name in .git/config
     */
    const val UPSTREAM_BRANCH_CONFIG_PREFIX = "refs/heads/"

    // Remotes can have slashes in the name, but we won't care about it, known issue
    const val REMOTE_PREFIX_LENGTH = 3
    const val LOCAL_PREFIX_LENGTH = 2
}