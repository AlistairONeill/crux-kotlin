package test.crux.kotlin.queries

import clojure.lang.Keyword
import clojure.lang.PersistentArrayMap
import clojure.lang.PersistentVector
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
import test.crux.kotlin.Convenience

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class Projection {
    companion object {
        private val doctor = Profession("doctor".kw, "Doctor")
        private val lawyer = Profession("lawyer".kw, "Lawyer")

        private val allProfessions = listOf(doctor, lawyer)

        private val ivan = Person("ivan".kw, 1,"Ivan", doctor.id)
        private val petr = Person("petr".kw, 2, "Petr", lawyer.id)
        private val smith = Person("smith".kw, 3, "Smith", doctor.id)

        private val allPeople = listOf(ivan, petr, smith)

        private val personId = "person/id".kw
        private val personName = "person/name".kw
        private val personProfession = "person/profession".kw
        private val professionName = "profession/name".kw
        private val inverseProfession = "person/_profession".kw

        //For renaming tests
        private val people = "people".kw
        private val job = "job".kw
    }

    private data class Profession(
        val id: Keyword,
        val name: String
    )

    private data class Person(
        val id: Keyword,
        val uid: Long,
        val name: String,
        val profession: Keyword
    )

    private val node = Crux.startNode().apply {
        submitTx {
            allPeople.forEach {
                put(it.id) {
                    doc(personId to it.uid,
                        personName to it.name,
                        personProfession to it.profession)
                }
            }

            allProfessions.forEach {
                put(it.id) {
                    doc(professionName to it.name)
                }
            }
        }

        sync(Convenience.TIMEOUT)
    }

    @Nested
    inner class Manual {

        @Test
        fun `Simple projection`() {
            val uid = "?uid".sym
            val person = "?person".sym

            val resultsRaw = node.db().use {
                it.queryKt {
                    find {
                        project(person) {
                            field(personId)
                            field(personName)
                            field(personProfession)
                        }
                    }

                    where {
                        add(person, personId, uid)
                    }
                }()
            }

            assertNotNull(resultsRaw)

            @Suppress("UNCHECKED_CAST")
            val results = resultsRaw!!.toList() as List<List<PersistentArrayMap>>

            assertEquals(3, results.size) { "Should have results from all three people" }
            assert(results.all { it.size == 1 } ) { "Should have one item per result (the projection map)" }
            assert(results.all { it[0].size == 3} ) { "Maps should all have size 3" }
            assert(results.all { it[0].containsKey(personProfession) } ) { "Maps should all contain :person/profession" }
            assert(results.all { it[0].containsKey(personId) } ) { "Maps should all contain :person/id" }
            assert(results.all { it[0].containsKey(personName) } ) { "Maps should all contain :person/name" }
        }

        @Test
        fun `Nested projection`() {
            val uid = "?uid".sym
            val person = "?person".sym

            val resultsRaw = node.db().use {
                it.queryKt {
                    find {
                        project(person) {
                            field(personId)
                            field(personName)
                            nest(personProfession) {
                                field(professionName)
                            }
                        }
                    }

                    where {
                        add(person, personId, uid)
                    }
                }()
            }

            assertNotNull(resultsRaw) { "Should have result object" }

            @Suppress("UNCHECKED_CAST")
            val results = resultsRaw!!.toList() as List<List<PersistentArrayMap>>
            assertEquals(3, results.size) { "Should have results from all three people" }
            assert(results.all { it.size == 1 } ) { "Should have one item per result (the projection map)" }
            assert(results.all { it[0].size == 3} ) { "Maps should all have size 3" }
            assert(results.all { it[0].containsKey(personName) } ) { "Maps should all contain :person/name" }
            assert(results.all { it[0].containsKey(personProfession) } ) { "Maps should all contain :person/profession" }
            assert(results.all { (it[0][personProfession] as PersistentArrayMap).size == 1}) { "Each :person/profession should be a map of size 1" }
            assert(results.all { (it[0][personProfession] as PersistentArrayMap).containsKey(professionName) } ) { "Each :person/profession map should contain the :profession/name" }
        }

        @Test
        fun `Inverse projection`() {
            val prof = "?profession".sym

            val resultsRaw = node.db().use {
                it.queryKt {
                    find {
                        project(prof) {
                            field(professionName)
                            nest(inverseProfession) {
                                field(personId)
                                field(personName)
                            }
                        }
                    }

                    where {
                        add(prof, professionName)
                    }
                }()
            }

            assertNotNull(resultsRaw) { "Should have result object" }

            @Suppress("UNCHECKED_CAST")
            val results = resultsRaw!!.toList() as List<List<PersistentArrayMap>>
            assertEquals(2, results.size) { "Should have results from both professions." }
            assert(results.all { it.size == 1 } ) { "Should have one item per result (the projection map)" }
            assert(results.all { it[0].size == 2} ) { "Maps should all have size 2" }
            assert(results.all { it[0].containsKey(professionName) } ) { "Maps should all contain :profession/name" }
            assert(results.all { it[0].containsKey(inverseProfession) } ) { "Maps should all contain :person/_profession" }
            assert(results.all { (it[0][inverseProfession] is PersistentVector) } ) { "Each :person/_profession should be a vector" }
            assert(results.all { (it[0][inverseProfession] as PersistentVector).all { it is PersistentArrayMap } } ) { "Each item in :person/_profession should be a PAM" }
            assert(results.all { (it[0][inverseProfession] as PersistentVector).all { (it as PersistentArrayMap).containsKey(personName) } } ) { "Each :person/_profession map should contain :person/name" }
            assert(results.all { (it[0][inverseProfession] as PersistentVector).all { (it as PersistentArrayMap).containsKey(personId) } } ) { "Each :person/_profession map should contain :person/id" }
        }

        @Test
        fun `Renaming attributes`() {
            val prof = "?profession".sym

            val resultsRaw = node.db().use {
                it.queryKt {
                    find {
                        project(prof) {
                            field(professionName, job)
                            nest(inverseProfession, people) {
                                field(personId)
                                field(personName)
                            }
                        }
                    }

                    where {
                        add(prof, professionName)
                    }
                }()
            }

            assertNotNull(resultsRaw) { "Should have result object" }

            @Suppress("UNCHECKED_CAST")
            val results = resultsRaw!!.toList() as List<List<PersistentArrayMap>>
            assertEquals(2, results.size) { "Should have results from both professions." }
            assert(results.all { it.size == 1 } ) { "Should have one item per result (the projection map)" }
            assert(results.all { it[0].size == 2} ) { "Maps should all have size 2" }
            assert(results.all { it[0].containsKey(job) } ) { "Maps should all contain :job" }
            assert(results.all { it[0].containsKey(people) } ) { "Maps should all contain :people" }
            assert(results.all { (it[0][people] is PersistentVector) } ) { "Each :people should be a vector" }
            assert(results.all { (it[0][people] as PersistentVector).all { it is PersistentArrayMap } } ) { "Each item in :people should be a PAM" }
            assert(results.all { (it[0][people] as PersistentVector).all { (it as PersistentArrayMap).containsKey(personName) } } ) { "Each :people map should contain :person/name" }
            assert(results.all { (it[0][people] as PersistentVector).all { (it as PersistentArrayMap).containsKey(personId) } } ) { "Each :people map should contain :person/id" }
        }
    }

    @AfterAll
    fun closeNode() {
        node.close()
    }
}