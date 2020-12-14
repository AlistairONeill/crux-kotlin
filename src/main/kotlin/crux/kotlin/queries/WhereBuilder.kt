package crux.kotlin.queries

import clojure.lang.PersistentVector
import clojure.lang.Symbol
import crux.kotlin.extensions.pl
import crux.kotlin.extensions.pv

class WhereBuilder {
    private val clauses = ArrayList<Any>()

    fun add(vararg words: Any) = clauses.add(PersistentVector.create(*words))

    fun pred(symbol: Symbol, vararg args: Any) {
        val list = listOf(symbol, *args).pl as Any
        clauses.add(PersistentVector.create(list))
    }

    fun rule(symbol: Symbol, vararg args: Any) = clauses.add(listOf(symbol, *args).pl)

    fun build(): PersistentVector = clauses.pv
}