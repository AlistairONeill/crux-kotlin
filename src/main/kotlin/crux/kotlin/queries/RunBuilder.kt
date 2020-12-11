package crux.kotlin.queries

import clojure.lang.PersistentVector

class RunBuilder {
    private val args = ArrayList<Any>()

    fun scalar(data: Any) = args.add(data)
    fun collection(vararg data: Any) = args.add(PersistentVector.create(*data))
    fun tuple(vararg data: Any) = args.add(PersistentVector.create(*data))

    fun build(): Array<out Any> = args.toArray()
}