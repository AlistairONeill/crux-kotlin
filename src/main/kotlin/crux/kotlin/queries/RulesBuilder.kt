package crux.kotlin.queries

import clojure.lang.PersistentVector
import clojure.lang.Symbol
import crux.kotlin.extensions.pv

class RulesBuilder {
    private val rules = ArrayList<PersistentVector>()

    fun add(name: Symbol, vararg args: Symbol, f: RuleBuilder.()->Unit) {
        rules.add(RuleBuilder(name, *args).also(f).build())
    }

    fun build(): PersistentVector = rules.pv
}