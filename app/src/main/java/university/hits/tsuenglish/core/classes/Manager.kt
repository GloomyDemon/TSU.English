package university.hits.tsuenglish.core.classes

import university.hits.tsuenglish.core.dataclasses.Card
import university.hits.tsuenglish.core.dataclasses.CompletionEvent
import university.hits.tsuenglish.core.dataclasses.Dictionary
import university.hits.tsuenglish.core.dataclasses.Stats
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.PriorityQueue

class Manager {
    private var dictionaries: Map<Long, Dictionary> = emptyMap()
    private val newCards = mutableSetOf<Card>()
    private val pendingQueue = PriorityQueue<Card>(compareBy { it.getExpirationTime() })
    private val onPauseQueue = PriorityQueue<Card>(compareBy { it.getExpirationTime() })
    private val completedCards = mutableSetOf<Card>()
    private val manualRepeatPool = mutableListOf<Card>()
    private var currentIsManualRepeat = false
    var selectedDictionaryId: Long? = null
        set(value) {
            field = value
            refresh()
        }
    private val completionLog = mutableListOf<CompletionEvent>()

    var isManualRepeat: Boolean = false
        set(value) {
            if (value && !field) {
                manualRepeatPool.clear()
                manualRepeatPool.addAll(completedCards)
                manualRepeatPool.shuffle()
            } else if (!value && field) {
                completedCards.addAll(manualRepeatPool)
                manualRepeatPool.clear()
            }
            field = value
        }

    fun loadDictionaries(newDictionaries: Map<Long, Dictionary>) {
        dictionaries = newDictionaries
        refresh()
    }

    fun updateDictionary(dictId: Long, newDict: Dictionary) {
        dictionaries = dictionaries + (dictId to newDict)
        refresh()
    }

    fun applyLike(card: Card, dictId: Long): Card {
        val newCard = processLike(card)
        val updatedDict = dictionaries[dictId]?.let { dict ->
            val newDict = dict.updateCard(newCard)
            if (newCard.isCompleted && !newCard.isAlreadyKnown && newCard !== card) {
                val event = CompletionEvent(newCard.id, dictId, System.currentTimeMillis())
                completionLog.add(event)
                newDict.logCompletion(event)
            } else {
                newDict
            }
        }
        if (updatedDict != null) {
            dictionaries = dictionaries + (dictId to updatedDict)
        }
        return newCard
    }

    fun applyDislike(card: Card, dictId: Long): Card {
        val newCard = processDislike(card)
        dictionaries[dictId]?.let { dict ->
            dictionaries = dictionaries + (dictId to dict.updateCard(newCard))
        }
        return newCard
    }

    private fun processLike(card: Card): Card {
        if (currentIsManualRepeat) {
            currentIsManualRepeat = false
            returnToCompletedIfManualRepeat(card)
            return card
        }
        val newCard = card.like()
        updatePoolsAfterAction(card, newCard)
        return newCard
    }

    private fun processDislike(card: Card): Card {
        if (currentIsManualRepeat) {
            currentIsManualRepeat = false
            returnToCompletedIfManualRepeat(card)
            return card
        }
        val newCard = card.dislike()
        updatePoolsAfterAction(card, newCard)
        return newCard
    }

    private fun returnToCompletedIfManualRepeat(card: Card) {
        completedCards.add(card)
        newCards.remove(card)
        pendingQueue.remove(card)
        onPauseQueue.remove(card)
    }

    private fun updatePoolsAfterAction(oldCard: Card, newCard: Card) {
        newCards.remove(oldCard)
        pendingQueue.remove(oldCard)
        onPauseQueue.remove(oldCard)
        completedCards.remove(oldCard)

        if (newCard.isCompleted) {
            completedCards.add(newCard)
            if (isManualRepeat) {
                manualRepeatPool.add(newCard)
                manualRepeatPool.shuffle()
            }
        } else {
            onPauseQueue.add(newCard)
        }
    }

    fun next(): Card? {
        if (isManualRepeat && manualRepeatPool.isNotEmpty()) {
            val card = manualRepeatPool.removeAt(manualRepeatPool.lastIndex)
            currentIsManualRepeat = true
            return card
        }

        val now = System.currentTimeMillis()
        while (onPauseQueue.isNotEmpty()) {
            val peek = onPauseQueue.peek()!!
            if (peek.getExpirationTime() <= now) {
                pendingQueue.add(onPauseQueue.poll())
            } else {
                break
            }
        }

        if (pendingQueue.isNotEmpty()) {
            currentIsManualRepeat = false
            return pendingQueue.poll()
        }

        if (newCards.isNotEmpty()) {
            val card = newCards.random()
            newCards.remove(card)
            currentIsManualRepeat = false
            return card
        }

        currentIsManualRepeat = false
        return null
    }

    fun getTotalStats(): Stats {
        var total = 0
        var completed = 0
        var inProcess = 0
        var alreadyKnown = 0
        dictionaries.values.forEach { dict ->
            total += dict.stats.wordsTotal
            completed += dict.stats.completed
            inProcess += dict.stats.inProcess
            alreadyKnown += dict.stats.alreadyKnown
        }
        return Stats(total, completed, inProcess, alreadyKnown)
    }


    fun getDictionaryStats(dictId: Long): Stats = dictionaries[dictId]?.stats ?: Stats(0)

    fun getPoolSizes(): PoolSizes = PoolSizes(
        new = newCards.size,
        pending = pendingQueue.size,
        onPause = onPauseQueue.size,
        completed = completedCards.size,
        manualRepeat = manualRepeatPool.size
    )

    data class PoolSizes(
        val new: Int,
        val pending: Int,
        val onPause: Int,
        val completed: Int,
        val manualRepeat: Int
    )

    private fun refresh() {
        newCards.clear()
        pendingQueue.clear()
        onPauseQueue.clear()
        completedCards.clear()
        manualRepeatPool.clear()

        val now = System.currentTimeMillis()
        dictionaries
            .filterKeys { selectedDictionaryId == null || it == selectedDictionaryId }
            .values
            .forEach { dict ->
            dict.cards.forEach { card ->
                when {
                    card.isCompleted -> completedCards.add(card)
                    card.isNew -> newCards.add(card)
                    else -> {
                        if (card.getExpirationTime() <= now) {
                            pendingQueue.add(card)
                        } else {
                            onPauseQueue.add(card)
                        }
                    }
                }
            }
        }
        if (isManualRepeat) {
            manualRepeatPool.addAll(completedCards)
            manualRepeatPool.shuffle()
        }
    }

    fun getDailyCompletionCounts(
        dictId: Long? = null,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null
    ): Map<LocalDate, Int> {
        val filtered = completionLog.filter { event ->
            (dictId == null || event.dictId == dictId) &&
                    (startDate == null || toLocalDate(event.timestamp) >= startDate) &&
                    (endDate == null || toLocalDate(event.timestamp) <= endDate)
        }
        return filtered.groupBy { toLocalDate(it.timestamp) }
            .mapValues { it.value.size }
    }

    private fun toLocalDate(timestamp: Long): LocalDate {
        return Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).toLocalDate()
    }

    fun clearDictionaryProgress(dictId: Long) {
        dictionaries[dictId]?.let { dict ->
            dictionaries = dictionaries + (dictId to dict.resetAllCards())
        }
        completionLog.removeAll { it.dictId == dictId }
        refresh()
    }

    fun clearAllProgress() {
        dictionaries = dictionaries.mapValues { (_, dict) -> dict.resetAllCards() }
        completionLog.clear()
        refresh()
    }

}