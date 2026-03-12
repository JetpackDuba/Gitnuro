package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.RebaseTodoLine

interface IGetRebaseInteractiveTodoLinesGitAction {
    suspend operator fun invoke(git: Git): List<RebaseTodoLine>
}