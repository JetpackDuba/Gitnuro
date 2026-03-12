package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RebaseTodoLine

interface IGetRebaseLinesFullMessageGitAction {
    suspend operator fun invoke(
        git: Git,
        rebaseTodoLines: List<RebaseTodoLine>,
    ): Map<String, String>
}