package crux.kotlin.queries

import clojure.lang.IPersistentMap
import clojure.lang.Keyword
import clojure.lang.PersistentArrayMap
import clojure.lang.PersistentVector
import crux.kotlin.extensions.kw

class QueryBuilder {
    companion object {
        val FIND: Keyword = "find".kw
        val WHERE: Keyword = "where".kw
        val IN: Keyword = "in".kw
    }

    private val map = HashMap<Keyword, PersistentVector>()

    private fun clause(name: Keyword, f: StatementBuilder.()->Unit) {
        map[name] = StatementBuilder().also(f).build()
    }

    private fun statement(name: Keyword, vararg words: Any) {
        map[name] = PersistentVector.create(*words)
    }

    fun find(vararg words: Any) = statement(FIND, *words)
    fun where(f: StatementBuilder.()->Unit) = clause(WHERE, f)
    fun args(vararg words: Any) = statement(IN, *words)

    fun build(): IPersistentMap = PersistentArrayMap.create(map)
}