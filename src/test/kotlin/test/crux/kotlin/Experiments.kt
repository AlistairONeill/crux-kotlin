package test.crux.kotlin

import clojure.lang.Keyword
import crux.kotlin.extensions.kw
import crux.kotlin.extensions.pam
import crux.api.Crux
import crux.kotlin.transactions.submitTx
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.*
import kotlin.collections.ArrayList


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Experiments {
    companion object {
        private val id = "foo".kw
        private val key = "value".kw
        private val txTimeKey = "crux.tx/tx-time".kw
    }


    private val node = Crux.startNode()

    @Test
    fun `Do the thing as in the spreadsheet`() {
        val times = ArrayList<Date>()

        times.add(submit("A" to null))
        Thread.sleep(1000) // Putting sleeps in to make sure our times are spaced apart enough for clarity

        times.add(submit("B" to times[0]))
        Thread.sleep(1000)

        times.add(Calendar.getInstance().time)
        Thread.sleep(1000)

        times.add(submit(
            "G" to times[0],
            "E" to times[1],
            "D" to times[2],
            "C" to null
        ))
        Thread.sleep(1000)

        times.add(submit("F" to times[1]))

        val chart = generateChart(times)

        println(chart)
        /*
        ABBCC
        ABBCC
        ABBDD
        ABBEF
        ABBGG
        */
    }

    @Test
    fun `Do the thing adding in F in between "Tuesday" and "Wednesday" instead of on Tuesday`() {
        val times = ArrayList<Date>()

        times.add(submit("A" to null))
        Thread.sleep(1000) // Putting sleeps in to make sure our times are spaced apart enough for clarity

        times.add(submit("B" to times[0]))
        Thread.sleep(1000)

        times.add(Calendar.getInstance().time)
        Thread.sleep(500)
        times.add(Calendar.getInstance().time)
        Thread.sleep(500)

        times.add(submit(
            "G" to times[0],
            "E" to times[1],
            "D" to times[3],
            "C" to null
        ))
        Thread.sleep(1000)

        times.add(submit("F" to times[2]))

        val chart = generateChart(times)

        println(chart)
        /*
        ABBBCC
        ABBBCC
        ABBBDD
        ABBBEF
        ABBBEE
        ABBBGG
         */
    }

    @Test
    fun `Add F on valid between Twednesday morning and afternoon`() {
        val times = ArrayList<Date>()

        times.add(submit("A" to null))
        Thread.sleep(1000) // Putting sleeps in to make sure our times are spaced apart enough for clarity

        times.add(submit("B" to times[0]))
        Thread.sleep(1000)

        times.add(Calendar.getInstance().time) // We don't add anything on third tt "Wednesday"
        Thread.sleep(333)
        times.add(Calendar.getInstance().time)
        Thread.sleep(333)
        times.add(Calendar.getInstance().time)
        Thread.sleep(333)

        times.add(submit(
            "G" to times[0],
            "E" to times[1],
            "D" to times[4],
            "C" to null
        ))
        Thread.sleep(333)
        times.add(Calendar.getInstance().time)
        Thread.sleep(333)
        times.add(Calendar.getInstance().time)
        Thread.sleep(333)

        times.add(
            node.submitTx {
                put(id) {
                    doc(key to "F")
                    validTime = times[2] //Twednesday morning
                    endValidTime = times[3] //Wursday afternoon
                }
            }[txTimeKey] as Date
        )

        val chart = generateChart(times)
        println(chart)
        /*
        ABBBBCCCC
        ABBBBCCCC
        ABBBBCCCC
        ABBBBCCCC
        ABBBBDDDD
        ABBBBEEEE
        ABBBBEEEF
        ABBBBEEEE
        ABBBBGGGG
         */
    }

    /**
        Shorthand for adding a document with id of :foo and :value of pair.first onto the node.
        pair.second is the optional valid time
     **/
    private fun submit(vararg values: Pair<String, Date?>): Date {
        val txTime = node.submitTx {
            values.forEach {
                put(id) {
                    doc(key to it.first)

                    if (it.second != null) {
                        validTime = it.second
                    }
                }
            }
        }[txTimeKey] as Date

        node.sync(Convenience.TIMEOUT)

        return txTime
    }

    @Suppress("UNCHECKED_CAST")
    private fun generateChart(times: List<Date>) =
        times.reversed().joinToString("\n") { vt ->
            times.joinToString("") { tt ->
                node.db(
                    mapOf(
                        "crux.db/valid-time".kw to vt,
                        "crux.tx/tx-time".kw to tt
                    ).pam as MutableMap<Keyword, *>
                ).entity(id)[key] as String
            }
        }

    @AfterEach
    fun evictId() {
        node.submitTx {
            evict(id)
        }

        node.sync(Convenience.TIMEOUT)
    }
}