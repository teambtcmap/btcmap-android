package org.btcmap.api

import kotlinx.coroutines.test.runTest
import org.junit.Assert
import org.junit.Test

class PlaceIssueApiTest : ApiTestBase() {
    @Test
    fun getPlaceIssues_sendsAreaIdAndParsesSummary() = runTest {
        enqueueJson(ISSUES)

        val response = api().getPlaceIssues(areaId = 120)

        val request = takeRequest()
        Assert.assertEquals("GET", request.method)
        Assert.assertEquals("/v4/place-issues", request.url.encodedPath)
        Assert.assertEquals("120", request.url.queryParameter("area_id"))
        Assert.assertEquals("50", request.url.queryParameter("limit"))

        Assert.assertEquals(5, response.totalIssues)
        Assert.assertEquals(2, response.requestedIssues.size)

        val issue = response.requestedIssues[0]
        Assert.assertEquals("node", issue.elementOsmType)
        Assert.assertEquals(987654321L, issue.elementOsmId)
        Assert.assertEquals("Bitcoin ATM", issue.elementName)
        Assert.assertEquals("missing_icon", issue.issueCode)

        Assert.assertEquals("way", response.requestedIssues[1].elementOsmType)
        Assert.assertEquals("outdated", response.requestedIssues[1].issueCode)
    }

    @Test
    fun getPlaceIssues_handlesEmptyList() = runTest {
        enqueueJson("""{"total_issues": 0, "requested_issues": []}""")

        val response = api().getPlaceIssues(areaId = 1)

        Assert.assertEquals(0, response.totalIssues)
        Assert.assertTrue(response.requestedIssues.isEmpty())
    }

    private companion object {
        const val ISSUES = """
            {
                "total_issues": 5,
                "requested_issues": [
                    {
                        "element_osm_type": "node",
                        "element_osm_id": 987654321,
                        "element_name": "Bitcoin ATM",
                        "issue_code": "missing_icon"
                    },
                    {
                        "element_osm_type": "way",
                        "element_osm_id": 456789123,
                        "element_name": "Satoshi's Pub",
                        "issue_code": "outdated"
                    }
                ]
            }
        """
    }
}
