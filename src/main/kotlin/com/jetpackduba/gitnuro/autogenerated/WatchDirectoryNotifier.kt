
//package 

interface WatchDirectoryNotifier {

fun shouldKeepLooping(): Boolean
fun detectedChange(
        paths: Array<String>,
    )
fun onError(
        code: Int,
    )
}
