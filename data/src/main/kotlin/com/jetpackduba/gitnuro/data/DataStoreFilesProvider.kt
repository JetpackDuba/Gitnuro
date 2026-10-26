package com.jetpackduba.gitnuro.data

import com.jetpackduba.gitnuro.common.OS
import com.jetpackduba.gitnuro.common.currentOs
import com.jetpackduba.gitnuro.common.systemSeparator
import javax.inject.Inject

class DataStoreFilesProvider @Inject constructor() {
    fun getGlobalSettingsPath(): String {
        return getPreferencesDir() + systemSeparator + "user_prefs.preferences_pb"
    }

    fun getRecentRepositoriesPath(): String {
        return getPreferencesDir() + systemSeparator + "recent_repositories.preferences_pb"
    }

    fun getTabsOpenPath(): String {
        return getPreferencesDir() + systemSeparator + "tabs_open.preferences_pb"
    }

    fun getUiSizesPath(): String {
        return getPreferencesDir() + systemSeparator + "ui_sizes.preferences_pb"
    }

    fun initDataStoreDirPath() {
        if (currentOs == OS.LINUX) {
            val xdgConfigHome: String? = System.getenv("XDG_CONFIG_HOME")

            val settingsPath = if (xdgConfigHome.isNullOrBlank()) {
                val home = System.getProperty("user.home").orEmpty()
                "$home/.config/gitnuro"
            } else {
                "$xdgConfigHome/gitnuro"
            }

            System.setProperty("java.util.prefs.userRoot", settingsPath)
        }
    }

    private fun getPreferencesDir(): String {
        val home = System.getProperty("user.home").orEmpty()

        return when (currentOs) {
            OS.LINUX -> {
                val xdgConfigHome: String? = System.getenv("XDG_CONFIG_HOME")

                val settingsPath = if (xdgConfigHome.isNullOrBlank()) {
                    "$home/.config/gitnuro"
                } else {
                    "$xdgConfigHome/gitnuro"
                }

                settingsPath
            }

            OS.MAC -> {
                "$home/Library/Application Support/gitnuro"
            }

            else -> {
                System.getProperty("java.util.prefs.userRoot") ?: "$home${systemSeparator}gitnuro"
            }
        }
    }
}