package crux.kotlin.queries

import crux.api.ICruxDatasource

fun ICruxDatasource.queryKt(f: QueryBuilder.() -> Unit): QueryRunnable {
    val built = QueryBuilder().also(f).build()
    return QueryRunnable(built, this)
}