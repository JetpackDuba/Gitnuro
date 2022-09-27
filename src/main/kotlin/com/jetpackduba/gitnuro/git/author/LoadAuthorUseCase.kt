package com.jetpackduba.gitnuro.git.author

import com.jetpackduba.gitnuro.models.AuthorInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.storage.file.FileBasedConfig
import javax.inject.Inject

class LoadAuthorUseCase @Inject constructor() {
    suspend operator fun invoke(git: Git): AuthorInfo = withContext(Dispatchers.IO) {
        val config = git.repository.config
        val globalConfig = git.repository.config.baseConfig

        val repoConfig = FileBasedConfig((config as FileBasedConfig).file, git.repository.fs)
        repoConfig.load()

        val globalName = globalConfig.getString("user", null, "name")
        val globalEmail = globalConfig.getString("user", null, "email")

        val name = repoConfig.getString("user", null, "name")
        val email = repoConfig.getString("user", null, "email")

        return@withContext AuthorInfo(
            globalName,
            globalEmail,
            name,
            email,
        )
    }
}