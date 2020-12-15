package crux.kotlin.queries

import clojure.lang.Symbol
import crux.kotlin.extensions.pl
import crux.kotlin.extensions.pv
import crux.kotlin.extensions.sym
import crux.kotlin.projection.ICruxDataClass
import kotlin.reflect.KClass

class FindBuilder {
    companion object {
        private val PROJECT = "eql/project".sym
    }

    private val args = ArrayList<Any>()

    fun sym(symbol: Symbol) = args.add(symbol)
    fun sym(string: String) = args.add(string.sym)

    fun agg(agg: Symbol, sym: Symbol) = args.add(listOf(agg, sym).pl)

    fun project(sym: Symbol, f: ProjectionBuilder.()->Unit) {
        args.add(
            listOf(
                PROJECT,
                sym,
                ProjectionBuilder().also(f).build()
            ).pl
        )
    }

    fun <T: ICruxDataClass> project(sym: Symbol, cruxClass: KClass<T>) = project(sym, ICruxDataClass.getProjectionSpec(cruxClass))

    fun build() = args.pv
}