package crux.kotlin.queries

import clojure.lang.IPersistentMap
import crux.api.ICruxDatasource

class QueryRunnable(private val params: IPersistentMap, private val datasource: ICruxDatasource) {

    fun run(f: (RunBuilder.()->Unit)? = null): MutableCollection<MutableList<*>>? =
        datasource.query(params, *RunBuilder().also{f?.invoke(it)}.build())

    operator fun invoke(f: (RunBuilder.()->Unit)? = null) = run(f)
}