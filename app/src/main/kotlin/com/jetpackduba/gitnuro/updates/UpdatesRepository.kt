package com.jetpackduba.gitnuro.updates

import com.jetpackduba.gitnuro.AppConstants
import com.jetpackduba.gitnuro.common.printLog
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

private val updateJson = Json {
    this.ignoreUnknownKeys = true
}

private const val TAG = "UpdatesRepository"

@Singleton
class UpdatesRepository @Inject constructor(
    private val httpClient: HttpClient,
) {
    val hasUpdatesFlow = flow {
        while (currentCoroutineContext().isActive) {
            printLog(TAG, "Checking for new updates in ${AppConstants.VERSION_CHECK_URL}")

            val latestReleaseJson = httpClient
                .get(AppConstants.VERSION_CHECK_URL)
                .body<String>()

            val update = updateJson.decodeFromString<Update?>(latestReleaseJson)

            if (update != null && update.appCode > AppConstants.APP_VERSION_CODE) {
                emit(update)
            }

            delay(5.minutes)
        }
    }
}
