package crux.kotlin.queries

import clojure.lang.PersistentVector

class StatementBuilder {
    private val clauses = ArrayList<PersistentVector>()

    fun add(vararg words: Any) = clauses.add(PersistentVector.create(*words))

    fun build(): PersistentVector = PersistentVector.create(clauses)
}