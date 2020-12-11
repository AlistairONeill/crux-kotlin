package crux.kotlin

import clojure.lang.Keyword
import crux.api.Crux
import crux.api.ICruxAPI
import crux.kotlin.configuration.NodeConfiguratorKt
import crux.kotlin.extensions.kw

object CruxKt {
    val DB_ID: Keyword = "crux.db/id".kw

    val EVICT: Keyword = "crux.tx/evict".kw
    val MATCH: Keyword = "crux.tx/match".kw
    val DELETE: Keyword = "crux.tx/delete".kw
    val PUT: Keyword = "crux.tx/put".kw
    val FN: Keyword = "crux.tx/fn".kw

    val FN_ID: Keyword = "crux.db/fn".kw

    val EVENT_TYPE: Keyword = "crux/event-type".kw
    val WITH_TX_OPS: Keyword = "with-tx-ops?".kw
    val TX_OPS: Keyword = "crux/tx-ops".kw

    fun startNode(f: NodeConfiguratorKt.() -> Unit): ICruxAPI = Crux.startNode(NodeConfiguratorKt().also(f).modules)
}