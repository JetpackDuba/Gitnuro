package com.jetpackduba.gitnuro.domain.usecases

import javax.inject.Inject

class SaveConfigurationUseCase @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
) {
}