package crux.kotlin.transactions.statements

import clojure.lang.PersistentArrayMap
import crux.kotlin.CruxKt.DB_ID
import crux.kotlin.CruxKt.MATCH

class MatchTxBuilder(private val id: Any): AbstractTxStatementBuilder(MATCH) {
    var exists = true
    private var data: HashMap<Any, Any> = hashMapOf(DB_ID to id)

    fun doc(vararg pairs: Pair<Any, Any>) {
        data = hashMapOf(DB_ID to id)
        add(*pairs)
    }

    fun add(vararg pairs: Pair<Any, Any>) {
        data.putAll(pairs)
    }

    override val words: Array<out Any?>
        get() = if (exists) {
            arrayOf(id, PersistentArrayMap.create(data))
        }
        else {
            arrayOf(id, null)
        }
}