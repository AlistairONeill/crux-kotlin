package crux.kotlin.extensions

import clojure.lang.Keyword
import clojure.lang.RT
import clojure.lang.Symbol

val String.kw: Keyword get() = Keyword.intern(this)
val String.sym: Symbol get() = Symbol.intern(this)
val String.clj: Any get() = RT.readString(this)