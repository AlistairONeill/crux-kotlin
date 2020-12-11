package crux.kotlin.queries

import clojure.lang.PersistentVector
import clojure.lang.Symbol
import crux.kotlin.extensions.clj
import crux.kotlin.extensions.pv

class ArgBuilder {
    companion object {
        private val COL = "...".clj
    }

    private val args = ArrayList<Any>()

    fun scalar(symbol: Symbol) = args.add(symbol)
    fun collection(symbol: Symbol) = args.add(PersistentVector.create(symbol, COL))
    fun tuple(vararg symbols: Symbol) = args.add(PersistentVector.create(*symbols))

    fun build() = args.pv
}