package com.jetpackduba.gitnuro.domain

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppEnvInfo @Inject constructor() {
    var isFlatpak = false
}