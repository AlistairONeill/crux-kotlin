package test.crux.kotlin.queries

import clojure.lang.Keyword
import crux.api.Crux
import crux.kotlin.extensions.kw
import crux.kotlin.extensions.sym
import crux.kotlin.queries.queryKt
import crux.kotlin.transactions.submitTx
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import test.crux.kotlin.Convenience.TIMEOUT

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JoinedQueries {
    companion object {
        private val name = "name".kw

        private val ivan = Person("ivan".kw, "Ivan")
        private val petr = Person("petr".kw, "Petr")
        private val sergei = Person("sergei".kw, "Sergei")
        private val denisA = Person("denis-a".kw, "Dennis")
        private val denisB = Person("denis-b".kw, "Dennis")

        private val all = listOf(ivan, petr, sergei, denisA, denisB)
    }

    private data class Person(val id: Keyword, val name: String)

    private val node = Crux.startNode().apply {
        submitTx {
            all.forEach {
                put(it.id) {
                    doc(name to it.name)
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
                    sym(p1)
                    sym(p2)
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

    @AfterAll
    fun closeNode() {
        node.close()
    }
}