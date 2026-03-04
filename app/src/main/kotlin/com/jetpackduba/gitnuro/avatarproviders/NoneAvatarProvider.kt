package com.jetpackduba.gitnuro.avatarproviders


class NoneAvatarProvider : AvatarProvider {
    override fun getAvatarUrl(hashedEmail: String): String? {
        return null
    }
}