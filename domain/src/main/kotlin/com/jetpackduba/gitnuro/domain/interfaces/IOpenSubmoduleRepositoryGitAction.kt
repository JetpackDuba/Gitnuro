package com.jetpackduba.gitnuro.domain.interfaces

import org.eclipse.jgit.lib.Repository
import java.io.File

interface IOpenSubmoduleRepositoryGitAction {
    suspend operator fun invoke(directory: File): Repository
}