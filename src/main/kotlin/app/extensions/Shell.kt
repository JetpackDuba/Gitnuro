package app.extensions

import java.util.*

fun runCommand(command: String): String? {
    return try {
        var result: String?
        Runtime.getRuntime().exec(command).inputStream.use { inputStream ->
            Scanner(inputStream).useDelimiter("\\A").use { s ->
                result = if (s.hasNext())
                    s.next()
                else
                    null
            }
        }

        result
    } catch (ex: Exception) {
        ex.printStackTrace()
        null
    }

}