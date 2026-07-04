package university.hits.tsuenglish.core.dataclasses

data class Stats(
    val wordsTotal: Int,
    val completed: Int = 0,
    val inProcess: Int = 0,
    val alreadyKnown: Int = 0
) {
    val wordsLeft: Int get() = wordsTotal - completed - alreadyKnown
    fun recordCompleted(): Stats = copy(
        completed = completed + 1,
        inProcess = inProcess - 1
    )
    fun recordInProcess(): Stats = copy(inProcess = inProcess + 1)
    fun recordAlreadyKnown(): Stats = copy(alreadyKnown = alreadyKnown + 1)
}