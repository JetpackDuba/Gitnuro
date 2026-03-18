package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.interfaces.IGetFileCommitsAction
import org.eclipse.jgit.api.Git
import javax.inject.Inject

class GetFileCommitsUseCase @Inject constructor(
    private val getFileCommitsAction: IGetFileCommitsAction,
) {
    suspend operator fun invoke(git: Git, filePath: String) = getFileCommitsAction(git, filePath)
}