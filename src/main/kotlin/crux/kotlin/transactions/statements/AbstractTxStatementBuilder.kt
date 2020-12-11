package crux.kotlin.transactions.statements

import clojure.lang.Keyword
import clojure.lang.PersistentVector
import java.time.Instant
import java.util.*

abstract class AbstractTxStatementBuilder(private val command: Keyword) {
    abstract val words: Array<out Any?>

    var validTime: Date? = null
    var endValidTime: Date? = null

    fun validTime(inst: Instant) {
        validTime = Date.from(inst)
    }

    fun endValidTime(inst: Instant) {
        endValidTime = Date.from(inst)
    }

    fun build(): PersistentVector {
        val output = PersistentVector.create(command, *words)
        val validTime = validTime ?: return output
        val endValidTime = endValidTime ?: return output.cons(validTime)
        return output.cons(validTime).cons(endValidTime)
    }
}