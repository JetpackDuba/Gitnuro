package com.jetpackduba.gitnuro.git.config

import com.jetpackduba.gitnuro.extensions.nullIfEmpty
import com.jetpackduba.gitnuro.models.SignOffConfig
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileBasedConfig
import java.io.File
import javax.inject.Inject


class LoadSignOffConfigUseCase @Inject constructor() {
    operator fun invoke(repository: Repository): SignOffConfig {
        val configFile = File(repository.directory, LocalConfigConstants.CONFIG_FILE_NAME)
        configFile.createNewFile()

        val config = FileBasedConfig(configFile, repository.fs)
        config.load()

        val enabled = config.getBoolean(
            LocalConfigConstants.SignOff.SECTION,
            null,
            LocalConfigConstants.SignOff.FIELD_ENABLED,
            LocalConfigConstants.SignOff.DEFAULT_SIGN_OFF_ENABLED
        )


        val format = config.getString(
            LocalConfigConstants.SignOff.SECTION,
            null,
            LocalConfigConstants.SignOff.FIELD_FORMAT
        )?.nullIfEmpty ?: LocalConfigConstants.SignOff.DEFAULT_SIGN_OFF_FORMAT

        return SignOffConfig(enabled, format)
    }
}
