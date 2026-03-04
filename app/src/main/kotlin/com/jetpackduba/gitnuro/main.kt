package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.data.repositories.initPreferencesPath
import com.jetpackduba.gitnuro.di.DaggerAppComponent
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security


fun main(args: Array<String>) {
    Security.addProvider(BouncyCastleProvider())

    initPreferencesPath()

    val app: App = DaggerAppComponent
        .create()
        .app()

    app.start(args)
}
