package com.jetpackduba.gitnuro.data.git.workspace

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.errors.Either
import com.jetpackduba.gitnuro.domain.errors.GenericError
import com.jetpackduba.gitnuro.domain.errors.GitError
import com.jetpackduba.gitnuro.domain.errors.raiseError
import com.jetpackduba.gitnuro.domain.interfaces.IDeleteFileGitAction
import com.jetpackduba.gitnuro.domain.interfaces.IDiscardEntriesGitAction
import com.jetpackduba.gitnuro.domain.models.StatusEntry
import com.jetpackduba.gitnuro.domain.models.StatusType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import java.io.File
import javax.inject.Inject

class DeleteFileGitAction @Inject constructor(
    private val jgit: JGit,
) : IDeleteFileGitAction {
    override suspend fun invoke(
        repositoryPath: String,
        filePath: String
    ) = jgit.provide(repositoryPath) { git ->
        val fileToDelete = File(git.repository.workTree, filePath)

        if (!fileToDelete.deleteRecursively()) {
            raiseError(GenericError("Delete file recursively failed"))
        }
    }
}