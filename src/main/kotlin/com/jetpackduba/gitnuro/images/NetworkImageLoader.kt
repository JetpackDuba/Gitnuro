package com.jetpackduba.gitnuro.images

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.useResource
import com.jetpackduba.gitnuro.extensions.acquireAndUse
import com.jetpackduba.gitnuro.extensions.toByteArray
import com.jetpackduba.gitnuro.logging.printLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.io.FileNotFoundException
import java.net.HttpURLConnection
import java.net.URL

object NetworkImageLoader {
    // FIXME: Keep this at 1, until we fix locking per-URL
    // Above 1, the URL-fetch in the critical section will race with itself,
    // because multiple fetches will be queued while the first image is still being put
    // into the cache.
    private const val MAX_LOADING_IMAGES = 1

    private val loadingImagesSemaphore = Semaphore(MAX_LOADING_IMAGES)
    private val cache: ImagesCache = InMemoryImagesCache

    fun loadCachedImage(url: String): ImageBitmap? {
        val cachedImage = cache.getCachedImage(url)

        return cachedImage?.toComposeImage()
    }

    suspend fun loadImageNetwork(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val cachedImage = loadCachedImage(url)

            if (cachedImage != null) {
                return@withContext cachedImage
            }

            loadingImagesSemaphore.acquireAndUse {
                // re-try cache; perhaps another coroutine found the same image
                // while we were waiting to acquire the semaphore
                val retryCachedImage = loadCachedImage(url)
                if (retryCachedImage != null) {
                    printLog("NET", "found image $url on retry");
                    return@withContext retryCachedImage
                }

                val imageByteArray = loadImage(url)
                cache.cacheImage(url, imageByteArray)
                return@withContext imageByteArray.toComposeImage()
            }

        } catch (ex: Exception) {
            if (ex !is FileNotFoundException) {
                // Commented as it fills the logs without useless info when there is no internet connection
                //ex.printStackTrace()
            }
        }

        // If a previous return hasn't been called, something has gone wrong, return null
        return@withContext null
    }

    suspend fun loadImage(link: String): ByteArray = withContext(Dispatchers.IO) {
        // Network access can be considered somewhat surprising for a repository-browser
        // Don't be shy about admitting the work we do.
        printLog("NET", "loading image from $link")

        val url = URL(link)
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()

        connection.inputStream.toByteArray()
    }
}

@Composable
fun rememberNetworkImageOrNull(url: String, placeHolderImageRes: String? = null): ImageBitmap? {
    val networkImageLoader = NetworkImageLoader
    val cacheImageUsed = remember { ValueHolder(false) }

    var image by remember(url) {
        val cachedImage = networkImageLoader.loadCachedImage(url)

        val image: ImageBitmap? = when {
            cachedImage != null -> {
                cacheImageUsed.value = true
                cachedImage
            }

            placeHolderImageRes != null -> useResource(placeHolderImageRes) {
                Image.makeFromEncoded(it.toByteArray()).toComposeImageBitmap()
            }

            else -> null
        }

        return@remember mutableStateOf(image)
    }

    LaunchedEffect(url) {
        if (!cacheImageUsed.value) {
            val networkImage = NetworkImageLoader.loadImageNetwork(url)

            if (networkImage != null && !cacheImageUsed.value) {
                image = networkImage
            }
        }

    }

    return image
}

fun ByteArray.toComposeImage() = Image.makeFromEncoded(this).toComposeImageBitmap()


internal class ValueHolder<T>(var value: T) {
    override fun toString(): String {
        return value.toString()
    }
}