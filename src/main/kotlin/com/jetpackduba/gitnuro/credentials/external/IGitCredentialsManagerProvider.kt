package com.jetpackduba.gitnuro.credentials.external

interface IGitCredentialsManagerProvider {
    fun loadPath(): String?
}