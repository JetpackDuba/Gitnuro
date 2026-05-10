package com.jetpackduba.gitnuro.data.git.config

import com.jetpackduba.gitnuro.data.git.JGit
import com.jetpackduba.gitnuro.domain.SignOffConstants
import com.jetpackduba.gitnuro.domain.interfaces.ISaveLocalRepositoryConfigGitAction
import com.jetpackduba.gitnuro.domain.models.SignOffConfig
import org.eclipse.jgit.storage.file.FileBasedConfig
import java.io.File
import javax.inject.Inject

class SaveLocalRepositoryConfigGitAction @Inject constructor(
    private val jgit: JGit,
) : ISaveLocalRepositoryConfigGitAction {
    override suspend operator fun invoke(
        repositoryPath: String,
        signOffConfig: SignOffConfig,
    ) = jgit.provide(repositoryPath) { git ->
        val repository = git.repository
        val configFile = File(repository.directory, LocalConfigConstants.CONFIG_FILE_NAME)
        configFile.createNewFile()

        val config = FileBasedConfig(configFile, repository.fs)

        config.setBoolean(
            SignOffConstants.SECTION,
            null,
            SignOffConstants.FIELD_ENABLED,
            signOffConfig.isEnabled
        )

        config.setString(
            SignOffConstants.SECTION,
            null,
            SignOffConstants.FIELD_FORMAT,
            signOffConfig.format
        )

        config.save()
    }
}