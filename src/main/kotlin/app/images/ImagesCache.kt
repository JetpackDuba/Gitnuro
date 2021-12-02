package app.images

interface ImagesCache {
    fun getCachedObject(urlSource: String): ByteArray?
    fun cacheImage(urlSource: String, image: ByteArray)
}