package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.data.repositories.configuration.initPreferencesPath
import com.jetpackduba.gitnuro.di.DaggerAppComponent
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security


suspend fun main(args: Array<String>) {
    if (args.contains("--graalvm")) {
        val currentDir = System.getProperty("user.dir")

        System.setProperty("java.home", currentDir)
    }

    Security.addProvider(BouncyCastleProvider())

    initPreferencesPath()

    val app: App = DaggerAppComponent
        .create()
        .app()

    app.start(args)
}
