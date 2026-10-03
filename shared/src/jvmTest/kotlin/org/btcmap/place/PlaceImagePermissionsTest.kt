package org.btcmap.place

import org.btcmap.api.PlaceImage
import org.btcmap.db.table.user.User
import org.junit.Assert
import org.junit.Test

/** The place-image delete permission: own uploads, or a moderator role. */
class PlaceImagePermissionsTest {

    @Test
    fun ownUpload_isDeletable() {
        Assert.assertTrue(
            user(id = 7, roles = listOf("user")).canDeletePlaceImage(image(createdBy = 7)),
        )
    }

    @Test
    fun anotherUsersUpload_isNotDeletableForAPlainUser() {
        Assert.assertFalse(
            user(id = 7, roles = listOf("user")).canDeletePlaceImage(image(createdBy = 8)),
        )
    }

    @Test
    fun adminAndRoot_canDeleteAnyoneS() {
        Assert.assertTrue(
            user(id = 7, roles = listOf("admin")).canDeletePlaceImage(image(createdBy = 8)),
        )
        Assert.assertTrue(
            user(id = 7, roles = listOf("root")).canDeletePlaceImage(image(createdBy = 8)),
        )
    }

    @Test
    fun unknownUploader_isOnlyDeletableByAModerator() {
        Assert.assertFalse(
            user(id = 7, roles = listOf("user")).canDeletePlaceImage(image(createdBy = null)),
        )
        Assert.assertTrue(
            user(id = 7, roles = listOf("admin")).canDeletePlaceImage(image(createdBy = null)),
        )
    }

    private fun user(id: Long, roles: List<String>) = User(
        id = id,
        name = "tester",
        roles = roles,
        savedPlaces = emptyList(),
        savedAreas = emptyList(),
    )

    private fun image(createdBy: Long?) = PlaceImage(
        id = 3,
        placeId = 42,
        type = "user",
        width = 1024,
        height = 768,
        sizeBytes = 1,
        createdAt = "2026-10-01T04:22:53.706Z",
        createdBy = createdBy,
        authorName = "tester",
    )
}
