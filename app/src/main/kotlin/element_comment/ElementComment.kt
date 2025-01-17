package element_comment

data class ElementComment(
    val id: Long,
    val elementId: Long,
    val comment: String,
    val createdAt: String,
    val updatedAt: String,
)