package crux.kotlin.queries

import clojure.lang.Keyword
import clojure.lang.PersistentVector
import crux.kotlin.extensions.kw
import crux.kotlin.extensions.pam

class QueryBuilder {
    companion object {
        val FIND: Keyword = "find".kw
        val WHERE: Keyword = "where".kw
        val IN: Keyword = "in".kw
    }

    private val map = HashMap<Keyword, PersistentVector>()

    private fun clause(name: Keyword, f: WhereBuilder.()->Unit) {
        map[name] = WhereBuilder().also(f).build()
    }

    private fun statement(name: Keyword, vararg words: Any) {
        map[name] = PersistentVector.create(*words)
    }

    fun find(vararg words: Any) = statement(FIND, *words)

    fun where(f: WhereBuilder.()->Unit) {
        map[WHERE] = WhereBuilder().also(f).build()
    }

    fun args(f: ArgBuilder.()->Unit) {
        map[IN] = ArgBuilder().also(f).build()
    }

    fun build() = map.pam
}