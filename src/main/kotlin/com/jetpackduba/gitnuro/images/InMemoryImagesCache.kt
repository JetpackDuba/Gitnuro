package com.jetpackduba.gitnuro.images

import com.jetpackduba.gitnuro.logging.printError

object InMemoryImagesCache : ImagesCache {
    private val cachedImages = hashMapOf<String, ByteArray>()

    override fun getCachedImage(urlSource: String): ByteArray? {
        return cachedImages[urlSource]
    }

    override fun cacheImage(urlSource: String, image: ByteArray) {
        if (cachedImages[urlSource] != null) {
            printError("CACHE", "Race condition! Image for $urlSource was already in cache")
        }
        cachedImages[urlSource] = image
    }
}