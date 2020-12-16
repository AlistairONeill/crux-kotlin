package crux.kotlin.projection.annotation


annotation class CruxEntity(val rel: Relationship, val storedOnThisDoc: Boolean) {
    enum class Relationship {
        ONE_TO_MANY,
        ONE_TO_ONE,
        MANY_TO_ONE,
        MANY_TO_MANY
    }
}