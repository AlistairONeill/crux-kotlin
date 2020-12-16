package crux.kotlin.projection

import clojure.lang.Keyword
import clojure.lang.PersistentArrayMap
import crux.kotlin.CruxKt.DB_ID
import crux.kotlin.extensions.kw
import crux.kotlin.projection.annotation.CruxEntity
import crux.kotlin.projection.annotation.CruxKey
import crux.kotlin.projection.exception.CruxDocumentFormatException
import crux.kotlin.queries.ProjectionBuilder
import crux.kotlin.transactions.statements.PutTxBuilder
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties

interface ICruxDataClass {
    companion object {
        private val allowedTypes: List<KType> = listOf(
            String::class,
            Keyword::class,
            Number::class,
            Boolean::class
        ).map { it.createType() }

        fun <T : ICruxDataClass> factory(data: PersistentArrayMap, kClass: KClass<T>): T {
            if (!kClass.isData) {
                throw CruxDocumentFormatException("Only supports data classes")
            }

            if (kClass.constructors.size != 1) {
                throw CruxDocumentFormatException("Only support classes with single constructors")
            }

            val className = kClass.simpleName?.toLowerCase() ?: throw CruxDocumentFormatException("Could not identify the class name")

            val fieldToData: HashMap<String, Any?> = hashMapOf("cruxId" to data[DB_ID])
            for (prop in kClass.memberProperties) {
                if (prop.name == "cruxId") {
                    continue
                }
                val key = getKey(className, prop)
                fieldToData[prop.name] = data[key]
            }

            val constructor = kClass.constructors.first()
            val constructorArgs = ArrayList<Any?>()
            constructor.parameters.forEach {
                constructorArgs.add(fieldToData[it.name])
            }
            return constructor.call(* constructorArgs.toArray())
        }

        fun <T : ICruxDataClass> getProjectionSpec(kClass: KClass<T>): ProjectionBuilder.()->Unit {
            if (!kClass.isData) {
                throw CruxDocumentFormatException("Only supports data classes")
            }
            val className = kClass.simpleName?.toLowerCase() ?: throw CruxDocumentFormatException("Could not identify the class name")
            val simpleKeys = arrayListOf(DB_ID)
            for (prop in kClass.memberProperties) {
                if (!isAllowed(prop)) {
                    throw CruxDocumentFormatException("Unsupported type ${prop.returnType}")
                }

                val key = getKey(className, prop)
                simpleKeys.add(key)
            }

            return {
                simpleKeys.forEach {
                    field(it)
                }
            }
        }

        fun isAllowed(prop: KProperty1<out ICruxDataClass, *>): Boolean {
            val returnType = prop.returnType
            return (allowedTypes.contains(returnType)
                    || allowedTypes.any { returnType.isSubtypeOf(it)})
        }

        fun getKey(className: String, prop: KProperty1<out ICruxDataClass, *>): Keyword {
            val cruxKeys = prop.annotations.filterIsInstance<CruxKey>()
            return when (cruxKeys.size) {
                0 -> "$className/${prop.name}".kw
                1 -> cruxKeys[0].key.kw
                else -> throw CruxDocumentFormatException("More than one CruxKey annotation attached to ${prop.name}")
            }
        }
    }

    val cruxId: Any

    fun generatePutData(): List<Pair<Any, PutTxBuilder.()->Unit>> {
        val thisClass = this::class
        if (!thisClass.isData) {
            throw CruxDocumentFormatException("Only supports data classes")
        }

        val className = thisClass.simpleName?.toLowerCase() ?: throw CruxDocumentFormatException("Could not identify the class name")
        val toSet = ArrayList<Pair<Keyword, Any>>()
        val others = ArrayList<Pair<Any, PutTxBuilder.()->Unit>>()
        for (prop in thisClass.memberProperties) {
            if (prop.name == "cruxId") {
                continue
            }


            if (!isAllowed(prop)) {
                throw CruxDocumentFormatException("Unsupported type ${prop.returnType}")
            }

            val key = getKey(className, prop)

            //TODO: Handle nulls
            val value = prop.call(this) ?: throw CruxDocumentFormatException("Can't handle null at this stage")

            toSet.add(key to value)
        }

        val thisCall: PutTxBuilder.()->Unit = {
            toSet.forEach {
                add(it)
            }
        }

        //TODO: Handle nested ICruxDataClasses
        return listOf(cruxId to thisCall)
    }
}