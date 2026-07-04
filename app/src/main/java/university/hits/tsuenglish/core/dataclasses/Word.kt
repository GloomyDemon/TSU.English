package university.hits.tsuenglish.core.dataclasses

data class Word(
    val script: String,
    val transcript: String,
    val examples: Set<String> = emptySet()
) {
    fun addExample(example: String): Word =
        copy(examples = examples + example)

    fun removeExample(example: String): Word =
        copy(examples = examples - example)

    fun addExamples(newExamples: Collection<String>): Word =
        copy(examples = examples + newExamples)
}