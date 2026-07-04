package university.hits.tsuenglish.core.dataclasses

data class CompletionEvent(
    val cardId: Long,
    val dictId: Long,
    val timestamp: Long
)