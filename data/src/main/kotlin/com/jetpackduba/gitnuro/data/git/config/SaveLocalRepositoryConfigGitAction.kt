package com.jetpackduba.gitnuro.data.git.config

import com.jetpackduba.gitnuro.domain.SignOffConstants
import com.jetpackduba.gitnuro.domain.interfaces.ISaveLocalRepositoryConfigGitAction
import com.jetpackduba.gitnuro.domain.models.SignOffConfig
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileBasedConfig
import java.io.File
import javax.inject.Inject

class SaveLocalRepositoryConfigGitAction @Inject constructor() : ISaveLocalRepositoryConfigGitAction {
    override operator fun invoke(
        repository: Repository,
        signOffConfig: SignOffConfig,
    ) {
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