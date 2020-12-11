package crux.kotlin.queries

import clojure.lang.Symbol
import crux.kotlin.extensions.pl
import crux.kotlin.extensions.pv
import crux.kotlin.extensions.sym

class FindBuilder {
    private val args = ArrayList<Any>()

    fun sym(symbol: Symbol) = args.add(symbol)
    fun sym(string: String) = args.add(string.sym)

    fun agg(agg: Symbol, sym: Symbol) = args.add(listOf(agg, sym).pl)

    fun build() = args.pv
}