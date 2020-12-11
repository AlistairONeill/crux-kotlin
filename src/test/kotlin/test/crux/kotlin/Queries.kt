package test.crux.kotlin

import clojure.lang.Keyword
import clojure.lang.PersistentHashSet
import crux.api.Crux
import crux.kotlin.extensions.kw
import crux.kotlin.extensions.sym
import crux.kotlin.queries.queryKt
import crux.kotlin.transactions.submitTx
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import test.crux.kotlin.Convenience.TIMEOUT
import java.lang.IllegalArgumentException
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Queries {
    companion object {
        private val ivan = TestDocument("ivan".kw, "Ivan", "Ivanov")
        private val petr = TestDocument("petr".kw, "Petr", "Petrov")
        private val smith = TestDocument("smith".kw, "Smith", "Smith")

        private val all = listOf(ivan, petr, smith)

        private val name = "name".kw
        private val lastName = "last-name".kw
    }

    private data class TestDocument(
        val id: Keyword,
        val name: String,
        val lastName: String,
    )

    private val node = Crux.startNode().also { node ->
        node.submitTx {
            all.forEach {
                put(it.id) {
                    doc(name to it.name,
                        lastName to it.lastName)
                }
            }
        }

        node.sync(TIMEOUT)
    }

    @Nested
    inner class BasicQuery {
        @Test
        fun `Perform simple query`() {
            val p1 = "p1".sym
            val n = "n".sym

            val resultRaw = node.db().use{
                it.queryKt {
                    find(p1)
                    where {
                        add(p1, name, n)
                        add(p1, lastName, n)
                    }
                }
            }.run()

            val result = parseResult(resultRaw)
            assertNotNull(result) { "Should have returned something" }
            assertEquals(1, result.size) { "Should only have returned Smith" }
            assertEquals(1, result[0].size) { "Should have returned one item for Smith" }
            assertEquals(smith.id, result[0][0]) { "Should have returned Smith's id" }
        }

        @Test
        fun `Returning some of our other symbols`() {
            val p1 = "p1".sym
            val n = "n".sym

            val resultRaw = node.db().use {
                it.queryKt {
                    find(p1, n)
                    where {
                        add(p1, name, n)
                        add(p1, lastName, n)
                    }
                }
            }.run()

            val result = parseResult(resultRaw)
            assertNotNull(result) { "Should have returned something" }
            assertEquals(1, result.size) { "Should only have returned Smith" }
            assertEquals(2, result[0].size) { "Should have returned two items for Smith" }
            assertEquals(smith.id, result[0][0]) { "Should have returned Smith's id" }
            assertEquals(smith.name, result[0][1]) { "Should have returned Smith's name" }
        }

        @Test
        fun `Returning more than one result`() {
            val p1 = "p1".sym

            val resultRaw = node.db().use {
                it.queryKt {
                    find(p1)
                    where {
                        add(p1, name)
                    }
                }
            }.run()

            val result = parseResult(resultRaw)
            assertNotNull(result) { "Should have returned something" }
            assertEquals(3, result.size) { "Should have returned all three" }
            assert(result.all { it.size == 1 }) { "Should have only returned one item for each" }

            @Suppress("UNCHECKED_CAST")
            assertEquals(
                all.map { it.id }.sorted(),
                (result.map { it[0] } as List<Keyword>).sorted()) {
                "Should have returned the id of each"
            }
        }

        @Test
        fun `Exceptions are thrown if an incorrect structure is used`() {
            assertThrows(IllegalArgumentException::class.java) {
                node.db().use {
                    it.queryKt {}
                }.run()
            }
        }
    }

    private fun parseResult(resultRaw: MutableCollection<MutableList<*>>?): List<List<Any>>? {
        @Suppress("UNCHECKED_CAST")
        return resultRaw?.toList() as List<List<Any>>?
    }

    @AfterAll
    fun closeNode() {
        node.close()
    }
}