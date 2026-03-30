package com.jetpackduba.gitnuro.data.git.author

import com.jetpackduba.gitnuro.data.git.jgit
import com.jetpackduba.gitnuro.domain.interfaces.ILoadAuthorGitAction
import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileBasedConfig
import javax.inject.Inject

class LoadAuthorGitAction @Inject constructor() : ILoadAuthorGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit(repositoryPath) {
        val config = repository.config
        val globalConfig = repository.config.baseConfig

        val repoConfig = FileBasedConfig((config as FileBasedConfig).file, repository.fs)
        repoConfig.load()

        val globalName = globalConfig.getString("user", null, "name")
        val globalEmail = globalConfig.getString("user", null, "email")

        val name = repoConfig.getString("user", null, "name")
        val email = repoConfig.getString("user", null, "email")

        AuthorInfo(
            globalName,
            globalEmail,
            name,
            email,
        )
    }
}