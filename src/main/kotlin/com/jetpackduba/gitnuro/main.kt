package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.repositories.initPreferencesPath

fun main(args: Array<String>) {
    System.load("/home/abde/Projects/Compose/Gitnuro/rs/target/release/libgitnuro_rs.so")
    initPreferencesPath()

    val app = App()
    app.start(args)
}
