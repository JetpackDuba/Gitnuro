package com.jetpackduba.gitnuro.data.git.author

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.interfaces.ILoadAuthorGitAction
import com.jetpackduba.gitnuro.domain.models.AuthorInfo
import org.eclipse.jgit.storage.file.FileBasedConfig
import javax.inject.Inject

class LoadAuthorGitAction @Inject constructor(private val jgit: JGit) : ILoadAuthorGitAction {
    override suspend operator fun invoke(repositoryPath: String) = jgit.provide(repositoryPath) { git ->
        val config = git.repository.config
        val globalConfig = git.repository.config.baseConfig

        val repoConfig = FileBasedConfig((config as FileBasedConfig).file, git.repository.fs)
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