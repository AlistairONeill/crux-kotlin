package test.crux.kotlin.queries

import clojure.lang.Keyword
import clojure.lang.PersistentHashSet
import crux.api.Crux
import crux.kotlin.extensions.kw
import crux.kotlin.extensions.sym
import crux.kotlin.queries.queryKt
import crux.kotlin.transactions.submitTx
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import test.crux.kotlin.Convenience

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Rules {
    companion object {
        val followKw = "follow".kw
        private val name = "name".kw

        private val ivan = Person("ivan".kw, "Ivan")
        private val petr = Person("petr".kw, "Petr", "sergei".kw)
        private val sergei = Person("sergei".kw, "Sergei", "ivan".kw)
        private val denisA = Person("denis".kw, "Dennis")

        private val all = listOf(ivan, petr, sergei, denisA)
    }

    private class Person(val id: Keyword, val name: String, vararg follows: Keyword) {
        val follows: PersistentHashSet = PersistentHashSet.create(*follows)
    }

    private val node = Crux.startNode().apply {
        submitTx {
            all.forEach {
                put(it.id) {
                    doc(name to it.name,
                        followKw to it.follows)
                }
            }
        }

        sync(Convenience.TIMEOUT)
    }


    @Test
    fun `Recursive rules`() {
        val followSym = "follow".sym

        val e1 = "?e1".sym
        val e2 = "?e2".sym
        val n = "?n".sym
        val t = "?t".sym

        val resultRaw = node.db().queryKt {
            find {
                sym(e1)
            }

            where {
                rule(followSym, e1, n)
            }

            args {
                scalar(n)
            }

            rules {
                add(followSym, e1, e2) {
                    add(e1, followKw, e2)
                }

                add(followSym, e1, e2) {
                    add(e1, followKw, t)
                    rule(followSym, t, e2)
                }
            }

            order {
                asc(e1)
            }
        }.run {
            scalar(ivan.id)
        }

        assertNotNull(resultRaw) { "We should have received a result object" }
        @Suppress("UNCHECKED_CAST")
        val result = resultRaw!!.toList() as List<List<Any>>
        assertEquals(2, result.size) { "Should have received two results" }
        assert(result.all { it.size == 1 } ) { "Each result should have one value" }
        assertEquals(listOf(petr.id, sergei.id), result.map { it[0] }) { "Should have returned petr and sergei's ids in that order" }
    }
}