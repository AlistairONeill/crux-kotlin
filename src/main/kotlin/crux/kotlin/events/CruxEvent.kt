package crux.kotlin.events

import clojure.lang.Keyword
import crux.kotlin.extensions.kw

enum class CruxEvent(val key: Keyword) {
    IndexedTx("crux/indexed-tx".kw)
}