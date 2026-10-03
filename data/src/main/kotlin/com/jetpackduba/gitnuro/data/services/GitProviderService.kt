package com.jetpackduba.gitnuro.data.services

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.services.IGitProviderService
import javax.inject.Inject

class GitProviderService @Inject constructor(
    private val jgit: JGit,
) : IGitProviderService {
    override fun cleanupExcept(repositoriesToKeep: Set<String>) {
        jgit.cleanupExcept(repositoriesToKeep.map { "$it/.git" }.toSet())
    }
}