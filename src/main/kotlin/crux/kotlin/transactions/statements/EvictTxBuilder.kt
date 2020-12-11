package crux.kotlin.transactions.statements

import clojure.lang.PersistentVector
import crux.kotlin.CruxKt.EVICT

class EvictTxBuilder(private val id: Any) {
    fun build(): PersistentVector = PersistentVector.create(EVICT, id)
}