package crux.kotlin.queries

import clojure.lang.PersistentVector
import clojure.lang.Symbol
import crux.kotlin.extensions.pl
import crux.kotlin.extensions.pv

class WhereBuilder {
    private val clauses = ArrayList<PersistentVector>()

    fun add(vararg words: Any) = clauses.add(PersistentVector.create(*words))

    fun pred(symbol: Symbol, vararg args: Any) {
        val list = listOf(symbol, *args).pl as Any
        clauses.add(PersistentVector.create(list))
    }

    fun build(): PersistentVector = clauses.pv
}