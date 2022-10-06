package trends

import kotlinx.serialization.Serializable

@Serializable
data class DailyReport(
    val date: String,
    val total_elements: Long,
    val total_elements_onchain: Long,
    val total_elements_lightning: Long,
    val total_elements_lightning_contactless: Long,
    val up_to_date_elements: Long,
    val outdated_elements: Long,
    val legacy_elements: Long,
    val elements_created: Long,
    val elements_updated: Long,
    val elements_deleted: Long,
)