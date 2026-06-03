package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.domain.UseCaseExecutor
import com.jetpackduba.gitnuro.domain.interfaces.IGetFileCommitsAction
import javax.inject.Inject

class GetFileCommitsUseCase @Inject constructor(
    private val getFileCommitsAction: IGetFileCommitsAction,
    private val useCaseExecutor: UseCaseExecutor,
) {
    suspend operator fun invoke(filePath: String) = useCaseExecutor.execute(
    ) { repositoryPath ->
        getFileCommitsAction(repositoryPath, filePath)
    }
}