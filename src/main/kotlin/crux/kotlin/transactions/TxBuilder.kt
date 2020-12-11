package crux.kotlin.transactions

import clojure.lang.PersistentVector
import crux.kotlin.extensions.pv
import crux.kotlin.transactions.statements.*

class TxBuilder {
    private val transactions = ArrayList<PersistentVector>()

    fun put(id: Any, f: (PutTxBuilder.()->Unit)? = null) = transactions.add(PutTxBuilder(id).also{f?.invoke(it)}.build())
    fun delete(id: Any, f: (DeleteTxBuilder.()->Unit)? = null) = transactions.add(DeleteTxBuilder(id).also{f?.invoke(it)}.build())
    fun match(id: Any, f:(MatchTxBuilder.()->Unit)? = null) = transactions.add(MatchTxBuilder(id).also{f?.invoke(it)}.build())
    fun evict(id: Any, f:(EvictTxBuilder.()->Unit)? = null) = transactions.add(EvictTxBuilder(id).also{f?.invoke(it)}.build())
    fun fn(id: Any, vararg args: Any) = transactions.add(FnTxBuilder(id, *args).build())

    fun build(): PersistentVector = transactions.pv
}