package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.managers.ShellManager
import com.jetpackduba.gitnuro.preferences.initPreferencesPath

fun main(args: Array<String>) {
    initPreferencesPath()

    val app = App()
    app.start(args)
}
