package test.crux.kotlin

import clojure.lang.Keyword
import clojure.lang.LazySeq
import clojure.lang.PersistentVector
import crux.api.Crux
import crux.kotlin.CruxKt.PUT
import crux.kotlin.CruxKt.TX_OPS
import crux.kotlin.events.CruxEvent
import crux.kotlin.extensions.kw
import crux.kotlin.CruxKt
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertEquals
import java.time.Instant
import java.util.*
import crux.kotlin.transactions.submitTx
import crux.kotlin.transactions.withTx
import crux.kotlin.events.listen

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Transactions {
    companion object {
        val pabloId: Keyword = "dbpedia.resource/Pablo-Picasso".kw

        val nameKey: Keyword = "first-name".kw
        val name: Keyword = "pablo".kw
        val version: Keyword = "version".kw

        val fnId = "increment-version".kw

        //Y2K
        private const val testSeconds = 946684800L

        //All of these are << current time
        val testTimes = (0 .. 10).map { Date.from(Instant.ofEpochSecond(testSeconds + it * 3600)) }
    }

    private val node = Crux.startNode()

    @Nested
    inner class Put{
        @Test
        fun `Pablo at current time`() {
            node.submitTx {
                put(pabloId) {
                    doc(nameKey to name)
                }
            }

            sync()

            assertPablo(getPablo())
        }

        @Test
        fun `Pablo at specific time`() {
            node.submitTx {
                put(pabloId) {
                    doc(nameKey to name)
                    validTime = testTimes[1]
                }
            }

            sync()

            assertNull(getPablo(0)) { "Pablo shouldn't exist before the supplied validTime" }
            assertPablo(getPablo(1)) { "Pablo should exist exactly on the supplied validTime" }
            assertPablo(getPablo(2)) { "Pablo should exist after the supplied validTime" }
        }

        @Test
        fun `Pablo with an end valid time`() {
            node.submitTx {
                put(pabloId) {
                    doc(nameKey to name)
                    validTime = testTimes[1]
                    endValidTime = testTimes[3]
                }
            }

            sync()

            assertNull(getPablo(0)) { "Pablo shouldn't exist before the supplied validTime" }
            assertPablo(getPablo(1)) { "Pablo should exist exactly on the supplied validTime" }
            assertPablo(getPablo(2)) { "Pablo should exist between the supplied valid time and supplied endValidTime" }
            assertNull(getPablo(3)) { "Pablo shouldn't exist exactly on the supplied endValidTime" }
            assertNull(getPablo(4)) { "Pablo shouldn't exist after the supplied endValidTime" }
        }

        @Test
        fun `Pablo with different versions`() {
            node.submitTx {
                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 0)
                    validTime = testTimes[1]
                }

                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 1)
                    validTime = testTimes[3]
                }
            }

            sync()

            assertNull(getPablo(0)) { "Pablo shouldn't exist before the first valid time" }
            assertEquals(0, assertPablo(getPablo(1))) { "Version should be 0 on the first valid time" }
            assertEquals(0, assertPablo(getPablo(2))) { "Version should be 0 between the supplied valid times" }
            assertEquals(1, assertPablo(getPablo(3))) { "Version should be 1 on the second valid time" }
            assertEquals(1, assertPablo(getPablo(4))) { "Version should be 1 after the second valid time" }
        }

        @Test
        fun `Using Instants rather than Dates`() {
            node.submitTx {
                put(pabloId) {
                    doc(nameKey to name,
                        version to 0)
                    validTime(testTimes[1].toInstant())
                    endValidTime(testTimes[3].toInstant())
                }

                put(pabloId) {
                    doc(nameKey to name,
                        version to 1)
                    validTime(testTimes[5].toInstant())
                }
            }

            sync()

            assertNull(getPablo(0)) { "Pablo shouldn't exist before the first valid time" }
            assertEquals(0, assertPablo(getPablo(1))) { "Version should be 0 on the first validTime" }
            assertEquals(0, assertPablo(getPablo(2))) { "Version should be 0 between the first validTime and endValidTime" }
            assertNull(getPablo(3)) { "Pablo shouldn't exist on the endValidTime" }
            assertNull(getPablo(4)) { "Pablo shouldn't exist between the endValidTime and next validTime" }
            assertEquals(1, assertPablo(getPablo(5))) { "Version should be 1 on the second validTime" }
            assertEquals(1, assertPablo(getPablo(6))) { "Version should be 1 after the second validTime" }
            assertEquals(1, assertPablo(getPablo())) { "Version should be 1 now" }
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `Deleting Pablo at current time`() {
            node.submitTx {
                put(pabloId) {
                    doc(nameKey to name)
                    validTime = testTimes[1]
                }

                delete(pabloId)
            }

            sync()

            assertNull(getPablo(0))  { "Pablo should not exist before the valid time of his creation" }
            assertPablo(getPablo(1)) { "Pablo should exist on the valid time of his creation" }
            assertPablo(getPablo(2)) { "Pablo should exist between being created and being deleted" }
            assertNull(getPablo()) { "Pablo should not exist after being deleted" }
        }

        @Test
        fun `Deleting Pablo at specific time`() {
            node.submitTx {
                put(pabloId) {
                    doc(nameKey to name)
                    validTime = testTimes[1]
                }
                delete(pabloId) {
                    validTime = testTimes[3]
                }
            }

            sync()

            assertNull(getPablo(0)) { "Pablo shouldn't exist before the first valid time" }
            assertPablo(getPablo(1)) { "Pablo should exist on the first valid time" }
            assertPablo(getPablo(2)) { "Pablo should exist between the valid times" }
            assertNull(getPablo(3)) { "Pablo should not exist on the second valid time as he has been deleted" }
            assertNull(getPablo(4)) { "Pablo should not exist after the valid time of his deletion" }
        }

        @Test
        fun `Deleting Pablo with an end time`() {
            node.submitTx {
                put(pabloId) {
                    doc(nameKey to name)
                    validTime = testTimes[1]
                }

                delete(pabloId) {
                    validTime = testTimes[3]
                    endValidTime = testTimes[5]
                }
            }

            sync()

            assertNull(getPablo(0)) { "Pablo shouldn't exist before the first valid time" }
            assertPablo(getPablo(1)) { "Pablo should exist on the first valid time" }
            assertPablo(getPablo(2)) { "Pablo should exist between the put time and deletion time" }
            assertNull(getPablo(3)) { "Pablo shouldn't exist on the validTime of his deletion" }
            assertNull(getPablo(4)) { "Pablo shouldn't exist between the validTime and endValidTime of his deletion." }
            assertPablo(getPablo(5)) { "Pablo should exist on the endValidTime of his deletion" }
            assertPablo(getPablo(6)) { "Pablo should exist after the endValidTime of his deletion" }
        }
    }

    @Nested
    inner class Match {
        @Test
        fun `Update if matches current`() {
            node.submitTx {
                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 0)
                }
            }

            sync()

            assertEquals(0, assertPablo(getPablo())) { "Pablo should exist now" }

            node.submitTx {
                match(pabloId) {
                    doc(
                        nameKey to name,
                        version to 0)
                }

                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 1)
                }
            }

            sync()

            assertEquals(1, assertPablo(getPablo())) { "Version should have updated as we matched" }
        }

        @Test
        fun `Don't update if failed to match current`() {
            node.submitTx {
                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 0)
                }
            }

            sync()

            assertEquals(0, assertPablo(getPablo())) { "Pablo should exist with version 1" }

            node.submitTx {
                match(pabloId) {
                    doc(
                        nameKey to name,
                        version to 5)
                }

                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 1)
                }
            }

            assertEquals(0, assertPablo(getPablo())) { "Version should still be 1 as we didn't match" }
        }

        @Test
        fun `Match isn't order dependent`() {
            node.submitTx {
                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 0)
                }
            }

            sync()

            assertEquals(0, assertPablo(getPablo())) { "Pablo should exist with version 0" }

            node.submitTx {
                match(pabloId) {
                    doc(
                        version to 0,
                        nameKey to name
                    )
                }

                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 1)
                }
            }

            sync()

            assertEquals(1, assertPablo(getPablo())) { "Pablo should have been updated" }
        }

        @Test
        fun `Update if matches given valid time`() {
            node.submitTx {
                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 0)
                    validTime = testTimes[1]
                    endValidTime = testTimes[3]
                }
            }

            sync()

            assertNull(getPablo(0)) { "Pablo shouldn't exist yet" }
            assertEquals(0, assertPablo(getPablo(1))) { "Pablo should have version 0 at the validTime of creation" }
            assertEquals(0, assertPablo(getPablo(2))) { "Pablo should have version 0 after the validTime of creation" }
            assertNull(getPablo(3)) { "Pablo no longer exist" }
            assertNull(getPablo(4)) { "Pablo no longer exist" }
            assertNull(getPablo(5)) { "Pablo no longer exist" }
            assertNull(getPablo(6)) { "Pablo no longer exist" }

            node.submitTx {
                match(pabloId) {
                    doc(
                        nameKey to name,
                        version to 0)
                    validTime = testTimes[1]
                }

                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 1)
                    validTime = testTimes[5]
                }
            }

            sync()

            assertNull(getPablo(0)) { "Pablo shouldn't exist yet" }
            assertEquals(0, assertPablo(getPablo(1))) {"Pablo should have version 0 at the validTime of creation" }
            assertEquals(0, assertPablo(getPablo(2))) {"Pablo should have version 0 after the validTime of creation" }
            assertNull(getPablo(3)) { "Pablo should not exist on the old endValidTime" }
            assertNull(getPablo(4)) { "Pablo should not exist between the old endValidTime and the new validTime" }
            assertEquals(1, assertPablo(getPablo(5))) { "Pablo should have version 1 at the validTime from being added after matching" }
            assertEquals(1, assertPablo(getPablo(6))) { "Pablo should have version 1 after the validTime from being added after matching"}
        }

        @Test
        fun `Don't update if doesn't match given valid time`() {
            node.submitTx {
                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 0)
                    validTime = testTimes[1]
                    endValidTime = testTimes[3]
                }
            }

            sync()

            assertNull(getPablo(0)) { "Pablo shouldn't exist yet" }
            assertEquals(0, assertPablo(getPablo(1))) { "Pablo should have version 0 at the validTime of creation" }
            assertEquals(0, assertPablo(getPablo(2))) { "Pablo should have version 0 after the validTime of creation" }
            assertNull(getPablo(3)) { "Pablo no longer exist" }
            assertNull(getPablo(4)) { "Pablo no longer exist" }
            assertNull(getPablo(5)) { "Pablo no longer exist" }
            assertNull(getPablo(6)) { "Pablo no longer exist" }

            node.submitTx {
                match(pabloId) {
                    doc(
                        nameKey to name,
                        version to 0)
                    validTime = testTimes[4]
                }

                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 1)
                    validTime = testTimes[6]
                }
            }

            sync()

            assertNull(getPablo(0)) { "Pablo shouldn't exist yet" }
            assertEquals(0, assertPablo(getPablo(1))) {"Pablo should have version 0 at the validTime of creation" }
            assertEquals(0, assertPablo(getPablo(2))) {"Pablo should have version 0 after the validTime of creation" }
            assertNull(getPablo(3)) { "Pablo should not exist after the old endValidTime" }
            assertNull(getPablo(4)) { "Pablo should not exist after the old endValidTime" }
            assertNull(getPablo(5)) { "Pablo should not exist after the old endValidTime" }
            assertNull(getPablo(6)) { "Pablo should not exist after the old endValidTime" }
            assertNull(getPablo(7)) { "Pablo should not exist after the old endValidTime" }
        }
    }

    @Nested
    inner class Evict {
        @Test
        fun `Check eviction works across all times`() {
            node.submitTx {
                put(pabloId) {
                    doc(nameKey to name)
                    validTime = testTimes[1]
                    endValidTime = testTimes[3]
                }

                put(pabloId) {
                    doc(nameKey to name)
                }
            }

            sync()

            assertNull(getPablo(0)) { "Pablo shouldn't exist yet" }
            assertPablo(getPablo(1)) { "Pablo should exist on the validTime time" }
            assertPablo(getPablo(2)) { "Pablo should exist between validTime and endValidTime" }
            assertNull(getPablo(3)) { "Pablo should not exist on the endValidTime" }
            assertNull(getPablo(4)) { "Pablo should not exist between the endValidTime and 'now'" }
            assertPablo(getPablo()) { "Pablo should exist now" }

            node.submitTx {
                evict(pabloId)
            }

            sync()

            assertNull(getPablo(0)) { "Pablo shouldn't exist at any point in time" }
            assertNull(getPablo(1)) { "Pablo shouldn't exist at any point in time" }
            assertNull(getPablo(2)) { "Pablo shouldn't exist at any point in time" }
            assertNull(getPablo(3)) { "Pablo shouldn't exist at any point in time" }
            assertNull(getPablo(4)) { "Pablo shouldn't exist at any point in time" }
            assertNull(getPablo()) { "Pablo shouldn't exist at any point in time" }
        }
    }

    @Nested
    inner class Functions {
        val functionRaw = "(fn [ctx eid] (let [db (crux.api/db ctx) entity (crux.api/entity db eid)] [[:crux.tx/put (update entity :version inc)]]))"

        @Test
        fun `Functions should work`() {
            node.submitTx {
                put(fnId) {
                    fn(functionRaw)
                }

                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 0)
                }
            }

            sync()

            assertEquals(0, assertPablo(getPablo())) { "Pablo should have version 0 now" }

            node.submitTx {
                fn(fnId, pabloId)
            }

            sync()

            assertEquals(1L, assertPablo(getPablo())) { "Pablo should have version 1 now. It is a long because the clojure function made it so." }
        }
    }

    @Nested
    inner class Events {
        @Test
        fun `Receive events without txOps`() {
            var received: MutableMap<Keyword, *>? = null

            val closeable = node.listen(CruxEvent.IndexedTx) {
                received = it
            }

            val transaction = node.submitTx {
                put(pabloId) {
                    doc(nameKey to name)
                }
            }

            sync()
            Thread.sleep(10) //Need to give it a bit of time to update the received count

            val toCheck = received
            assertNotNull(toCheck) { "We should have received something" }
            assertNull(toCheck!![TX_OPS]) { "We didn't ask for this" }
            assertEquals(transaction["crux.tx/tx-time".kw], toCheck["crux.tx/tx-time".kw]) { "Should match up with our transaction" }
            assertEquals(transaction["crux.tx/tx-id".kw], toCheck["crux.tx/tx-id".kw]) { "Should match up with our transaction" }

            received = null
            assertNull(received) { "Should definitely be null. Only checking in case there is some race condition malarkey" }

            closeable!!.close()

            node.submitTx {
                put(pabloId) {
                    doc(nameKey to name)
                }
            }

            sync()
            Thread.sleep(10) //Just in case it is being slow

            assertNull(received) { "We have closed our listener so it shouldn't update" }
        }

        @Test
        fun `Receive events with TxOps`() {
            var received: MutableMap<Keyword, *>? = null

            val closeable = node.listen(CruxEvent.IndexedTx, true) {
                received = it
            }

            val transaction = node.submitTx {
                put(pabloId) {
                    doc(nameKey to name)
                }
            }

            sync()
            Thread.sleep(10) //Need to give it a bit of time to update the received count

            val toCheck = received
            assertNotNull(toCheck) { "We should have received something" }
            val txOps = toCheck!![TX_OPS] as? LazySeq
            assertNotNull(txOps) { "We subscribed to this" }
            assertEquals(1, txOps!!.size) { "We only had one operation in our transaction" }
            val operation = txOps.first() as PersistentVector
            assertEquals(PUT, operation[0]) { "It should be the PUT operation" }
            @Suppress("UNCHECKED_CAST")
            val document = operation[1] as MutableMap<Keyword, Any>?
            assertPablo(document) { "It should be our boy, Pablo" }

            assertEquals(transaction["crux.tx/tx-time".kw], toCheck["crux.tx/tx-time".kw]) { "Should match up with our transaction" }
            assertEquals(transaction["crux.tx/tx-id".kw], toCheck["crux.tx/tx-id".kw]) { "Should match up with our transaction" }

            closeable?.close()
        }
    }

    @Nested
    inner class SpeculativeTransactions {
        @Test
        fun `Doesn't update the real node`() {
            node.submitTx {
                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 0)
                }
            }

            sync()

            val speculativeDb = node.db().withTx {
                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 1)
                }
            }

            sync() //This shouldn't be necessary if everything is behaving itself

            assertNotNull(speculativeDb) { "We should definitely get a db back as we haven't failed the transaction" }
            assertEquals(0, assertPablo(getPablo())) { "The pablo on our real database should still be version 0" }
            assertEquals(1, assertPablo(speculativeDb!!.entity(pabloId))) { "The pablo on our speculative database is version 1" }

            speculativeDb.close()

            assertEquals(0, assertPablo(getPablo())) { "Even after closing our speculative DB, our real DB shouldn't be changed" }
        }

        @Test
        fun `Failing a speculative transaction should return null`() {
            node.submitTx {
                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 0)
                }
            }

            sync()

            val speculativeDb = node.db().withTx {
                put(pabloId) {
                    doc(
                        nameKey to name,
                        version to 1)
                }

                match(pabloId) {
                    doc(
                        nameKey to name,
                        version to 5)
                }
            }

            sync() //Shouldn't be necessary if everything is behaving itself!

            assertEquals(0, assertPablo(getPablo())) { "Should not have touched our real DB" }
            assertNull(speculativeDb) { "Should be null as the transaction did not go through" }

            speculativeDb?.close()
        }
    }

    private fun sync() = node.sync()

    private fun getPablo(): MutableMap<Keyword, Any>? = node.db().entity(pabloId)
    private fun getPablo(at: Int) = getPablo(testTimes[at])
    private fun getPablo(at: Date): MutableMap<Keyword, Any>? = node.db(at).entity(pabloId)

    private fun assertPablo(pablo: MutableMap<Keyword, Any>?, errorMessage: (() -> String)? = null): Any? {
        assertNotNull(pablo, errorMessage)
        if (pablo == null) return null
        assertEquals(pabloId, pablo[CruxKt.DB_ID]) { "We seem to have retrieved something other than Pablo" }
        assertEquals(name, pablo["first-name".kw]) { "Pablo's name didn't make it into the database" }
        return pablo[version]
    }


    @AfterEach
    fun afterEach() {
        node.submitTx {
            evict(pabloId)
            evict(fnId)
        }

        sync()

        assertNull(node.db().entity(fnId))
        assertNull(getPablo())
    }

    @AfterAll
    fun closeNode() {
        node.close()
    }
}