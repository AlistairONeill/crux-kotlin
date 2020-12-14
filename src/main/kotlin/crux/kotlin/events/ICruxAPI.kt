package crux.kotlin.events

import clojure.lang.Keyword
import crux.api.ICruxAPI
import crux.kotlin.CruxKt.EVENT_TYPE
import crux.kotlin.CruxKt.WITH_TX_OPS
import crux.kotlin.extensions.pam

fun ICruxAPI.listen(event: CruxEvent, withTxOps: Boolean = false, action: ((MutableMap<Keyword, *>) -> Unit)?): AutoCloseable? {
    @Suppress("UNCHECKED_CAST")
    return listen(
        mapOf(
            EVENT_TYPE to event.key,
            WITH_TX_OPS to withTxOps
        ).pam as Map<Keyword, Any>,
        action
    )
}
