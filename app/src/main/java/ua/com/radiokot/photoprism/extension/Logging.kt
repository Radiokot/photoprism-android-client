package ua.com.radiokot.photoprism.extension

import mu.KLogger
import mu.KotlinLogging

fun Any.kLogger(name: String): KLogger = KotlinLogging.logger("$name@${Integer.toHexString(hashCode())}")