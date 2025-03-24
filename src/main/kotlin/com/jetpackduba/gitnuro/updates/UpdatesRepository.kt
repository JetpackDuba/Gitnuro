package com.jetpackduba.gitnuro.updates

import com.jetpackduba.gitnuro.AppConstants
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.minutes

private val updateJson = Json {
    this.ignoreUnknownKeys = true
}

@Singleton
class UpdatesRepository @Inject constructor(
    private val updatesWebService: UpdatesService,
) {
    val hasUpdatesFlow = flow {
        val latestReleaseJson = updatesWebService.release(AppConstants.VERSION_CHECK_URL)

        while (coroutineContext.isActive) {
            val update = updateJson.decodeFromString<Update?>(latestReleaseJson)

            if (update != null && update.appCode > AppConstants.APP_VERSION_CODE) {
                emit(update)
            }

            delay(5.minutes)
        }
    }
}
