package com.jetpackduba.gitnuro.domain.credentials.external

interface IGitCredentialsManagerProvider {
    fun loadPath(): String?
}