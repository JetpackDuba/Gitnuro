package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.repositories.initPreferencesPath

fun main(args: Array<String>) {
    initPreferencesPath()

    val app = App()
    app.start(args)
}
