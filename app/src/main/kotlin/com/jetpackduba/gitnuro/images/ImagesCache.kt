package com.jetpackduba.gitnuro.images

interface ImagesCache {
    fun getCachedImage(urlSource: String): ByteArray?
    fun cacheImage(urlSource: String, image: ByteArray)
}