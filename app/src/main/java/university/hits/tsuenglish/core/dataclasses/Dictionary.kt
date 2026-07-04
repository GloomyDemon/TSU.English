package university.hits.tsuenglish.core.dataclasses

data class Dictionary(
    val cards: Set<Card> = emptySet(),
    val localCompletionLog: List<CompletionEvent> = emptyList()
) {
    val stats: Stats
        get() {
            val total = cards.size
            val completed = cards.count { it.isCompleted && !it.isAlreadyKnown }
            val alreadyKnown = cards.count { it.isAlreadyKnown }
            val inProcess = cards.count { !it.isNew && !it.isCompleted && !it.isAlreadyKnown }
            return Stats(total, completed, inProcess, alreadyKnown)
        }

    fun addCard(card: Card): Dictionary = copy(cards = cards + card)
    fun addCards(newCards: Collection<Card>): Dictionary = copy(cards = cards + newCards)
    fun removeCard(card: Card): Dictionary = copy(cards = cards - card)
    fun removeCards(cardsToRemove: Collection<Card>): Dictionary =
        copy(cards = cards - cardsToRemove.toSet())
    fun removeCardById(id: Long): Dictionary =
        cards.find { it.id == id }?.let { copy(cards = cards - it) } ?: this

    fun updateCard(card: Card): Dictionary =
        copy(cards = cards.filter { it.id != card.id }.toSet() + card)

    fun getCard(id: Long): Card? = cards.find { it.id == id }

    fun getAllCards(): Set<Card> = cards

    fun resetAllCards(): Dictionary =
        copy(cards = cards.map { it.reset() }.toSet())

    fun logCompletion(event: CompletionEvent): Dictionary =
        copy(localCompletionLog = localCompletionLog + event)

    fun clearLocalLog(): Dictionary =
        copy(localCompletionLog = emptyList())
}