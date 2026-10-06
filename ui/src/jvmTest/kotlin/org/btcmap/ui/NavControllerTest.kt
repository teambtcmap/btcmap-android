package org.btcmap.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The shared back stack both hosts navigate with. */
class NavControllerTest {

    private enum class Route { Map, Settings, Colors }

    @Test
    fun startsAtTheRoot() {
        val nav = NavController(Route.Map)
        assertEquals(Route.Map, nav.current)
        assertFalse(nav.canGoBack)
        assertFalse(nav.pop())
        assertEquals(listOf(Route.Map), nav.routes)
    }

    @Test
    fun pushOpensAScreenOnTop() {
        val nav = NavController(Route.Map)
        nav.push(Route.Settings)
        nav.push(Route.Colors)
        assertEquals(Route.Colors, nav.current)
        assertTrue(nav.canGoBack)
        assertEquals(listOf(Route.Map, Route.Settings, Route.Colors), nav.routes)
    }

    @Test
    fun pushUniqueIgnoresTheScreenAlreadyOnTop() {
        val nav = NavController(Route.Map)
        nav.push(Route.Settings)
        nav.pushUnique(Route.Settings)
        assertEquals(listOf(Route.Map, Route.Settings), nav.routes)

        nav.pushUnique(Route.Colors)
        assertEquals(listOf(Route.Map, Route.Settings, Route.Colors), nav.routes)
    }

    @Test
    fun popReturnsToThePreviousScreenAndStopsAtTheRoot() {
        val nav = NavController(Route.Map)
        nav.push(Route.Settings)

        assertTrue(nav.pop())
        assertEquals(Route.Map, nav.current)
        assertFalse(nav.pop())
        assertEquals(listOf(Route.Map), nav.routes)
    }

    @Test
    fun popToDropsEverythingAboveTheTarget() {
        val nav = NavController(Route.Map)
        nav.push(Route.Settings)
        nav.push(Route.Colors)

        nav.popTo(Route.Settings)
        assertEquals(Route.Settings, nav.current)
        assertEquals(listOf(Route.Map, Route.Settings), nav.routes)

        // An absent target is a no-op rather than an implicit reset.
        nav.popTo(Route.Colors)
        assertEquals(listOf(Route.Map, Route.Settings), nav.routes)
    }

    @Test
    fun resetReplacesTheWholeStack() {
        val nav = NavController(Route.Map)
        nav.push(Route.Settings)
        nav.push(Route.Colors)

        nav.reset(Route.Map)
        assertEquals(Route.Map, nav.current)
        assertFalse(nav.canGoBack)
        assertEquals(listOf(Route.Map), nav.routes)
    }
}
