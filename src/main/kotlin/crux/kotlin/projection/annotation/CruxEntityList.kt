package crux.kotlin.projection.annotation

import crux.kotlin.projection.ICruxDataClass
import kotlin.reflect.KClass

annotation class CruxEntityList(val type: KClass<ICruxDataClass>)