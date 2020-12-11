package crux.kotlin.extensions

import clojure.lang.PersistentVector

val <E> List<E>.pv: PersistentVector get() = PersistentVector.create(this)