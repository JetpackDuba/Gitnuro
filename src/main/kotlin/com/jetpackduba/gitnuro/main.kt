package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.preferences.initPreferencesPath

private const val TAG = "main"

fun main(args: Array<String>) {
    initLogging()
    initPreferencesPath()

    val app = App()
    app.start(args)
}
