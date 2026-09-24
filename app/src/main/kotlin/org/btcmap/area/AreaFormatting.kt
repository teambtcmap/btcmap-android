package org.btcmap.area

import org.btcmap.R
import org.btcmap.api.GetEventsItem
import org.btcmap.util.isUpcoming
import java.time.ZonedDateTime

internal const val ARG_AREA_ID = "area_id"

private const val INVALID_TAG_VALUE_PREFIX = "invalid_tag_value:"
private const val MISSPELLED_TAG_NAME_PREFIX = "misspelled_tag_name:"

private val PARAGRAPH_SEPARATOR = Regex("\n\\s*\n")

internal fun descriptionParagraphs(description: String?): List<String> {
    return description
        ?.split(PARAGRAPH_SEPARATOR)
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        .orEmpty()
}

internal fun websiteDisplayText(websiteUrl: String): String {
    return websiteUrl.replace("https://", "").replace("http://", "").trimEnd('/')
}

internal fun upcomingEvents(events: List<GetEventsItem>, now: ZonedDateTime): List<GetEventsItem> {
    return events.filter { it.startsAt.isUpcoming(now) }.sortedBy { it.startsAt }
}

internal data class IssueDescription(
    val resId: Int,
    val formatArg: String? = null,
)

internal fun describeIssue(code: String): IssueDescription {
    return when {
        code == "outdated" -> IssueDescription(R.string.issue_outdated)
        code == "outdated_soon" -> IssueDescription(R.string.issue_outdated_soon)
        code == "not_verified" -> IssueDescription(R.string.not_verified)
        code == "missing_icon" -> IssueDescription(R.string.issue_missing_icon)
        code.startsWith(INVALID_TAG_VALUE_PREFIX) -> IssueDescription(
            R.string.issue_invalid_tag_value,
            code.substringAfter(INVALID_TAG_VALUE_PREFIX),
        )
        code.startsWith(MISSPELLED_TAG_NAME_PREFIX) -> IssueDescription(
            R.string.issue_misspelled_tag_name,
            code.substringAfter(MISSPELLED_TAG_NAME_PREFIX),
        )
        else -> IssueDescription(R.string.issue_unknown)
    }
}
