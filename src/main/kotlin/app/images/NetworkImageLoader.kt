package app.images

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.useResource
import app.extensions.acquireAndUse
import app.extensions.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.net.HttpURLConnection
import java.net.URL

private const val MAX_LOADING_IMAGES = 3

object NetworkImageLoader {
    private val loadingImagesSemaphore = Semaphore(MAX_LOADING_IMAGES)
    private val cache: ImagesCache = InMemoryImagesCache

    suspend fun loadImageNetwork(url: String): ImageBitmap? = withContext(Dispatchers.IO) {
        try {
            val cachedImage = cache.getCachedImage(url)

            if (cachedImage != null)
                return@withContext cachedImage.toComposeImage()

            loadingImagesSemaphore.acquireAndUse {
                val imageByteArray = loadImage(url)
                cache.cacheImage(url, imageByteArray)
                return@withContext imageByteArray.toComposeImage()
            }

        } catch (ex: Exception) {
            ex.printStackTrace()
        }

        // If a previous return hasn't been called, something has gone wrong, return null
        return@withContext null
    }

    suspend fun loadImage(link: String): ByteArray = withContext(Dispatchers.IO) {
        val url = URL(link)
        val connection = url.openConnection() as HttpURLConnection
        connection.connect()

        connection.inputStream.toByteArray()
    }
}


@Composable
fun rememberNetworkImage(url: String): ImageBitmap {
    val networkImageLoader = NetworkImageLoader
    var image by remember(url) {
        mutableStateOf(
            useResource("image.jpg") {
                Image.makeFromEncoded(it.toByteArray()).toComposeImageBitmap()
            }
        )
    }

    LaunchedEffect(url) {
        val networkImage = networkImageLoader.loadImageNetwork(url)
        if (networkImage != null)
            image = networkImage
    }

    return image
}

fun ByteArray.toComposeImage() = Image.makeFromEncoded(this).toComposeImageBitmap()

