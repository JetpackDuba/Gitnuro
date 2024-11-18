package com.jetpackduba.gitnuro

import com.jetpackduba.gitnuro.repositories.initPreferencesPath
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security



fun main(args: Array<String>) {
    Security.addProvider(BouncyCastleProvider())

    initPreferencesPath()

    val app = App()
    app.start(args)
}
