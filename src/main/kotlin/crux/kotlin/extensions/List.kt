package crux.kotlin.extensions

import clojure.lang.PersistentList
import clojure.lang.PersistentVector

val <E> List<E>.pv: PersistentVector get() = PersistentVector.create(this)
val <E> List<E>.pl: PersistentList get() = PersistentList.create(this) as PersistentList