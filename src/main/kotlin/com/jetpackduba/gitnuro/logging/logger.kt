package com.jetpackduba.gitnuro.logging

import io.github.oshai.kotlinlogging.KotlinLogging

val logger = KotlinLogging.logger("org.slf4j").apply {
}

fun printLog(tag: String, message: String) {
    println("[LOG] $tag - $message")
    logger.info { "$tag - $message" }
}

fun printDebug(tag: String, message: String) {
    println("[DEBUG] $tag - $message")
    logger.debug { "$tag - $message" }
}

fun printError(tag: String, message: String, e: Exception? = null) {
    println("[ERROR] $tag - $message")
    logger.error(e) { "$tag - $message" }
}