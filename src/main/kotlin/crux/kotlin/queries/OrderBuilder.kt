package crux.kotlin.queries

import clojure.lang.PersistentVector
import clojure.lang.Symbol
import crux.kotlin.extensions.kw
import crux.kotlin.extensions.pv

class OrderBuilder {
    companion object {
        private val ASC = "asc".kw
        private val DESC = "desc".kw
    }
    private val args = ArrayList<PersistentVector>()

    fun asc(sym: Symbol) = args.add(PersistentVector.create(sym, ASC))
    fun desc(sym: Symbol) = args.add(PersistentVector.create(sym, DESC))

    fun build() = args.pv
}