package test.crux.kotlin

import crux.kotlin.CruxKt
import crux.api.Crux
import crux.kotlin.transactions.submitTx
import test.crux.kotlin.Convenience.EPOCH
import crux.kotlin.extensions.kw
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import java.io.File


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class Configuration {
    /**
     * In this test, we start a node in multiple ways to verify that we are connecting to the same local DB and the configs line up
     */
    companion object {
        private val testKeyword = "foo".kw
    }

    /**
     * Start by deleting whatever might exist in this test folder.
     * It should be cleared up at the end of the test, but do it again just in case.
     * We enforce strict ordering purely so that the first one to run can verify that our keyword document doesn't exist
     */
    init {
        val file = File("test")
        file.deleteRecursively()
    }

    @Test
    @Order(1)
    fun `JSON Configuration`() {
        Crux.startNode(File("src/test/resources/config.json")).use { node ->
            var testDocument = node.db().use {
                it.entity(testKeyword)
            }

            //Our test document shouldn't exist yet!
            assertNull(testDocument)

            node.submitTx {
                put(testKeyword) {
                    validTime = EPOCH
                }
            }

            node.sync()

            testDocument = node.db().use {
                it.entity(testKeyword)
            }

            assertNotNull(testDocument)
        }
    }

    @Test
    @Order(2)
    fun `Resource Configuration`() {
        Crux.startNode(this::class.java.getResource("/config.json")).use { node ->
            val testDocument = node.db().use {
                it.entity(testKeyword)
            }

            assertNotNull(testDocument)
        }
    }

    @Test
    @Order(3)
    fun `Kotlin DSL Configuration`() {
        CruxKt.startNode {
            with ("crux/tx-log") {
                with("kv-store") {
                    module("crux.rocksdb/->kv-store")
                    set("db-dir") { "test/data/tx-log"}
                }
            }
            with("crux/document-store") {
                with("kv-store") {
                    module("crux.rocksdb/->kv-store")
                    set("db-dir") { "test/data/doc-store" }
                }
            }
            with("crux/index-store") {
                with("kv-store") {
                    module("crux.rocksdb/->kv-store")
                    set("db-dir") { "test/data/indices" }
                }
            }
        }.use { node ->
            val testDocument = node.db().use {
                it.entity(testKeyword)
            }

            assertNotNull(testDocument)
        }
    }

    @AfterAll
    fun cleanup() {
        val file = File("test")
        file.deleteRecursively()
    }
}