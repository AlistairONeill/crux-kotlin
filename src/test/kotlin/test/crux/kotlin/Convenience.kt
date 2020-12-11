package test.crux.kotlin

import crux.api.ICruxAPI
import test.crux.kotlin.Convenience.TIMEOUT
import java.time.Duration
import java.time.Instant
import java.util.*

object Convenience {
    val TIMEOUT = Duration.ofSeconds(10)
    val EPOCH = Date.from(Instant.EPOCH)
}

fun ICruxAPI.sync() {
    sync(TIMEOUT)
}