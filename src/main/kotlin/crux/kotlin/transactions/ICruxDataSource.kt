package crux.kotlin.transactions

import crux.api.ICruxDatasource

@Suppress("UNCHECKED_CAST")
fun ICruxDatasource.withTx(f: TxBuilder.()->Unit): ICruxDatasource? = withTx(TxBuilder().also(f).build() as List<List<*>>)