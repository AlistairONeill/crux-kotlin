package test.crux.kotlin.queries

import clojure.lang.Keyword
import clojure.lang.PersistentHashSet
import crux.api.Crux
import crux.kotlin.extensions.kw
import crux.kotlin.extensions.sym
import crux.kotlin.queries.queryKt
import crux.kotlin.transactions.submitTx
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import test.crux.kotlin.Convenience.TIMEOUT
import java.util.Comparator

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JoinedQueries {
    companion object {
        private val name = "name".kw
        private val follows = "follows".kw

        private val ivan = Person("ivan".kw, "Ivan")
        private val petr = Person("petr".kw, "Petr", "Ivan", "Sergei")
        private val sergei = Person("sergei".kw, "Sergei", "Ivan")
        private val denisA = Person("denis-a".kw, "Dennis")
        private val denisB = Person("denis-b".kw, "Dennis")

        private val all = listOf(ivan, petr, sergei, denisA, denisB)
    }

    private class Person(val id: Keyword, val name: String, vararg follows: String) {
        val follows = PersistentHashSet.create(*follows)
    }

    private val node = Crux.startNode().apply {
        submitTx {
            all.forEach {
                put(it.id) {
                    doc(name to it.name,
                        follows to it.follows)
                }
            }
        }

        sync(TIMEOUT)
    }

    @Test
    fun `Join across entities on a single attribute`() {
        val p1 = "p1".sym
        val p2 = "p2".sym
        val n = "n".sym

        val resultRaw = node.db().use {
            it.queryKt {
                find {
                    +p1
                    +p2
                }

                where {
                    add(p1, name, n)
                    add(p2, name, n)
                }
            }()
        }

        val comparator = Comparator { l1: List<Keyword>, l2: List<Keyword> ->
            val first = l1[0].compareTo(l2[0])
            if (first != 0) {
                first
            }
            else {
                l1[1].compareTo(l2[1])
            }
        }

        @Suppress("UNCHECKED_CAST")
        val result = resultRaw?.toList() as List<List<Keyword>>

        assertNotNull(result) { "We should have received a result object" }

        assertEquals(
            listOf(
                listOf(ivan.id, ivan.id),
                listOf(petr.id, petr.id),
                listOf(sergei.id, sergei.id),
                listOf(denisA.id, denisA.id),
                listOf(denisB.id, denisB.id),
                listOf(denisA.id, denisB.id),
                listOf(denisB.id, denisA.id)
            ).sortedWith(comparator),
            result.sortedWith(comparator)
        ) { "We should see everything match with itself and then the Denises matching with each other both ways" }
    }

    @Nested
    inner class MultiValue {
        @Test
        fun `Single result`() {
            val e = "e".sym
            val e2 = "e2".sym
            val l = "l".sym

            val resultRaw = node.db().use {
                it.queryKt {
                    find {
                        +e2
                    }

                    where {
                        add(e, name, l)
                        add(e2, follows, l)
                        add(e, name, "Sergei")
                    }
                }()
            }

            @Suppress("UNCHECKED_CAST")
            val result = resultRaw?.toList() as List<List<Keyword>>

            assertNotNull(result) { "We should have received a result object" }

            assertEquals(1, result.size) { "We should have received one result" }
            assertEquals(1, result[0].size) { "The result should only have one value" }
            assertEquals(petr.id, result[0][0]) { "The value should be Petr's ID"}
        }

        @Test
        fun `Multi result`() {
            val e = "e".sym
            val e2 = "e2".sym
            val l = "l".sym

            val resultRaw = node.db().use {
                it.queryKt {
                    find {
                        +e2
                    }

                    where {
                        add(e, name, l)
                        add(e2, follows, l)
                        add(e, name, "Ivan")
                    }
                }()
            }

            @Suppress("UNCHECKED_CAST")
            val result = resultRaw?.toList() as List<List<Keyword>>

            assertNotNull(result) { "We should have received a result object" }

            assertEquals(2, result.size) { "We should have received two results" }
            assert(result.all { it.size == 1 } ) { "All results should have one value" }
            assertEquals(
                listOf(
                    listOf(petr.id),
                    listOf(sergei.id)
                ).sortedBy { it[0] },
                result.sortedBy { it[0] }
            ) { "Should have returned Petr and Sergei as both of them follow Ivan" }
        }
    }


    @AfterAll
    fun closeNode() {
        node.close()
    }
}