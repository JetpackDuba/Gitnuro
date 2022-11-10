package com.jetpackduba.gitnuro.extensions

import com.jetpackduba.gitnuro.logging.printLog
import java.io.IOException
import java.util.*

private const val TAG = "Shell"

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

fun runCommandWithoutResult(command: String, args: String, file: String): Boolean {
    val parts: Array<String> = prepareCommand(command, args, file)

    printLog(TAG, "Running command ${parts.joinToString( )}")

    return try {
        val p = Runtime.getRuntime().exec(parts) ?: return false
        try {
            val exitValue = p.exitValue()

            if (exitValue == 0) {
                printLog(TAG, "Process ended immediately.")
                false
            } else {
                printLog(TAG, "Process crashed.")
                false
            }
        } catch (itse: IllegalThreadStateException) {
            printLog(TAG, "Process is running.")
            true
        }
    } catch (e: IOException) {
        printLog(TAG, "Error running command: ${e.message}")
        e.printStackTrace()
        false
    }
}


private fun prepareCommand(command: String, args: String?, file: String): Array<String> {
    val parts: MutableList<String> = ArrayList()
    parts.add(command)

    if (args != null) {
        for (s in args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()) {
            val stringFormatted = String.format(s, file) // put in the filename thing
            parts.add(stringFormatted.trim { it <= ' ' })
        }
    }
    return parts.toTypedArray()
}