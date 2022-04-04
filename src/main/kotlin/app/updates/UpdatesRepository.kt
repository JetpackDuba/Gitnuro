package app.updates

import app.AppConstants
import app.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject

private val updateJson = Json {
    this.ignoreUnknownKeys = true
}

class UpdatesRepository @Inject constructor(
    private val updatesWebService: UpdatesService,
) {
    suspend fun latestRelease(): Update? = withContext(Dispatchers.IO) {
        val latestReleaseJson = updatesWebService.release(AppConstants.VERSION_CHECK_URL)

        updateJson.decodeFromString(latestReleaseJson)
    }
}
