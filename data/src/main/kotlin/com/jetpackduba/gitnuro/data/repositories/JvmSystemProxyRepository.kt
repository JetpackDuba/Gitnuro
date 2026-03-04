package com.jetpackduba.gitnuro.data.repositories

import com.jetpackduba.gitnuro.domain.models.ProxySettings
import com.jetpackduba.gitnuro.domain.models.ProxyType
import com.jetpackduba.gitnuro.domain.repositories.SystemProxyRepository
import java.net.Authenticator
import java.net.PasswordAuthentication
import javax.inject.Inject

class JvmSystemProxyRepository @Inject constructor() : SystemProxyRepository {
    override suspend fun setProxy(proxySettings: ProxySettings) {
        when(proxySettings.proxyType) {
            ProxyType.HTTP -> setHttpProxy(proxySettings)
            ProxyType.SOCKS -> setSocksProxy(proxySettings)
        }
    }

    override suspend fun clearProxy() {
        System.setProperty("http.proxyHost", "")
        System.setProperty("http.proxyPort", "")
        System.setProperty("https.proxyHost", "")
        System.setProperty("https.proxyPort", "")
        System.setProperty("socksProxyHost", "")
        System.setProperty("socksProxyPort", "")
    }

    private fun setHttpProxy(proxySettings: ProxySettings) {
        System.setProperty("http.proxyHost", proxySettings.hostName)
        System.setProperty("http.proxyPort", proxySettings.hostPort.toString())
        System.setProperty("https.proxyHost", proxySettings.hostName)
        System.setProperty("https.proxyPort", proxySettings.hostPort.toString())

        if (proxySettings.useAuth) {
            Authenticator.setDefault(
                object : Authenticator() {
                    public override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(proxySettings.hostUser, proxySettings.hostPassword.toCharArray())
                    }
                }
            )

            System.setProperty("http.proxyUser", proxySettings.hostUser)
            System.setProperty("http.proxyPassword", proxySettings.hostPassword)
            System.setProperty("https.proxyUser", proxySettings.hostUser)
            System.setProperty("https.proxyPassword", proxySettings.hostPassword)
            System.setProperty("jdk.http.auth.tunneling.disabledSchemes", "")
        }
    }

    private fun setSocksProxy(proxySettings: ProxySettings) {
        System.setProperty("socksProxyHost", proxySettings.hostName)
        System.setProperty("socksProxyPort", proxySettings.hostPort.toString())

        if (proxySettings.useAuth) {
            Authenticator.setDefault(
                object : Authenticator() {
                    public override fun getPasswordAuthentication(): PasswordAuthentication {
                        return PasswordAuthentication(proxySettings.hostUser, proxySettings.hostPassword.toCharArray())
                    }
                }
            )

            System.setProperty("java.net.socks.username", proxySettings.hostUser)
            System.setProperty("java.net.socks.password", proxySettings.hostPassword)
        }
    }
}