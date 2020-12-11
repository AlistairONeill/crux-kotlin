package crux.kotlin.transactions

import clojure.lang.Keyword
import crux.api.ICruxIngestAPI

@Suppress("UNCHECKED_CAST")
fun ICruxIngestAPI.submitTx(f: TxBuilder.()->Unit): MutableMap<Keyword, *> = submitTx(TxBuilder().also(f).build() as List<List<*>>)