package university.hits.tsuenglish.core.dataclasses

import university.hits.tsuenglish.core.enums.PauseTime

data class Card(
    val id: Long,
    val word: Word,
    val translates: Set<Word>,
    val pauseTime: PauseTime = PauseTime.none,
    val isCompleted: Boolean = false,
    val isNew: Boolean = true,
    val isAlreadyKnown: Boolean = false,
    val lastTime: Long = System.currentTimeMillis(),
    val needHelp: Boolean = false,
    val needHelpPause: PauseTime = PauseTime.none
) {
    fun like(): Card {
        val now = System.currentTimeMillis()
        return when {
            isNew -> copy(
                isNew = false,
                isCompleted = true,
                isAlreadyKnown = true,
                pauseTime = PauseTime.none,
                lastTime = now
            )
            needHelp && needHelpPause == PauseTime.hour -> {
                if (pauseTime == PauseTime.three_months) {
                    copy(
                        needHelp = false,
                        needHelpPause = PauseTime.none,
                        isCompleted = true,
                        lastTime = now
                    )
                } else {
                    copy(
                        needHelp = false,
                        needHelpPause = PauseTime.none,
                        pauseTime = pauseTime.next(),
                        lastTime = now
                    )
                }
            }
            needHelp -> copy(
                needHelpPause = needHelpPause.next(),
                lastTime = now
            )
            pauseTime == PauseTime.three_months -> copy(
                isCompleted = true,
                lastTime = now
            )
            else -> copy(
                pauseTime = pauseTime.next(),
                lastTime = now
            )
        }
    }

    fun dislike(): Card {
        val now = System.currentTimeMillis()
        return if (isNew) {
            copy(
                isNew = false,
                pauseTime = PauseTime.five_minutes,
                isCompleted = false,
                needHelp = false,
                lastTime = now
            )
        } else {
            copy(
                needHelp = true,
                needHelpPause = PauseTime.five_minutes,
                isCompleted = false,
                lastTime = now
            )
        }
    }

    fun getExpirationTime(): Long {
        val duration = if (needHelp) needHelpPause.duration else pauseTime.duration
        return lastTime + duration.inWholeMilliseconds
    }

    fun reset(): Card = copy(
        isNew = true,
        isCompleted = false,
        isAlreadyKnown = false,
        pauseTime = PauseTime.none,
        lastTime = System.currentTimeMillis(),
        needHelp = false,
        needHelpPause = PauseTime.none
    )
}