package com.jetpackduba.gitnuro.updates

import com.jetpackduba.gitnuro.AppConstants
import com.jetpackduba.gitnuro.di.qualifiers.AppCoroutineScope
import com.jetpackduba.gitnuro.logging.printLog
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.minutes

private val updateJson = Json {
    this.ignoreUnknownKeys = true
}

private const val TAG = "UpdatesRepository"

@Singleton
class UpdatesRepository @Inject constructor(
    private val httpClient: HttpClient,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) {
    val hasUpdatesFlow = flow {
        while (coroutineContext.isActive) {

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
    }.stateIn(appCoroutineScope, started = SharingStarted.Eagerly, null)
}
