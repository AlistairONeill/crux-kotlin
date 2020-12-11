package crux.kotlin.queries

import clojure.lang.IPersistentMap
import crux.api.ICruxDatasource

class QueryRunnable(private val params: IPersistentMap, private val datasource: ICruxDatasource) {
    fun run(vararg args: Any): MutableCollection<MutableList<*>>? = datasource.query(params, *args)
    operator fun invoke(vararg args: Any): MutableCollection<MutableList<*>>? = run(*args)
}