package com.jetpackduba.gitnuro.images

object InMemoryImagesCache : ImagesCache {
    private val cachedImages = hashMapOf<String, ByteArray>()

    override fun getCachedImage(urlSource: String): ByteArray? {
        return cachedImages[urlSource]
    }

    override fun cacheImage(urlSource: String, image: ByteArray) {
        cachedImages[urlSource] = image
    }
}