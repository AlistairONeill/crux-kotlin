package crux.kotlin.transactions.statements

import crux.kotlin.CruxKt.DB_ID
import crux.kotlin.CruxKt.MATCH
import crux.kotlin.extensions.pam

class MatchTxBuilder(private val id: Any): AbstractTxStatementBuilder(MATCH) {
    var exists = true
    private var data: HashMap<Any, Any> = hashMapOf(DB_ID to id)

    fun add(vararg pairs: Pair<Any, Any>) {
        data.putAll(pairs)
    }

    operator fun Pair<Any, Any>.unaryPlus() {
        data[this.first] = this.second
    }

    override val words: Array<out Any?>
        get() = if (exists) {
            arrayOf(id, data.pam)
        }
        else {
            arrayOf(id, null)
        }
}