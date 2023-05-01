package com.jetpackduba.gitnuro.git.config

import com.jetpackduba.gitnuro.models.SignOffConfig
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileBasedConfig
import java.io.File
import javax.inject.Inject

class SaveLocalRepositoryConfigUseCase @Inject constructor() {
    operator fun invoke(
        repository: Repository,
        signOffConfig: SignOffConfig,
    ) {
        val configFile = File(repository.directory, LocalConfigConstants.CONFIG_FILE_NAME)
        configFile.createNewFile()

        val config = FileBasedConfig(configFile, repository.fs)

        config.setBoolean(
            LocalConfigConstants.SignOff.SECTION,
            null,
            LocalConfigConstants.SignOff.FIELD_ENABLED,
            signOffConfig.isEnabled
        )

        config.setString(
            LocalConfigConstants.SignOff.SECTION,
            null,
            LocalConfigConstants.SignOff.FIELD_FORMAT,
            signOffConfig.format
        )

        config.save()
    }
}