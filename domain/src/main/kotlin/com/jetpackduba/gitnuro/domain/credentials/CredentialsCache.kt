package com.jetpackduba.gitnuro.domain.credentials

import org.eclipse.jgit.transport.Transport


interface CredentialsCache {
    suspend fun cacheCredentialsIfNeeded()
}

interface CredentialsHandler {
    fun handleTransport(transport: Transport?)
}