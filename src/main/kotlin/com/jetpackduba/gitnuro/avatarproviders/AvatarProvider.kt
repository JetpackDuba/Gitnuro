package com.jetpackduba.gitnuro.avatarproviders

interface AvatarProvider {
    fun getAvatarUrl(hashedEmail: String): String?
}