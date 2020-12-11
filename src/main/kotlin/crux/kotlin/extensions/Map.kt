package crux.kotlin.extensions

import clojure.lang.IPersistentMap
import clojure.lang.PersistentArrayMap

val <K, V> Map<K, V>.pam: IPersistentMap get() = PersistentArrayMap.create(this)