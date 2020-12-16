package test.crux.kotlin.queries

import clojure.lang.Keyword
import crux.api.Crux
import crux.kotlin.extensions.kw
import crux.kotlin.extensions.sym
import crux.kotlin.queries.queryKt
import crux.kotlin.transactions.submitTx
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import test.crux.kotlin.Convenience.TIMEOUT
import java.util.Comparator
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OrderAndOffset {
    companion object {
        val number = "number".kw

        private val all = (0 until 100).map { it to (0 .. 1000).random() }.map { NumberEntity("id-${it.first}".kw, it.second.toLong()) }
    }

    private data class NumberEntity(val id: Keyword, val number: Long)

    private val node = Crux.startNode().apply {
        submitTx {
            all.forEach {
                put(it.id) {
                    doc(number to it.number)
                }
            }
        }

        sync(TIMEOUT)
    }

    @Nested
    inner class Order {

        @Test
        fun `Sorting ascending`() {
            val p = "p".sym
            val n = "n".sym

            val rawResult = node.db().use {
                it.queryKt {
                    find {
                        +p
                        +n
                    }

                    where {
                        add(p, number, n)
                    }

                    order {
                        asc(n)
                        asc(p)
                    }
                }()
            }

            val comparator = Comparator { l1: List<Any>, l2: List<Any> ->
                val n1 = l1[1] as Long
                val n2 = l2[1] as Long
                if (n1 != n2) {
                    (n1 - n2).toInt()
                }
                else {
                    val k1 = l1[0] as Keyword
                    val k2 = l2[0] as Keyword
                    k1.compareTo(k2)
                }
            }

            @Suppress("UNCHECKED_CAST")
            val result = rawResult?.toList() as List<List<Any>>
            assertNotNull(result) { "We should have received a result object" }

            assertEquals(100, result.size) { "We should have received all of our entities back" }

            assertEquals(
                all.map { listOf(it.id, it.number) }.sortedWith(comparator),
                result.map { listOf(it[0], it[1])}
            ) { "These should match up" }
        }

        @Test
        fun `Sorting descending`() {
            val p = "p".sym
            val n = "n".sym

            val rawResult = node.db().use {
                it.queryKt {
                    find {
                        +p
                        +n
                    }

                    where {
                        add(p, number, n)
                    }

                    order {
                        desc(n)
                        asc(p)
                    }
                }()
            }

            val comparator = Comparator { l1: List<Any>, l2: List<Any> ->
                val n1 = l1[1] as Long
                val n2 = l2[1] as Long
                if (n1 != n2) {
                    (n2 - n1).toInt()
                }
                else {
                    val k1 = l1[0] as Keyword
                    val k2 = l2[0] as Keyword
                    k1.compareTo(k2)
                }
            }

            @Suppress("UNCHECKED_CAST")
            val result = rawResult?.toList() as List<List<Any>>
            assertNotNull(result) { "We should have received a result object" }

            assertEquals(100, result.size) { "We should have received all of our entities back" }

            assertEquals(
                all.map { listOf(it.id, it.number) }.sortedWith(comparator),
                result.map { listOf(it[0], it[1])}
            ) { "These should match up" }
        }
    }

    @Nested
    inner class Limit {
        @Test
        fun `Sort ascending with limit`() {
            val p = "p".sym
            val n = "n".sym

            val rawResult = node.db().use {
                it.queryKt {
                    find {
                        +p
                        +n
                    }

                    where {
                        add(p, number, n)
                    }

                    order {
                        asc(n)
                        asc(p)
                    }

                    limit(10)
                }()
            }

            val comparator = Comparator { l1: List<Any>, l2: List<Any> ->
                val n1 = l1[1] as Long
                val n2 = l2[1] as Long
                if (n1 != n2) {
                    (n1 - n2).toInt()
                }
                else {
                    val k1 = l1[0] as Keyword
                    val k2 = l2[0] as Keyword
                    k1.compareTo(k2)
                }
            }

            @Suppress("UNCHECKED_CAST")
            val result = rawResult?.toList() as List<List<Any>>
            assertNotNull(result) { "We should have received a result object" }

            assertEquals(10, result.size) { "We should have received all of our entities back" }

            assertEquals(
                all.map { listOf(it.id, it.number) }.sortedWith(comparator).take(10),
                result.map { listOf(it[0], it[1])}
            ) { "These should match up" }
        }

        @Test
        fun `Sort descending with limit`() {
            val p = "p".sym
            val n = "n".sym

            val rawResult = node.db().use {
                it.queryKt {
                    find {
                        +p
                        +n
                    }

                    where {
                        add(p, number, n)
                    }

                    order {
                        desc(n)
                        asc(p)
                    }

                    limit(10)
                }()
            }

            val comparator = Comparator { l1: List<Any>, l2: List<Any> ->
                val n1 = l1[1] as Long
                val n2 = l2[1] as Long
                if (n1 != n2) {
                    (n2 - n1).toInt()
                }
                else {
                    val k1 = l1[0] as Keyword
                    val k2 = l2[0] as Keyword
                    k1.compareTo(k2)
                }
            }

            @Suppress("UNCHECKED_CAST")
            val result = rawResult?.toList() as List<List<Any>>
            assertNotNull(result) { "We should have received a result object" }

            assertEquals(10, result.size) { "We should have received all of our entities back" }

            assertEquals(
                all.map { listOf(it.id, it.number) }.sortedWith(comparator).take(10),
                result.map { listOf(it[0], it[1])}
            ) { "These should match up" }
        }
    }

    @Nested
    inner class Offset {
        @Test
        fun `Using in built offset`() {
            val p = "p".sym
            val n = "n".sym

            val rawResult = node.db().use {
                it.queryKt {
                    find {
                        +p
                        +n
                    }

                    where {
                        add(p, number, n)
                    }

                    order {
                        asc(n)
                        asc(p)
                    }

                    limit(10)

                    offset(10)
                }()
            }

            val comparator = Comparator { l1: List<Any>, l2: List<Any> ->
                val n1 = l1[1] as Long
                val n2 = l2[1] as Long
                if (n1 != n2) {
                    (n1 - n2).toInt()
                }
                else {
                    val k1 = l1[0] as Keyword
                    val k2 = l2[0] as Keyword
                    k1.compareTo(k2)
                }
            }

            @Suppress("UNCHECKED_CAST")
            val result = rawResult?.toList() as List<List<Any>>
            assertNotNull(result) { "We should have received a result object" }

            assertEquals(10, result.size) { "We should have received all of our entities back" }

            assertEquals(
                all.map { listOf(it.id, it.number) }.sortedWith(comparator).drop(10).take(10),
                result.map { listOf(it[0], it[1])}
            ) { "These should match up" }
        }
    }

    @AfterAll
    fun closeNode() {
        node.close()
    }
}