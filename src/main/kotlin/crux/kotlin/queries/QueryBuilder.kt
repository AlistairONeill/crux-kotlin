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
        val ORDER: Keyword = "order-by".kw
    }

    private val map = HashMap<Keyword, PersistentVector>()

    fun find(f: FindBuilder.()->Unit) {
        map[FIND] = FindBuilder().also(f).build()
    }

    fun where(f: WhereBuilder.()->Unit) {
        map[WHERE] = WhereBuilder().also(f).build()
    }

    fun args(f: ArgBuilder.()->Unit) {
        map[IN] = ArgBuilder().also(f).build()
    }

    fun order(f: OrderBuilder.()->Unit) {
        map[ORDER] = OrderBuilder().also(f).build()
    }

    fun build() = map.pam
}