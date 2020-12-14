package crux.kotlin.queries

import clojure.lang.Keyword
import clojure.lang.PersistentVector
import crux.kotlin.extensions.kw
import crux.kotlin.extensions.pam
import crux.kotlin.extensions.pl
import crux.kotlin.extensions.pv

class ProjectionBuilder {
    companion object {
        private val AS = "as".kw
    }

    private val fields = ArrayList<Any>()
    private val nested = HashMap<Any, PersistentVector>()

    fun field(keyword: Keyword) = fields.add(keyword)

    fun field(keyword: Keyword, alias: Keyword) = fields.add(listOf(keyword, mapOf(AS to alias).pam).pl)

    fun nest(keyword: Keyword, f: ProjectionBuilder.()->Unit) {
        nested[keyword] = ProjectionBuilder().also(f).build()
    }

    fun nest(keyword: Keyword, alias: Keyword, f: ProjectionBuilder.()->Unit) {
        val key = listOf(
            keyword,
            mapOf(AS to alias).pam
        ).pl
        nested[key] = ProjectionBuilder().also(f).build()
    }

    fun build(): PersistentVector = if (nested.isEmpty()) {
        fields.pv
    }
    else {
        PersistentVector.create(*(fields.toArray()), nested.pam)
    }
}