package test.crux.kotlin.queries

import crux.api.Crux
import crux.kotlin.extensions.pl
import crux.kotlin.extensions.pv
import crux.kotlin.extensions.sym
import crux.kotlin.queries.queryKt
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertNotNull

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubQueries {
    private val node = Crux.startNode()

    @Nested
    inner class Simple {
        @Test
        fun `Binding results as a scalar`() {
            val x = "x".sym
            val y = "y".sym

            val resultRaw = node.db().use {
                it.queryKt {
                    find {
                        sym(x)
                    }

                    where {
                        subQuery(x) {
                            find {
                                sym(y)
                            }

                            where {
                                add(listOf("identity".sym, 2).pl, x)
                                add(listOf("+".sym, x, 2).pl, y)
                            }
                        }
                    }
                }()
            }

            assertNotNull(resultRaw) { "Should have received a result" }
            @Suppress("UNCHECKED_CAST")
            val result = resultRaw.toList() as List<List<List<List<Long>>>>
            assertEquals(1, result.size)
            assertEquals(1, result[0].size)
            assertEquals(1, result[0][0].size)
            assertEquals(1, result[0][0][0].size)
            assertEquals(4L, result[0][0][0][0])
        }

        @Test
        fun `Binding results as a tuple`() {
            val x = "x".sym
            val y = "y".sym

            val resultRaw = node.db().use {
                it.queryKt {
                    find {
                        sym(x)
                    }

                    where {
                        subQuery(listOf(listOf(x).pv).pv) {
                            find {
                                sym(y)
                            }

                            where {
                                add(listOf("identity".sym, 2).pl, x)
                                add(listOf("+".sym, x, 2).pl, y)
                            }
                        }
                    }
                }()
            }

            assertNotNull(resultRaw) { "Should have received a result" }
            @Suppress("UNCHECKED_CAST")
            val result = resultRaw.toList() as List<List<List<List<Long>>>>
            assertEquals(1, result.size)
            assertEquals(1, result[0].size)
            assertEquals(4L, result[0][0])
        }
    }


    @AfterAll
    fun closeNode() {
        node.close()
    }
}