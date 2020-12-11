package crux.kotlin.queries

import clojure.lang.PersistentVector
import crux.kotlin.extensions.pv

class WhereBuilder {
    private val clauses = ArrayList<PersistentVector>()

    fun add(vararg words: Any) = clauses.add(PersistentVector.create(*words))

    fun build(): PersistentVector = clauses.pv
}