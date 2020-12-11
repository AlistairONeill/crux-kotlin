package crux.kotlin.transactions.statements

import crux.kotlin.CruxKt.DELETE

class DeleteTxBuilder(id: Any): AbstractTxStatementBuilder(DELETE) {
    override val words: Array<out Any> = arrayOf(id)
}