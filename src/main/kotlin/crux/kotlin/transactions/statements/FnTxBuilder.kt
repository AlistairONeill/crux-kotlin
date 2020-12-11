package crux.kotlin.transactions.statements

import crux.kotlin.CruxKt.FN

class FnTxBuilder(private val id: Any, private vararg val args: Any): AbstractTxStatementBuilder(FN) {
    override val words: Array<out Any?> get() = arrayOf(id, *args)
}