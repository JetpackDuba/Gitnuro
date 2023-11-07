package com.jetpackduba.gitnuro.preferences

import com.jetpackduba.gitnuro.di.TabScope
import com.jetpackduba.gitnuro.logging.printError
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.jgit.lib.Repository
import java.io.File
import javax.inject.Inject

private const val TAG = "RepositorySettings"
private const val GITNURO_CONFIG_FILE = "GITNURO_CONFIG"

class RepositorySettings @Inject constructor() {
    fun getGraphWidth(repository: Repository): Float {
//        return getConfig(repository)?.graphWidth ?: 0F // TODO use constants
        return 0F
    }

    fun setGraphWidth(repository: Repository, newValue: Float) {
        val config = getConfig(repository) ?: RepositoryConfig(0F)
        val newConfig = config.copy(graphWidth = newValue)

        setConfig(repository, newConfig)
    }

    private fun getConfig(repository: Repository): RepositoryConfig? {
        if(repository.directory == null || !repository.directory.exists()) {
            return null
        }

        val configFile = File(repository.directory, GITNURO_CONFIG_FILE)

        if (!configFile.exists()) {
            return null
        }

        val content = configFile.readText()

        return try {
            Json.decodeFromString<RepositoryConfig>(content)
        } catch (e: Exception) {
            printError(TAG, "Error parsing repository config: ${e.message}", e)
            null
        }
    }

    private fun setConfig(repository: Repository, newConfig: RepositoryConfig) {
        if(repository.directory == null || !repository.directory.exists()) {
            return
        }

        val configFile = File(repository.directory, GITNURO_CONFIG_FILE)

        if (!configFile.exists()) {
            configFile.createNewFile()
        }

        configFile.writeText(Json.encodeToString(newConfig))
    }
}

@Serializable
data class RepositoryConfig(
    val graphWidth: Float
)