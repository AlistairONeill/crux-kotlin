package crux.kotlin.transactions.statements

import crux.kotlin.CruxKt.DB_ID
import crux.kotlin.CruxKt.FN_ID
import crux.kotlin.CruxKt.PUT
import crux.kotlin.extensions.clj
import crux.kotlin.extensions.pam

class PutTxBuilder(private val id: Any): AbstractTxStatementBuilder(PUT) {
    override val words get() = arrayOf(data.pam)

    private var data: HashMap<Any, Any> = hashMapOf(DB_ID to id)

    fun doc(vararg pairs: Pair<Any, Any>) {
        data = hashMapOf(DB_ID to id)
        add(*pairs)
    }

    fun add(vararg pairs: Pair<Any, Any>) {
        data.putAll(pairs)
    }

    fun fn(raw: String) = add(FN_ID to raw.clj)
}