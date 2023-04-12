package com.jetpackduba.gitnuro.logging

import io.github.oshai.KotlinLogging

val logger = KotlinLogging.logger("org.slf4j")

fun printLog(tag: String, message: String) {
    logger.info("$tag - $message")
}
fun printDebug(tag: String, message: String) {
    logger.debug("$tag - $message")
}

fun printError(tag: String, message: String, e: Exception? = null) {
    logger.error("$tag - $message", e)
}