package test.crux.kotlin.queries

import clojure.lang.Keyword
import crux.api.Crux
import crux.kotlin.extensions.kw
import crux.kotlin.extensions.pl
import crux.kotlin.extensions.pv
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
class SimpleQueries {
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

    private val node = Crux.startNode().apply {
        submitTx {
            all.forEach {
                put(it.id) {
                    doc(
                        name to it.name,
                        lastName to it.lastName)
                }
            }
        }

        sync(TIMEOUT)
    }

    @Nested
    inner class BasicQuery {
        @Test
        fun `Perform simple query`() {
            val p1 = "p1".sym
            val n = "n".sym

            val resultRaw = node.db().use{
                it.queryKt {
                    find {
                        sym(p1)
                    }

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
                    find {
                        sym(p1)
                        sym(n)
                    }

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
                    find {
                        sym(p1)
                    }

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

        @Test
        fun `Empty results behave`() {
            val p1 = "p1".sym

            val resultRaw = node.db().use {
                it.queryKt {
                    find {
                        sym(p1)
                    }

                    where {
                        add(p1, name, "James")
                    }
                }.run()
            }

            val result = parseResult(resultRaw)

            assertNotNull(result) { "Should have received an empty result set" }
            assertEquals(0, result.size) { "Should have received an empty result set" }
        }
    }

    @Nested
    inner class Arguments {
        @Nested
        inner class ScalarBinding {
            @Test
            fun `Single scalar binding`() {
                val p1 = "p1".sym
                val n = "name".sym

                val resultRaw = node.db().use {
                    it.queryKt {
                        find {
                            sym(p1)
                        }

                        args {
                            scalar(n)
                        }

                        where {
                            add(p1, name, n)
                        }
                    }.run {
                        scalar(ivan.name)
                    }
                }

                val result = parseResult(resultRaw)

                assertNotNull(result) { "Should have received a result" }
                assertEquals(1, result.size) { "Should only have received 1 result" }
                assertEquals(1, result[0].size) { "Should only have received 1 value in result" }
                assertEquals(ivan.id, result[0][0]) { "Should only have received Ivan's id" }
            }


            @Test
            fun `Multiple scalar bindings`() {
                val p1 = "p1".sym
                val n = "name".sym
                val l = "lastName".sym

                val resultRaw = node.db().use {
                    it.queryKt {
                        find {
                            sym(p1)
                        }

                        args {
                            scalar(n)
                            scalar(l)
                        }

                        where {
                            add(p1, name, n)
                            add(p1, lastName, l)
                        }
                    }.run {
                        scalar(ivan.name)
                        scalar(ivan.lastName)
                    }
                }

                val result = parseResult(resultRaw)

                assertNotNull(result) { "Should have received a result" }
                assertEquals(1, result.size) { "Should only have received 1 result" }
                assertEquals(1, result[0].size) { "Should only have received 1 value in result" }
                assertEquals(ivan.id, result[0][0]) { "Should only have received Ivan's id" }
            }

            @Test
            fun `Make sure that all args are being used`() {
                val p1 = "p1".sym
                val n = "name".sym
                val l = "lastName".sym

                val resultRaw = node.db().use {
                    it.queryKt {
                        find {
                            sym(p1)
                        }

                        args {
                            scalar(n)
                            scalar(l)
                        }

                        where {
                            add(p1, name, n)
                            add(p1, lastName, l)
                        }
                    }.run {
                        scalar(ivan.name)
                        scalar("NotIvanov")
                    }
                }

                val result = parseResult(resultRaw)

                assertNotNull(result) { "Should have received a result object" }
                assertEquals(0, result.size) { "Shouldn't have received any results" }
            }
        }

        @Nested
        inner class Collection {
            @Test
            fun `Collection binding works`() {
                val p1 = "p1".sym
                val n = "name".sym

                val resultRaw = node.db().use {
                    it.queryKt {
                        find {
                            sym(p1)
                        }

                        args {
                            collection(n)
                        }

                        where {
                            add(p1, name, n)
                        }
                    }.run {
                        collection(ivan.name, petr.name)
                    }
                }

                val result = parseResult(resultRaw)
                assertNotNull(result) { "Should have received a result object" }
                assertEquals(2, result.size) { "Should have received two results" }
                assert(result.all { it.size == 1 }) { "Should only have received the id of each" }

                @Suppress("UNCHECKED_CAST")
                assertEquals(
                    listOf(ivan, petr).map { it.id }.sorted(),
                    (result.map { it[0] } as List<Keyword>).sorted()) { "Should have received the id of Ivan and Petr" }
            }
        }

        @Nested
        inner class Tuple {
            @Test
            fun `Tuple Binding works`() {
                val p1 = "p1".sym
                val n = "name".sym
                val l = "lastName".sym

                val resultRaw = node.db().use {
                    it.queryKt {
                        find {
                            sym(p1)
                        }

                        args {
                            tuple(n, l)
                        }

                        where {
                            add(p1, name, n)
                            add(p1, lastName, l)
                        }
                    }.run {
                        tuple(ivan.name, ivan.lastName)
                    }
                }

                val result = parseResult(resultRaw)

                assertNotNull(result) { "We should have received a result" }
                assertEquals(1, result.size) { "We should have only received Ivan" }
                assertEquals(1, result[0].size) { "We should have only received Ivan's ID" }
                assertEquals(ivan.id, result[0][0]) { "We should have received Ivan's ID" }
            }
        }
    }

    @Nested
    inner class Predicates {
        @Test
        fun `Simple predicates work`() {
            val age = "age".sym
            val gt = ">".sym

            val resultRaw = node.db().use {
                it.queryKt {
                    find {
                        sym(age)
                    }

                    args {
                        collection(age)
                    }

                    where {
                        pred(gt, age, 21)
                    }
                }.run {
                    collection(21, 22)
                }
            }

            val result = parseResult(resultRaw)

            assertNotNull(result) { "We should have received a result" }
            assertEquals(1, result.size) { "We should only have received one result" }
            assertEquals(1, result[0].size) { "We should only have received one value" }
            assertEquals(22L, result[0][0]) { "That value should be 22" }
        }

        @Test
        fun `Another predicate`() {
            val age = "age".sym
            val odd = "odd?".sym

            val resultRaw = node.db().use {
                it.queryKt {
                    find {
                        sym(age)
                    }

                    args {
                        collection(age)
                    }

                    where {
                        pred(odd, age)
                    }
                }.run {
                    collection(21, 22)
                }
            }

            val result = parseResult(resultRaw)

            assertNotNull(result) { "We should have received a result" }
            assertEquals(1, result.size) { "We should only have received one result" }
            assertEquals(1, result[0].size) { "We should only have received one value" }
            assertEquals(21L, result[0][0]) { "That value should be 21" }
        }
    }

    @Nested
    inner class Aggregates {
        @Test
        fun `Mythology examples`() {
            val heads = "?heads".sym
            val monster = "?monster".sym

            val sum = "sum".sym
            val min = "min".sym
            val max = "max".sym
            val count = "count".sym
            val countDistinct = "count-distinct".sym

            val resultRaw = node.db().use {
                it.queryKt {
                    find {
                        agg(sum, heads)
                        agg(min, heads)
                        agg(max, heads)
                        agg(count, heads)
                        agg(countDistinct, heads)
                    }

                    where {
                        add(
                            listOf(
                                "identity".sym,
                                listOf(
                                    listOf("Cerberus", 3).pv,
                                    listOf("Medusa", 1).pv,
                                    listOf("Cyclops", 1).pv,
                                    listOf("Chimera", 1).pv
                                ).pv
                            ).pl,
                            listOf(
                                listOf(monster, heads).pv
                            ).pv
                        )
                    }
                }()
            }

            val result = parseResult(resultRaw)

            assertNotNull(result) { "We should have received a result" }
            assertEquals(1, result.size) { "There should be one result" }
            assertEquals(5, result[0].size) { "There should be five values in the result" }
            assertEquals(6L, result[0][0]) { "There should be six heads in total" }
            assertEquals(1L, result[0][1]) { "There is a minimum of one head per monster" }
            assertEquals(3L, result[0][2]) { "There is a maximum of three heads per monster" }
            assertEquals(4L, result[0][3]) { "There are four monsters" }
            assertEquals(2, result[0][4]) { "There are two different counts of heads/monster" }
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