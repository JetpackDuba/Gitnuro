package com.jetpackduba.gitnuro.domain.usecases

import com.jetpackduba.gitnuro.common.flows.combine
import com.jetpackduba.gitnuro.domain.models.ProxySettings
import com.jetpackduba.gitnuro.domain.repositories.SystemProxyRepository
import com.jetpackduba.gitnuro.domain.services.GlobalSettingsService
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class ObserveAndApplyProxySettingsUseCase @Inject constructor(
    private val settingsService: GlobalSettingsService,
    private val systemProxyRepository: SystemProxyRepository,
) {
    operator fun invoke() {
        @OptIn(DelicateCoroutinesApi::class)
        GlobalScope.launch {
            val proxySettings = loadProxySettings()


            proxySettings.collect { proxySettings ->
                if (proxySettings.useProxy) {
                    systemProxyRepository.setProxy(proxySettings)
                } else {
                    systemProxyRepository.clearProxy()
                }
            }
        }
    }

    private fun loadProxySettings() = combine(
        settingsService.useProxy,
        settingsService.proxyUseAuth,
        settingsService.proxyType,
        settingsService.proxyHostName,
        settingsService.proxyPortNumber,
        settingsService.proxyHostUser,
        settingsService.proxyHostPassword,
    ) { useProxy,
        proxyUseAuth,
        proxyType,
        proxyHostName,
        proxyPortNumber,
        proxyHostUser,
        proxyHostPassword ->
        ProxySettings(
            useProxy,
            proxyType,
            proxyHostName.orEmpty(),
            proxyPortNumber ?: 0,
            proxyUseAuth,
            proxyHostUser.orEmpty(),
            proxyHostPassword.orEmpty(),
        )
    }
}