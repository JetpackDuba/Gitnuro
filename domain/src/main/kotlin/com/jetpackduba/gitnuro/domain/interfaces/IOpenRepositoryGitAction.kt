package com.jetpackduba.gitnuro.domain.interfaces

import com.jetpackduba.gitnuro.domain.errors.AppError
import com.jetpackduba.gitnuro.domain.errors.Either
import org.eclipse.jgit.lib.Repository
import java.io.File

interface IOpenRepositoryGitAction {
    suspend operator fun invoke(directory: String): Either<String, AppError>
}