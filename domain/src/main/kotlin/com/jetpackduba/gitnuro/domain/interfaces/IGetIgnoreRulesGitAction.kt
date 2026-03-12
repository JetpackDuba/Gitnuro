package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.ignore.FastIgnoreRule
import org.eclipse.jgit.lib.Repository

interface IGetIgnoreRulesGitAction {
    suspend operator fun invoke(repository: Repository): List<FastIgnoreRule>
}