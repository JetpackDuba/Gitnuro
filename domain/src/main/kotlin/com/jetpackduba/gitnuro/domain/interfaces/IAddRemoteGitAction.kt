package com.jetpackduba.gitnuro.domain.interfaces

interface IAddRemoteGitAction {
    suspend operator fun invoke(
        repositoryPath: String,
        remoteName: String,
        fetchUri: String,
    ): Unit
}