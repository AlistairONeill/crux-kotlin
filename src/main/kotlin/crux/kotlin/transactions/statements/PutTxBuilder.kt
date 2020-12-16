package crux.kotlin.transactions.statements

import clojure.lang.Keyword
import crux.kotlin.CruxKt.DB_ID
import crux.kotlin.CruxKt.FN_ID
import crux.kotlin.CruxKt.PUT
import crux.kotlin.extensions.clj
import crux.kotlin.extensions.pam

class PutTxBuilder(id: Any): AbstractTxStatementBuilder(PUT) {
    override val words get() = arrayOf(data.pam)

    private var data: HashMap<Keyword, Any> = hashMapOf(DB_ID to id)

    fun add(vararg pairs: Pair<Keyword, Any>) {
        data.putAll(pairs)
    }

    fun fn(raw: String) = add(FN_ID to raw.clj)

    operator fun Pair<Keyword, Any>.unaryPlus() {
        add(this)
    }
}