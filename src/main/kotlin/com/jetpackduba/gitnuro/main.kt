package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.managers.ShellManager
import com.jetpackduba.gitnuro.preferences.initPreferencesPath

fun main(args: Array<String>) {
    initLogging()
    initPreferencesPath()

    val app = App()
    app.start(args)
}
